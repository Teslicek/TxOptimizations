package com.teslicek.txoptimizations;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.logging.LogUtils;
import com.mojang.renderpearl.api.GpuFormat;
import com.mojang.renderpearl.api.pipeline.PrimitiveTopology;
import com.mojang.renderpearl.api.vertex.VertexFormat;
import com.mojang.renderpearl.api.vertex.VertexFormatElement;
import com.teslicek.txoptimizations.mixin.FrontendGpuDeviceAccessor;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;
import net.minecraft.client.renderer.DynamicGpuData;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3d;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector3d;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.slf4j.Logger;

public final class FacingCulling {

    private static final Logger LOGGER = LogUtils.getLogger();

    private static final int    GROUPS                = 7;
    private static final int    UNASSIGNED            = 3;
    private static final int    OPPOSITE              = GROUPS - 1;
    private static final int    PLANES                = GROUPS;
    private static final int    THINNESS              = GROUPS * 2;
    private static final int    BOX                   = GROUPS * 3;
    private static final int    RANGES                = BOX + 6;
    private static final int    AXES                  = 3;
    private static final int    X                     = 0;
    private static final int    Y                     = 1;
    private static final int    Z                     = 2;
    private static final int    W                     = 3;
    private static final int    ROW                   = 4;
    private static final int    NO_RUN                = -1;
    private static final int    NO_SUBPIXEL_BITS      = -1;
    private static final int    VERTICES_PER_QUAD     = PrimitiveTopology.QUADS.primitiveLength;
    private static final int    INDICES_PER_QUAD      = PrimitiveTopology.QUADS.indexCount(VERTICES_PER_QUAD);
    private static final double MAX_INVERSE_THICKNESS = 4.0;
    private static final double FLOAT_ROUNDOFF        = 0x1p-24;
    private static final double GPU_ERROR             = 21.0 * FLOAT_ROUNDOFF;
    private static final double VIEWPORT_ERROR        = 5.0 * FLOAT_ROUNDOFF;
    private static final double DOUBLE_ERROR          = 0x1p-50;
    private static final double LENGTH_ERROR          = 0x1p-46;
    private static final double SAFETY                = 1.0 + 0x1p-20;
    private static final double SLACK                 = 0x1p-30;
    private static final double MAX_CONDITION         = 0x1p20;

    private static final Matrix4f PROJECTION = new Matrix4f();
    private static final Matrix3d LINEAR     = new Matrix3d();
    private static final Matrix3d INVERSE    = new Matrix3d();
    private static final Vector3d EYE        = new Vector3d();
    private static final double[] CLIP       = new double[ROW * ROW];
    private static final double[] CLIP_ABS   = new double[ROW * ROW];
    private static final double[] ERROR_ABS  = new double[ROW * ROW];

    private static boolean projectionCaptured;
    private static boolean backendWarned;
    private static boolean cullable;
    private static int     cameraBlockX;
    private static int     cameraBlockY;
    private static int     cameraBlockZ;
    private static double  cameraOffsetX;
    private static double  cameraOffsetY;
    private static double  cameraOffsetZ;
    private static double  eyeBaseX;
    private static double  eyeBaseY;
    private static double  eyeBaseZ;
    private static double  eyeSlack;
    private static double  halfWidth;
    private static double  halfHeight;
    private static double  subpixel;
    private static double  normX;
    private static double  normY;
    private static double  normW;
    private static double  inverseArea;

    private FacingCulling() {
    }

