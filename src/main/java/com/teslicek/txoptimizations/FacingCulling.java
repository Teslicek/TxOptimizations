package com.teslicek.txoptimizations;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.renderpearl.api.GpuFormat;
import com.mojang.renderpearl.api.pipeline.PrimitiveTopology;
import com.mojang.renderpearl.api.vertex.VertexFormat;
import com.mojang.renderpearl.api.vertex.VertexFormatElement;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;
import net.minecraft.client.renderer.DynamicGpuData;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4d;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

public final class FacingCulling {

    private static final int    GROUPS            = 7;
    private static final int    UNASSIGNED        = 3;
    private static final int    OPPOSITE          = GROUPS - 1;
    private static final int    RANGES            = GROUPS * 2;
    private static final int    AXES              = 3;
    private static final int    X                 = 0;
    private static final int    Y                 = 1;
    private static final int    NO_RUN            = -1;
    private static final double MARGIN            = 1.0 / 32.0;
    private static final int    VERTICES_PER_QUAD = PrimitiveTopology.QUADS.primitiveLength;
    private static final int    INDICES_PER_QUAD  = PrimitiveTopology.QUADS.indexCount(VERTICES_PER_QUAD);

    private static final Matrix4f PROJECTION      = new Matrix4f();
    private static final Matrix4d VIEW_PROJECTION = new Matrix4d();
    private static final Vector3d EYE             = new Vector3d();

    private static boolean projectionCaptured;

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
        int        quadSize   = vertexSize * VERTICES_PER_QUAD;
        int        quads      = state.vertexCount() / VERTICES_PER_QUAD;
        ByteBuffer vertices   = mesh.vertexBuffer();

        if ((long) quads * INDICES_PER_QUAD != state.indexCount())
            throw new IllegalStateException(quads + " quads do not match the mesh's " + state.indexCount() + " indices");

        if ((long) quads * quadSize > vertices.remaining())
            throw new IllegalStateException(quads + " quads of " + quadSize + " bytes do not fit in " + vertices.remaining() + " bytes of vertex data");

        long    base         = MemoryUtil.memAddress(vertices);
        long    positions    = base + position.offset();
        int[]   destinations = new int[quads];
        int[]   ranges       = new int[RANGES];
        int[]   next         = new int[GROUPS];
        float[] bounds       = new float[GROUPS];

        Arrays.fill(bounds, Float.POSITIVE_INFINITY);

        for (int quad = 0; quad < quads; quad ++) {
            long first = positions + (long) quad * quadSize;
            int  group = classify(first, vertexSize);

            destinations[quad] = group;
            ranges[group] ++;

            if (group != UNASSIGNED)
                bounds[group] = Math.min(bounds[group], signedPlane(first, group));
        }

        int end = 0;

        for (int group = 0; group < GROUPS; group ++) {
            next[group]            = end;
            end                   += ranges[group];
            ranges[group]          = end;
            ranges[GROUPS + group] = Float.floatToRawIntBits(bounds[group]);
        }

        for (int quad = 0; quad < quads; quad ++)
            destinations[quad] = next[destinations[quad]] ++;

        permute(base, quadSize, destinations);

        return ranges;
    }

    public static void captureProjection(Matrix4fc projection) {
        RenderSystem.assertOnRenderThread();

        PROJECTION.set(projection);
        projectionCaptured = true;
    }

    public static Vector3dc eye(Vec3 camera, Matrix4fc viewRotation) {
        RenderSystem.assertOnRenderThread();

        if (!projectionCaptured)
            throw new IllegalStateException("Terrain is drawn before the level projection was set");

        VIEW_PROJECTION.set(PROJECTION).mul(viewRotation).perspectiveOrigin(EYE);

        if (!Double.isFinite(EYE.x) || !Double.isFinite(EYE.y) || !Double.isFinite(EYE.z))
            throw new IllegalStateException("The level projection " + PROJECTION + " with view rotation " + viewRotation + " has no eye point but " + EYE);

        return EYE.add(camera.x, camera.y, camera.z);
    }

    public static int visibleGroups(int[] ranges, double x, double y, double z) {
        int visible    = 0;
        int groupStart = 0;

        for (int group = 0; group < GROUPS; group ++) {
            int groupEnd = ranges[group];

            if (groupEnd != groupStart && facesEye(ranges, group, x, y, z))
                visible |= 1 << group;

            groupStart = groupEnd;
        }

        return visible;
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

    private static DynamicGpuData.IndexedDraw draw(int firstQuad, int endQuad, int baseVertex, int sectionInfo) {
        return new DynamicGpuData.IndexedDraw((endQuad - firstQuad) * INDICES_PER_QUAD, 1, firstQuad * INDICES_PER_QUAD, baseVertex, sectionInfo);
    }

    private static boolean facesEye(int[] ranges, int group, double x, double y, double z) {
        if (group == UNASSIGNED)
            return true;

        int    axis = axis(group);
        double eye  = axis == X ? x : axis == Y ? y : z;

        return (group < UNASSIGNED ? eye : -eye) >= Float.intBitsToFloat(ranges[GROUPS + group]) - MARGIN;
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
