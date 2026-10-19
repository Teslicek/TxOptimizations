package com.teslicek.txoptimizations;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.renderpearl.api.GpuFormat;
import com.mojang.renderpearl.api.pipeline.PrimitiveTopology;
import com.mojang.renderpearl.api.vertex.VertexFormat;
import com.mojang.renderpearl.api.vertex.VertexFormatElement;
import java.nio.ByteBuffer;
import java.util.Arrays;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

public final class FacingCulling {

    public static final int GROUPS   = 7;
    public static final int RANGES   = GROUPS * 2;
    public static final int OCCUPIED = GROUPS + 3;

    private static final int    UNASSIGNED         = 3;
    private static final int    OPPOSITE           = GROUPS - 1;
    private static final int    PLANES             = GROUPS;
    private static final int    AXES               = 3;
    private static final int    X                  = 0;
    private static final int    Y                  = 1;
    private static final int    Z                  = 2;
    private static final int    SEGMENT_FIELD_BITS = 3;
    private static final int    SEGMENT_FIELD      = (1 << SEGMENT_FIELD_BITS) - 1;
    private static final int    VERTICES_PER_QUAD  = PrimitiveTopology.QUADS.primitiveLength;
    private static final int    INDICES_PER_QUAD   = PrimitiveTopology.QUADS.indexCount(VERTICES_PER_QUAD);
    private static final double MARGIN             = 3.0;
    private static final double OFF_AXIS           = 0.5 / 127.0;

    private static final int[] SEGMENTS = segments();

    private static double cameraX;
    private static double cameraY;
    private static double cameraZ;
    private static double eyeEast;
    private static double eyeUp;
    private static double eyeSouth;
    private static double eyeNorth;
    private static double eyeDown;
    private static double eyeWest;

    private FacingCulling() {
    }

    public static void groupQuads(MeshData mesh, int[] ranges, int offset) {
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

        groupVertices(MemoryUtil.memAddress(vertices), position.offset(), vertexSize, quads, ranges, offset);
    }

    public static void beginFrame(Vec3 camera) {
        RenderSystem.assertOnRenderThread();

        cameraX = camera.x;
        cameraY = camera.y;
        cameraZ = camera.z;
    }

    public static void prepareSection(int originX, int originY, int originZ) {
        double eyeX = cameraX - originX;
        double eyeY = cameraY - originY;
        double eyeZ = cameraZ - originZ;

        eyeEast  = eyeX + MARGIN;
        eyeUp    = eyeY + MARGIN;
        eyeSouth = eyeZ + MARGIN;
        eyeNorth = MARGIN - eyeZ;
        eyeDown  = MARGIN - eyeY;
        eyeWest  = MARGIN - eyeX;
    }

    public static int visibleGroups(int[] ranges, int offset) {
        int culled = behind(ranges, offset, OPPOSITE - X, eyeWest) | behind(ranges, offset, OPPOSITE - Y, eyeDown) | behind(ranges, offset, OPPOSITE - Z, eyeNorth) | behind(ranges, offset, X, eyeEast) | behind(ranges, offset, Y, eyeUp) | behind(ranges, offset, Z, eyeSouth);

        return ranges[offset + OCCUPIED] & ~culled;
    }

    public static void addDraws(SectionDrawList draws, int drawGroup, int[] ranges, int offset, int visibleGroups, int baseVertex, int sectionInfo) {
        int segments = SEGMENTS[ranges[offset + OCCUPIED] & ~visibleGroups];
        int count    = segments & SEGMENT_FIELD;

        for (int segment = 0; segment < count; segment ++) {
            int shift = SEGMENT_FIELD_BITS * (1 + 2 * segment);
            int first = segments >>> shift & SEGMENT_FIELD;
            int start = first == 0 ? 0 : ranges[offset + first - 1];
            int end   = ranges[offset + (segments >>> shift + SEGMENT_FIELD_BITS & SEGMENT_FIELD)];

            if (end != start)
                draws.add(drawGroup, (end - start) * INDICES_PER_QUAD, start * INDICES_PER_QUAD, baseVertex, sectionInfo);
        }
    }