    public static int[] groupQuads(MeshData mesh) {
        MeshData.DrawState state = mesh.drawState();

        if (state.primitiveTopology() != PrimitiveTopology.QUADS)
            throw new IllegalStateException("Expected opaque section geometry made of quads but got " + state.primitiveTopology());

        if (mesh.indexBuffer() != null)
            throw new IllegalStateException("Opaque section geometry is drawn with the shared quad index buffer but this mesh has its own index buffer");

        VertexFormat        format   = state.format();
        VertexFormatElement position = ((VertexFormatElementCache) format).txoptimizations$getPositionElement();

        if (position == null || position.format() != GpuFormat.RGB32_FLOAT)
            throw new IllegalStateException("Expected three float positions in " + format + " but got " + position);

        int        vertexSize = format.getVertexSize();
        int        quads      = state.vertexCount() / VERTICES_PER_QUAD;
        ByteBuffer vertices   = mesh.vertexBuffer();

        if ((long) quads * INDICES_PER_QUAD != state.indexCount())
            throw new IllegalStateException(quads + " quads do not match the mesh's " + state.indexCount() + " indices");

        if ((long) quads * vertexSize * VERTICES_PER_QUAD > vertices.remaining())
            throw new IllegalStateException(quads + " quads of " + vertexSize * VERTICES_PER_QUAD + " bytes do not fit in " + vertices.remaining() + " bytes of vertex data");

        return groupVertices(MemoryUtil.memAddress(vertices), position.offset(), vertexSize, quads);
    }

    public static void captureProjection(Matrix4fc projection) {
        RenderSystem.assertOnRenderThread();

        PROJECTION.set(projection);
        projectionCaptured = true;
    }

    public static void beginFrame(Vec3 camera, Matrix4fc viewRotation, int width, int height) {
        RenderSystem.assertOnRenderThread();

        if (!projectionCaptured)
            throw new IllegalStateException("Terrain is drawn before the level projection was set");

        cullable = prepareFrame(PROJECTION, viewRotation, camera, width, height, subpixelBits());
    }

    public static int visibleGroups(int[] ranges, int originX, int originY, int originZ) {
        int occupied = occupiedGroups(ranges);

        if (!cullable)
            return occupied;

        double eyeX   = (cameraBlockX - originX) + eyeBaseX;
        double eyeY   = (cameraBlockY - originY) + eyeBaseY;
        double eyeZ   = (cameraBlockZ - originZ) + eyeBaseZ;
        int    behind = 0;

        for (int group = 0; group < GROUPS; group ++)
            if (group != UNASSIGNED && (occupied & 1 << group) != 0 && heightBehind(ranges, group, eyeX, eyeY, eyeZ) > 0.0)
                behind |= 1 << group;

        if (behind == 0)
            return occupied;

        float  minX    = Float.intBitsToFloat(ranges[BOX]);
        float  minY    = Float.intBitsToFloat(ranges[BOX + 1]);
        float  minZ    = Float.intBitsToFloat(ranges[BOX + 2]);
        float  maxX    = Float.intBitsToFloat(ranges[BOX + 3]);
        float  maxY    = Float.intBitsToFloat(ranges[BOX + 4]);
        float  maxZ    = Float.intBitsToFloat(ranges[BOX + 5]);
        double halfX   = ((double) maxX - minX) * 0.5 + SLACK;
        double halfY   = ((double) maxY - minY) * 0.5 + SLACK;
        double halfZ   = ((double) maxZ - minZ) * 0.5 + SLACK;
        double centerX = ((double) minX + maxX) * 0.5 + (originX - cameraBlockX) + cameraOffsetX;
        double centerY = ((double) minY + maxY) * 0.5 + (originY - cameraBlockY) + cameraOffsetY;
        double centerZ = ((double) minZ + maxZ) * 0.5 + (originZ - cameraBlockZ) + cameraOffsetZ;
        double reach   = Math.max(Math.abs(centerX) + halfX, Math.max(Math.abs(centerY) + halfY, Math.abs(centerZ) + halfZ)) + 1.0;
        double x       = clip(X, centerX, centerY, centerZ);
        double y       = clip(Y, centerX, centerY, centerZ);
        double z       = clip(Z, centerX, centerY, centerZ);
        double w       = clip(W, centerX, centerY, centerZ);
        double spreadX = spread(X, halfX, halfY, halfZ);
        double spreadY = spread(Y, halfX, halfY, halfZ);
        double spreadZ = spread(Z, halfX, halfY, halfZ);
        double spreadW = spread(W, halfX, halfY, halfZ);
        double errorX  = gpuError(X, reach);
        double errorY  = gpuError(Y, reach);
        double errorZ  = gpuError(Z, reach);
        double errorW  = gpuError(W, reach);
        double nearest = w - spreadW;
        double inner   = nearest - errorW;
        double extentX = Math.abs(x) + spreadX;
        double extentY = Math.abs(y) + spreadY;

        if (!(inner > extentX + errorX && inner > extentY + errorY && z - spreadZ > errorZ && inner > z + spreadZ + errorZ))
            return occupied;

        double farthest       = w + spreadW;
        double inverseNearest = 1.0 / nearest;
        double inverseInner   = 1.0 / inner;
        double slopeX         = extentX * inverseNearest;
        double slopeY         = extentY * inverseNearest;
        double shift          = subpixel + Math.max(halfWidth * ((errorX + slopeX * errorW) * inverseInner + VIEWPORT_ERROR), halfHeight * ((errorY + slopeY * errorW) * inverseInner + VIEWPORT_ERROR));
        double stretch        = halfWidth * (normX + slopeX * normW) + halfHeight * (normY + slopeY * normW);
        double cube           = farthest * farthest * farthest * inverseArea * SAFETY;
        double linear         = 2.0 * shift * stretch * inverseNearest * cube;
        double quadratic      = 4.0 * shift * shift * cube;
        int    culled         = 0;

        for (int group = 0; group < GROUPS; group ++) {
            if ((behind & 1 << group) == 0)
                continue;

            double inverseThickness = Float.intBitsToFloat(ranges[THINNESS + group]);

            if (heightBehind(ranges, group, eyeX, eyeY, eyeZ) > (linear + quadratic * inverseThickness) * inverseThickness)
                culled |= 1 << group;
        }

        return occupied & ~culled;
    }

