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

    private static final int GROUPS         = 11;
    private static final int LAST           = GROUPS - 1;
    private static final int PLANE_BITS     = 10;
    private static final int PLANES_PER_INT = Integer.SIZE / PLANE_BITS;

    public static final int RANGES = LAST + (GROUPS + PLANES_PER_INT - 1) / PLANES_PER_INT;

    private static final int    UP                = 0;
    private static final int    OTHER             = 1;
    private static final int    EAST              = 2;
    private static final int    NORTH_EAST        = 3;
    private static final int    NORTH             = 4;
    private static final int    NORTH_WEST        = 5;
    private static final int    WEST              = 6;
    private static final int    SOUTH_WEST        = 7;
    private static final int    SOUTH             = 8;
    private static final int    SOUTH_EAST        = 9;
    private static final int    DOWN              = 10;
    private static final int    PLANES            = LAST;
    private static final int    PLANE_SHIFT       = Integer.SIZE - PLANE_BITS;
    private static final int    PLANE_MASK        = (1 << PLANE_BITS) - 1;
    private static final int    LOWEST_PLANE      = -(1 << PLANE_BITS - 1);
    private static final int    HIGHEST_PLANE     = (1 << PLANE_BITS - 1) - 1;
    private static final double PLANE_UNITS       = 8.0;
    private static final double MARGIN            = 3.0 * PLANE_UNITS;
    private static final double DIAGONAL_MARGIN   = MARGIN * Math.sqrt(2.0);
    private static final int    X                 = 0;
    private static final int    Y                 = 1;
    private static final int    Z                 = 2;
    private static final int    VERTICES_PER_QUAD = PrimitiveTopology.QUADS.primitiveLength;
    private static final int    INDICES_PER_QUAD  = PrimitiveTopology.QUADS.indexCount(VERTICES_PER_QUAD);
    private static final int    QUAD_BITS         = 27;
    private static final int    QUAD_MASK         = (1 << QUAD_BITS) - 1;
    private static final int    GROUP_SHIFT       = QUAD_BITS + Integer.SIZE;
    private static final float  DEPTH_STEPS       = 1024.0F;
    private static final double OFF_AXIS          = 0.5 / 127.0;
    private static final int[]  NORMAL_X          = {0, 0, 1, 1, 0, -1, -1, -1, 0, 1, 0};
    private static final int[]  NORMAL_Y          = {1, 0, 0, 0, 0, 0, 0, 0, 0, 0, -1};
    private static final int[]  NORMAL_Z          = {0, 0, 0, -1, -1, -1, 0, 1, 1, 1, 0};

    private static double cameraX;
    private static double cameraY;
    private static double cameraZ;
    private static double eyeUp;
    private static double eyeEast;
    private static double eyeNorthEast;
    private static double eyeNorth;
    private static double eyeNorthWest;
    private static double eyeWest;
    private static double eyeSouthWest;
    private static double eyeSouth;
    private static double eyeSouthEast;
    private static double eyeDown;

    private FacingCulling() {
    }

    public static void groupQuads(MeshData mesh, int indexCount, int[] ranges, int offset) {
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

        if ((long) quads * INDICES_PER_QUAD != indexCount)
            throw new IllegalStateException(quads + " quads do not match the " + indexCount + " indices drawn for the mesh");

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
        double eyeX = (cameraX - originX) * PLANE_UNITS;
        double eyeY = (cameraY - originY) * PLANE_UNITS;
        double eyeZ = (cameraZ - originZ) * PLANE_UNITS;

        eyeUp        = eyeY + MARGIN;
        eyeEast      = eyeX + MARGIN;
        eyeNorthEast = eyeX - eyeZ + DIAGONAL_MARGIN;
        eyeNorth     = MARGIN - eyeZ;
        eyeNorthWest = DIAGONAL_MARGIN - eyeX - eyeZ;
        eyeWest      = MARGIN - eyeX;
        eyeSouthWest = eyeZ - eyeX + DIAGONAL_MARGIN;
        eyeSouth     = eyeZ + MARGIN;
        eyeSouthEast = eyeX + eyeZ + DIAGONAL_MARGIN;
        eyeDown      = MARGIN - eyeY;
    }

    public static void addDraws(SectionDrawList draws, int layer, int vertexBuffer, int[] ranges, int offset, int indexCount, int baseVertex, int sectionInfo) {
        int quads  = indexCount / INDICES_PER_QUAD;
        int culled = culledGroups(ranges, offset);
        int run    = 0;

        while (culled != 0) {
            int group = Integer.numberOfTrailingZeros(culled);
            int start = group == 0 ? 0 : ranges[offset + group - 1];
            int end   = group == LAST ? quads : ranges[offset + group];

            if (end != start) {
                addRange(draws, layer, vertexBuffer, run, start, baseVertex, sectionInfo);
                run = end;
            }

            culled &= culled - 1;
        }

        addRange(draws, layer, vertexBuffer, run, quads, baseVertex, sectionInfo);
    }

    private static int culledGroups(int[] ranges, int offset) {
        return behind(ranges, offset, UP, eyeUp) | behind(ranges, offset, EAST, eyeEast) | behind(ranges, offset, NORTH_EAST, eyeNorthEast) | behind(ranges, offset, NORTH, eyeNorth) | behind(ranges, offset, NORTH_WEST, eyeNorthWest) | behind(ranges, offset, WEST, eyeWest) | behind(ranges, offset, SOUTH_WEST, eyeSouthWest) | behind(ranges, offset, SOUTH, eyeSouth) | behind(ranges, offset, SOUTH_EAST, eyeSouthEast) | behind(ranges, offset, DOWN, eyeDown);
    }

    private static int behind(int[] ranges, int offset, int group, double signedEye) {
        return ranges[offset + PLANES + group / PLANES_PER_INT] << PLANE_SHIFT - PLANE_BITS * (group % PLANES_PER_INT) >> PLANE_SHIFT >= signedEye ? 1 << group : 0;
    }

    private static void addRange(SectionDrawList draws, int layer, int vertexBuffer, int start, int end, int baseVertex, int sectionInfo) {
        if (end > start)
            draws.add(draws.group(layer, false, vertexBuffer, DrawRecords.NO_BUFFER, 0), (end - start) * INDICES_PER_QUAD, start * INDICES_PER_QUAD, baseVertex, sectionInfo);
    }

    private static void groupVertices(long base, int positionOffset, int vertexSize, int quads, int[] target, int offset) {
        if (quads > QUAD_MASK + 1)
            throw new IllegalStateException(quads + " quads do not fit the " + QUAD_BITS + " bit quad numbers of the face direction order");

        int    quadSize     = vertexSize * VERTICES_PER_QUAD;
        long   positions    = base + positionOffset;
        long[] order        = new long[quads];
        int[]  destinations = new int[quads];
        int[]  counts       = new int[GROUPS];
        int[]  planes       = new int[GROUPS];
        int[]  ranges       = new int[RANGES];

        Arrays.fill(planes, HIGHEST_PLANE);

        for (int quad = 0; quad < quads; quad ++) {
            long first = positions + (long) quad * quadSize;
            int  group = classify(first, vertexSize);
            int  depth = 0;

            if (group != OTHER) {
                float plane     = plane(first, vertexSize, group);
                int   quantized = (int) Math.floor(plane * PLANE_UNITS);

                if (quantized < LOWEST_PLANE) {
                    group = OTHER;
                } else {
                    planes[group] = Math.min(planes[group], quantized);
                    depth         = Math.round(plane * DEPTH_STEPS) ^ Integer.MAX_VALUE;
                }
            }

            counts[group] ++;
            order[quad] = (long) group << GROUP_SHIFT | (depth & 0xFFFFFFFFL) << QUAD_BITS | quad;
        }

        int end = 0;

        for (int group = 0; group < LAST; group ++) {
            end          += counts[group];
            ranges[group] = end;
        }

        for (int group = 0; group < GROUPS; group ++)
            ranges[PLANES + group / PLANES_PER_INT] |= (planes[group] & PLANE_MASK) << PLANE_BITS * (group % PLANES_PER_INT);

        Arrays.sort(order);

        for (int position = 0; position < quads; position ++)
            destinations[(int) (order[position] & QUAD_MASK)] = position;

        permute(base, quadSize, destinations);
        System.arraycopy(ranges, 0, target, offset, RANGES);
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
            return OTHER;

        double limit = OFF_AXIS * length;

        if (Math.abs(ny) < limit && Math.abs(nz) < limit)
            return nx > 0.0 ? EAST : WEST;

        if (Math.abs(nx) < limit && Math.abs(nz) < limit)
            return ny > 0.0 ? UP : DOWN;

        if (Math.abs(nx) < limit && Math.abs(ny) < limit)
            return nz > 0.0 ? SOUTH : NORTH;

        if (Math.abs(ny) >= limit || Math.abs(Math.abs(nx) - Math.abs(nz)) >= limit)
            return OTHER;

        if (nx > 0.0)
            return nz > 0.0 ? SOUTH_EAST : NORTH_EAST;

        return nz > 0.0 ? SOUTH_WEST : NORTH_WEST;
    }

    private static float plane(long first, int vertexSize, int group) {
        float plane = Float.POSITIVE_INFINITY;

        for (int vertex = 0; vertex < VERTICES_PER_QUAD; vertex ++) {
            long position = first + (long) vertex * vertexSize;

            plane = Math.min(plane, NORMAL_X[group] * coordinate(position, X) + NORMAL_Y[group] * coordinate(position, Y) + NORMAL_Z[group] * coordinate(position, Z));
        }

        return plane;
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