    private static void groupVertices(long base, int positionOffset, int vertexSize, int quads, int[] target, int offset) {
        int     quadSize     = vertexSize * VERTICES_PER_QUAD;
        long    positions    = base + positionOffset;
        int[]   destinations = new int[quads];
        int[]   ranges       = new int[RANGES];
        int[]   next         = new int[GROUPS];
        float[] planes       = new float[GROUPS];

        Arrays.fill(planes, Float.POSITIVE_INFINITY);

        for (int quad = 0; quad < quads; quad ++) {
            long first = positions + (long) quad * quadSize;
            int  group = classify(first, vertexSize);

            destinations[quad] = group;
            ranges[group] ++;

            if (group != UNASSIGNED)
                planes[group] = Math.min(planes[group], signedPlane(first, vertexSize, group));
        }

        int end      = 0;
        int occupied = 0;

        for (int group = 0; group < GROUPS; group ++) {
            if (ranges[group] != 0)
                occupied |= 1 << group;

            next[group]            = end;
            end                   += ranges[group];
            ranges[group]          = end;
            ranges[PLANES + group] = Float.floatToRawIntBits(planes[group]);
        }

        ranges[OCCUPIED] = occupied;

        for (int quad = 0; quad < quads; quad ++)
            destinations[quad] = next[destinations[quad]] ++;

        permute(base, quadSize, destinations);
        System.arraycopy(ranges, 0, target, offset, RANGES);
    }

    private static int behind(int[] ranges, int offset, int group, double signedEye) {
        return Float.intBitsToFloat(ranges[offset + PLANES + group]) >= signedEye ? 1 << group : 0;
    }

    private static int[] segments() {
        int[] segments = new int[1 << GROUPS];

        for (int culled = 0; culled < segments.length; culled ++) {
            int packed = 0;
            int count  = 0;
            int first  = -1;

            for (int group = 0; group <= GROUPS; group ++) {
                if (group < GROUPS && (culled & 1 << group) == 0) {
                    if (first == -1)
                        first = group;

                    continue;
                }

                if (first == -1)
                    continue;

                packed |= (first | group - 1 << SEGMENT_FIELD_BITS) << SEGMENT_FIELD_BITS * (1 + 2 * count);
                count ++;
                first = -1;
            }

            segments[culled] = packed | count;
        }

        return segments;
    }

    private static int classify(long first, int vertexSize) {
        long   second = first + vertexSize;
        long   third  = second + vertexSize;
        long   fourth = third + vertexSize;
        double ax     = coordinate(third, X) - coordinate(first, X);
        double ay     = coordinate(third, Y) - coordinate(first, Y);
        double az     = coordinate(third, Z) - coordinate(first, Z);
        double bx     = coordinate(fourth, X) - coordinate(second, X);
        double by     = coordinate(fourth, Y) - coordinate(second, Y);
        double bz     = coordinate(fourth, Z) - coordinate(second, Z);
        double nx     = ay * bz - az * by;
        double ny     = az * bx - ax * bz;
        double nz     = ax * by - ay * bx;
        double length = Math.sqrt(nx * nx + ny * ny + nz * nz);

        if (!(length > 0.0))
            return UNASSIGNED;

        double limit = OFF_AXIS * length;

        if (Math.abs(ny) < limit && Math.abs(nz) < limit)
            return nx > 0.0 ? X : OPPOSITE - X;

        if (Math.abs(nx) < limit && Math.abs(nz) < limit)
            return ny > 0.0 ? Y : OPPOSITE - Y;

        if (Math.abs(nx) < limit && Math.abs(ny) < limit)
            return nz > 0.0 ? Z : OPPOSITE - Z;

        return UNASSIGNED;
    }

    private static float signedPlane(long first, int vertexSize, int group) {
        int   axis  = axis(group);
        float plane = Float.POSITIVE_INFINITY;

        for (int vertex = 0; vertex < VERTICES_PER_QUAD; vertex ++) {
            float value = coordinate(first + (long) vertex * vertexSize, axis);

            plane = Math.min(plane, group < UNASSIGNED ? value : -value);
        }

        return plane;
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