    public static void addDraws(List<DynamicGpuData.IndexedDraw> draws, int[] ranges, int visibleGroups, int baseVertex, int sectionInfo) {
        int runStart   = NO_RUN;
        int groupStart = 0;

        for (int group = 0; group < GROUPS; group ++) {
            int groupEnd = ranges[group];

            if (groupEnd == groupStart)
                continue;

            if ((visibleGroups & 1 << group) != 0) {
                if (runStart == NO_RUN)
                    runStart = groupStart;
            } else if (runStart != NO_RUN) {
                draws.add(draw(runStart, groupStart, baseVertex, sectionInfo));
                runStart = NO_RUN;
            }

            groupStart = groupEnd;
        }

        if (runStart != NO_RUN)
            draws.add(draw(runStart, groupStart, baseVertex, sectionInfo));
    }

    private static int[] groupVertices(long base, int positionOffset, int vertexSize, int quads) {
        int     quadSize     = vertexSize * VERTICES_PER_QUAD;
        long    positions    = base + positionOffset;
        int[]   destinations = new int[quads];
        int[]   ranges       = new int[RANGES];
        int[]   next         = new int[GROUPS];
        float[] planes       = new float[GROUPS];
        float[] thinness     = new float[GROUPS];
        float[] box          = {Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY};

        Arrays.fill(planes, Float.POSITIVE_INFINITY);

        for (int quad = 0; quad < quads; quad ++) {
            long   first     = positions + (long) quad * quadSize;
            int    group     = classify(first, vertexSize);
            double thickness = group == UNASSIGNED ? 0.0 : inverseThickness(first, vertexSize, axis(group));

            if (thickness > MAX_INVERSE_THICKNESS * SAFETY)
                group = UNASSIGNED;

            destinations[quad] = group;
            ranges[group] ++;

            if (group == UNASSIGNED)
                continue;

            planes[group]   = Math.min(planes[group], signedPlane(first, group));
            thinness[group] = Math.max(thinness[group], roundUp(thickness));

            for (int vertex = 0; vertex < VERTICES_PER_QUAD; vertex ++)
                include(box, first + (long) vertex * vertexSize);
        }

        int end = 0;

        for (int group = 0; group < GROUPS; group ++) {
            next[group]              = end;
            end                     += ranges[group];
            ranges[group]            = end;
            ranges[PLANES + group]   = Float.floatToRawIntBits(planes[group]);
            ranges[THINNESS + group] = Float.floatToRawIntBits(thinness[group]);
        }

        for (int corner = 0; corner < box.length; corner ++)
            ranges[BOX + corner] = Float.floatToRawIntBits(box[corner]);

        for (int quad = 0; quad < quads; quad ++)
            destinations[quad] = next[destinations[quad]] ++;

        permute(base, quadSize, destinations);

        return ranges;
    }

    private static boolean prepareFrame(Matrix4fc projection, Matrix4fc view, Vec3 camera, int width, int height, int subpixelBits) {
        if (width <= 0 || height <= 0 || subpixelBits < 0)
            return false;

        for (int row = 0; row < ROW; row ++) {
            for (int column = 0; column < ROW; column ++) {
                double value = 0.0;
                double bound = 0.0;

                for (int inner = 0; inner < ROW; inner ++) {
                    double left  = projection.get(inner, row);
                    double right = view.get(column, inner);

                    value += left * right;
                    bound += Math.abs(left) * Math.abs(right);
                }

                CLIP[row * ROW + column]      = value;
                CLIP_ABS[row * ROW + column]  = Math.abs(value);
                ERROR_ABS[row * ROW + column] = bound;
            }
        }

        for (double bound : ERROR_ABS)
            if (!Double.isFinite(bound))
                return false;

        LINEAR.set(entry(X, 0), entry(Y, 0), entry(W, 0), entry(X, 1), entry(Y, 1), entry(W, 1), entry(X, 2), entry(Y, 2), entry(W, 2));

        double determinant = LINEAR.determinant();

        if (!Double.isFinite(determinant) || determinant == 0.0)
            return false;

        INVERSE.set(LINEAR).invert();
        INVERSE.transform(EYE.set(-entry(X, 3), -entry(Y, 3), -entry(W, 3)));

        double linearNorm  = Math.max(rowNorm(LINEAR, 0), Math.max(rowNorm(LINEAR, 1), rowNorm(LINEAR, 2)));
        double inverseNorm = Math.max(rowNorm(INVERSE, 0), Math.max(rowNorm(INVERSE, 1), rowNorm(INVERSE, 2))) * SAFETY;

        if (!(linearNorm * inverseNorm <= MAX_CONDITION))
            return false;

        double residual  = Math.max(Math.abs(residual(X)), Math.max(Math.abs(residual(Y)), Math.abs(residual(W))));
        double magnitude = Math.max(residualMagnitude(X), Math.max(residualMagnitude(Y), residualMagnitude(W)));
        double eyeError  = inverseNorm * (residual + DOUBLE_ERROR * magnitude) * SAFETY;

        if (!Double.isFinite(eyeError))
            return false;

        cameraBlockX  = Mth.floor(camera.x);
        cameraBlockY  = Mth.floor(camera.y);
        cameraBlockZ  = Mth.floor(camera.z);
        cameraOffsetX = (float) (cameraBlockX - camera.x);
        cameraOffsetY = (float) (cameraBlockY - camera.y);
        cameraOffsetZ = (float) (cameraBlockZ - camera.z);
        eyeBaseX      = EYE.x - cameraOffsetX;
        eyeBaseY      = EYE.y - cameraOffsetY;
        eyeBaseZ      = EYE.z - cameraOffsetZ;
        eyeSlack      = eyeError + SLACK;
        halfWidth     = width * 0.5;
        halfHeight    = height * 0.5;
        subpixel      = Math.scalb(1.0, -subpixelBits);
        normX         = euclideanNorm(X) * SAFETY;
        normY         = euclideanNorm(Y) * SAFETY;
        normW         = euclideanNorm(W) * SAFETY;
        inverseArea   = SAFETY / (halfWidth * halfHeight * Math.abs(determinant));

        return Double.isFinite(eyeBaseX + eyeBaseY + eyeBaseZ + inverseArea);
    }

    private static int subpixelBits() {
        if (RenderSystem.getDevice() instanceof FrontendGpuDeviceAccessor device && device.txoptimizations$backend() instanceof SubpixelPrecision precision)
            return precision.txoptimizations$subpixelBits();

        if (!backendWarned) {
            LOGGER.warn("Drawing every terrain face direction because the graphics backend does not report the GPU's subpixel precision");
            backendWarned = true;
        }

        return NO_SUBPIXEL_BITS;
    }

    private static int occupiedGroups(int[] ranges) {
        int occupied   = 0;
        int groupStart = 0;

        for (int group = 0; group < GROUPS; group ++) {
            if (ranges[group] != groupStart)
                occupied |= 1 << group;

            groupStart = ranges[group];
        }

        return occupied;
    }

    private static double heightBehind(int[] ranges, int group, double x, double y, double z) {
        int    axis = axis(group);
        double eye  = axis == X ? x : axis == Y ? y : z;

        return Float.intBitsToFloat(ranges[PLANES + group]) - (group < UNASSIGNED ? eye : -eye) - eyeSlack;
    }

    private static double clip(int row, double x, double y, double z) {
        int start = row * ROW;

        return CLIP[start] * x + CLIP[start + 1] * y + CLIP[start + 2] * z + CLIP[start + 3];
    }

    private static double spread(int row, double x, double y, double z) {
        int start = row * ROW;

        return CLIP_ABS[start] * x + CLIP_ABS[start + 1] * y + CLIP_ABS[start + 2] * z;
    }

    private static double gpuError(int row, double reach) {
        int start = row * ROW;

        return GPU_ERROR * ((ERROR_ABS[start] + ERROR_ABS[start + 1] + ERROR_ABS[start + 2]) * reach + ERROR_ABS[start + 3]);
    }

    private static double entry(int row, int column) {
        return CLIP[row * ROW + column];
    }

    private static double euclideanNorm(int row) {
        return Math.sqrt(entry(row, 0) * entry(row, 0) + entry(row, 1) * entry(row, 1) + entry(row, 2) * entry(row, 2));
    }

    private static double residual(int row) {
        return entry(row, 0) * EYE.x + entry(row, 1) * EYE.y + entry(row, 2) * EYE.z + entry(row, 3);
    }

    private static double residualMagnitude(int row) {
        return Math.abs(entry(row, 0) * EYE.x) + Math.abs(entry(row, 1) * EYE.y) + Math.abs(entry(row, 2) * EYE.z) + Math.abs(entry(row, 3));
    }

    private static double rowNorm(Matrix3d matrix, int row) {
        return Math.abs(matrix.get(0, row)) + Math.abs(matrix.get(1, row)) + Math.abs(matrix.get(2, row));
    }

    private static DynamicGpuData.IndexedDraw draw(int firstQuad, int endQuad, int baseVertex, int sectionInfo) {
        return new DynamicGpuData.IndexedDraw((endQuad - firstQuad) * INDICES_PER_QUAD, 1, firstQuad * INDICES_PER_QUAD, baseVertex, sectionInfo);
    }

    private static int classify(long first, int vertexSize) {
        long second = first + vertexSize;
        long third  = second + vertexSize;
        long fourth = third + vertexSize;

        for (int axis = 0; axis < AXES; axis ++) {
            float plane = coordinate(first, axis);

            if (plane != coordinate(second, axis) || plane != coordinate(third, axis) || plane != coordinate(fourth, axis))
                continue;

            if (!Float.isFinite(plane))
                return UNASSIGNED;

            int    u              = (axis + 1) % AXES;
            int    v              = (axis + 2) % AXES;
            double firstTriangle  = normal(first, second, third, u, v);
            double secondTriangle = normal(third, fourth, first, u, v);

            if (!Double.isFinite(firstTriangle) || !Double.isFinite(secondTriangle))
                return UNASSIGNED;

            if (firstTriangle > 0.0 && secondTriangle > 0.0)
                return axis;

            if (firstTriangle < 0.0 && secondTriangle < 0.0)
                return OPPOSITE - axis;

            return UNASSIGNED;
        }

        return UNASSIGNED;
    }

    private static double normal(long first, long second, long third, int u, int v) {
        double originU = coordinate(first, u);
        double originV = coordinate(first, v);
        double edgeU   = coordinate(second, u) - originU;
        double edgeV   = coordinate(second, v) - originV;
        double otherU  = coordinate(third, u) - originU;
        double otherV  = coordinate(third, v) - originV;

        return edgeU * otherV - edgeV * otherU;
    }

    private static double inverseThickness(long first, int vertexSize, int axis) {
        int  u      = (axis + 1) % AXES;
        int  v      = (axis + 2) % AXES;
        long second = first + vertexSize;
        long third  = second + vertexSize;
        long fourth = third + vertexSize;

        return Math.max(inverseThickness(first, second, third, u, v), inverseThickness(third, fourth, first, u, v));
    }

    private static double inverseThickness(long first, long second, long third, int u, int v) {
        double originU    = coordinate(first, u);
        double originV    = coordinate(first, v);
        double edgeU      = coordinate(second, u) - originU;
        double edgeV      = coordinate(second, v) - originV;
        double otherU     = coordinate(third, u) - originU;
        double otherV     = coordinate(third, v) - originV;
        double edge       = Math.sqrt(edgeU * edgeU + edgeV * edgeV);
        double other      = Math.sqrt(otherU * otherU + otherV * otherV);
        double closing    = Math.sqrt((otherU - edgeU) * (otherU - edgeU) + (otherV - edgeV) * (otherV - edgeV));
        double shorter    = edge + other + closing - Math.max(edge, Math.max(other, closing));
        double twiceArea  = Math.abs(edgeU * otherV - edgeV * otherU) - DOUBLE_ERROR * (Math.abs(edgeU * otherV) + Math.abs(edgeV * otherU));

        return twiceArea > 0.0 ? shorter * (1.0 + LENGTH_ERROR) / twiceArea : Double.POSITIVE_INFINITY;
    }

    private static float roundUp(double value) {
        float rounded = (float) value;

        return rounded < value ? Math.nextUp(rounded) : rounded;
    }

    private static void include(float[] box, long vertex) {
        for (int axis = 0; axis < AXES; axis ++) {
            float value = coordinate(vertex, axis);

            box[axis]        = Math.min(box[axis], value);
            box[AXES + axis] = Math.max(box[AXES + axis], value);
        }
    }

    private static float signedPlane(long first, int group) {
        float plane = coordinate(first, axis(group));

        return group < UNASSIGNED ? plane : -plane;
    }

    private static int axis(int group) {
        return group < UNASSIGNED ? group : OPPOSITE - group;
    }

    private static float coordinate(long vertex, int axis) {
        return MemoryUtil.memGetFloat(vertex + (long) axis * Float.BYTES);
    }

    private static void permute(long base, int quadSize, int[] destinations) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            long carried   = stack.nmalloc(quadSize);
            long displaced = stack.nmalloc(quadSize);

            for (int start = 0; start < destinations.length; start ++) {
                if (destinations[start] == start)
                    continue;

                MemoryUtil.memCopy(base + (long) start * quadSize, carried, quadSize);

                int current = start;

                do {
                    int  target  = destinations[current];
                    long address = base + (long) target * quadSize;

                    destinations[current] = current;

                    if (target != start)
                        MemoryUtil.memCopy(address, displaced, quadSize);

                    MemoryUtil.memCopy(carried, address, quadSize);

                    long swap = carried;

                    carried   = displaced;
                    displaced = swap;
                    current   = target;
                } while (current != start);
            }
        }
    }
}
