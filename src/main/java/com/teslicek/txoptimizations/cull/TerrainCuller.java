package com.teslicek.txoptimizations.cull;

import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import java.util.Arrays;
import net.caffeinemc.mods.sodium.client.render.chunk.ChunkRenderMatrices;
import com.teslicek.txoptimizations.mixin.cull.RenderSectionManagerInvoker;
import net.caffeinemc.mods.sodium.client.render.chunk.LocalSectionIndex;
import net.caffeinemc.mods.sodium.client.render.chunk.RenderSection;
import net.caffeinemc.mods.sodium.client.render.chunk.RenderSectionFlags;
import net.caffeinemc.mods.sodium.client.render.chunk.lists.ChunkRenderList;
import net.caffeinemc.mods.sodium.client.render.chunk.lists.ChunkRenderListIterable;
import net.caffeinemc.mods.sodium.client.render.chunk.region.RenderRegion;
import net.caffeinemc.mods.sodium.client.util.iterator.ByteIterator;
import net.minecraft.core.SectionPos;
import org.joml.Matrix4f;

public final class TerrainCuller {

    private static final int                     WIDTH           = 256;
    private static final int                     HEIGHT          = 144;
    private static final int                     OCCLUDER_RANGE  = 6;
    private static final int                     CORNERS         = 4;
    private static final int                     MASK_WORDS      = 4;
    private static final double                  NEAR            = 0.1;
    private static final double                  EDGE_MARGIN     = 0.05;
    private static final double                  DEPTH_MARGIN    = 1.0e-3;
    private static final double                  DEPTH_RELATIVE  = 1.0e-4;
    private static final double                  SECTION_SIZE    = OccluderBoxes.SIZE;
    private static final float[][]               PYRAMID         = createPyramid();
    private static final boolean[]               INSIDE          = new boolean[(WIDTH + 1) * (HEIGHT + 1)];
    private static final double[]                SCREEN_X        = new double[CORNERS];
    private static final double[]                SCREEN_Y        = new double[CORNERS];
    private static final double[]                FACE_X          = new double[CORNERS];
    private static final double[]                FACE_Y          = new double[CORNERS];
    private static final double[]                FACE_Z          = new double[CORNERS];
    private static final long[]                  HIDDEN          = new long[MASK_WORDS];
    private static final float[]                 LAST_MATRIX     = new float[16];
    private static final float[]                 MATRIX          = new float[16];
    private static final Matrix4f                VIEW_PROJECTION = new Matrix4f();
    private static final LongOpenHashSet         DRAWN           = new LongOpenHashSet();
    private static final LongArrayList           DRAWN_ORDER     = new LongArrayList();
    private static final LongArrayList           LAST_DRAWN      = new LongArrayList();
    private static final FilteredSectionIterator FILTERED        = new FilteredSectionIterator();

    private static RenderSectionManagerInvoker sectionManager;
    private static long                        occluderVersion;
    private static long                        lastOccluderVersion = -1L;
    private static double                      lastCameraX;
    private static double                      lastCameraY;
    private static double                      lastCameraZ;
    private static double                      projectedX;
    private static double                      projectedY;
    private static double                      projectedW;

    private TerrainCuller() {
    }

    public static void markOccludersChanged() {
        occluderVersion ++;
    }

    public static boolean isHidden(RenderRegion region, int sectionIndex) {
        long[] hidden = ((RegionOcclusion) region).txoptimizations$getHiddenSections();

        return (hidden[sectionIndex >>> 6] >>> sectionIndex & 1L) == 1L;
    }

    public static ByteIterator filter(ChunkRenderList list, ByteIterator sections) {
        if (sections == null)
            return null;

        long[] hidden = ((RegionOcclusion) list.getRegion()).txoptimizations$getHiddenSections();

        if ((hidden[0] | hidden[1] | hidden[2] | hidden[3]) == 0L)
            return sections;

        FILTERED.reset();

        while (sections.hasNext()) {
            int section = sections.nextByteAsInt();

            if ((hidden[section >>> 6] >>> section & 1L) == 0L)
                FILTERED.add(section);
        }

        return FILTERED.isEmpty() ? null : FILTERED;
    }

    public static void cull(RenderSectionManagerInvoker manager, ChunkRenderListIterable lists, ChunkRenderMatrices matrices, double cameraX, double cameraY, double cameraZ) {
        sectionManager = manager;
        VIEW_PROJECTION.set(matrices.projection()).mul(matrices.modelView()).get(MATRIX);
        collectDrawn(lists);

        if (isUnchanged(cameraX, cameraY, cameraZ))
            return;

        remember(cameraX, cameraY, cameraZ);
        Arrays.fill(PYRAMID[0], Float.POSITIVE_INFINITY);
        rasterizeOccluders(lists, cameraX, cameraY, cameraZ);
        buildPyramid();
        testSections(lists, cameraX, cameraY, cameraZ);
    }

    private static void collectDrawn(ChunkRenderListIterable lists) {
        DRAWN.clear();
        DRAWN_ORDER.clear();

        for (ChunkRenderList list : iterable(lists)) {
            RenderRegion region   = list.getRegion();
            ByteIterator sections = list.sectionsWithGeometryIterator(false);

            if (sections == null)
                continue;

            while (sections.hasNext()) {
                int  section = sections.nextByteAsInt();
                long key     = sectionKey(region, section);

                DRAWN.add(key);
                DRAWN_ORDER.add(key);
            }
        }
    }

    private static boolean isUnchanged(double cameraX, double cameraY, double cameraZ) {
        return occluderVersion == lastOccluderVersion && cameraX == lastCameraX && cameraY == lastCameraY && cameraZ == lastCameraZ && Arrays.equals(MATRIX, LAST_MATRIX) && DRAWN_ORDER.equals(LAST_DRAWN);
    }

    private static void remember(double cameraX, double cameraY, double cameraZ) {
        lastOccluderVersion = occluderVersion;
        lastCameraX         = cameraX;
        lastCameraY         = cameraY;
        lastCameraZ         = cameraZ;

        System.arraycopy(MATRIX, 0, LAST_MATRIX, 0, MATRIX.length);
        LAST_DRAWN.clear();
        LAST_DRAWN.addAll(DRAWN_ORDER);
    }

    private static void rasterizeOccluders(ChunkRenderListIterable lists, double cameraX, double cameraY, double cameraZ) {
        int cameraSectionX = SectionPos.blockToSectionCoord(cameraX);
        int cameraSectionY = SectionPos.blockToSectionCoord(cameraY);
        int cameraSectionZ = SectionPos.blockToSectionCoord(cameraZ);

        for (ChunkRenderList list : iterable(lists)) {
            RenderRegion region   = list.getRegion();
            ByteIterator sections = list.sectionsWithGeometryIterator(false);

            if (sections == null)
                continue;

            while (sections.hasNext()) {
                int section = sections.nextByteAsInt();
                int chunkX  = region.getChunkX() + LocalSectionIndex.unpackX(section);
                int chunkY  = region.getChunkY() + LocalSectionIndex.unpackY(section);
                int chunkZ  = region.getChunkZ() + LocalSectionIndex.unpackZ(section);

                if (Math.abs(chunkX - cameraSectionX) > OCCLUDER_RANGE || Math.abs(chunkY - cameraSectionY) > OCCLUDER_RANGE || Math.abs(chunkZ - cameraSectionZ) > OCCLUDER_RANGE)
                    continue;

                int[] boxes = ((RegionOcclusion) region).txoptimizations$getOccluders(section);

                if (boxes == null)
                    continue;

                for (int box : boxes)
                    rasterizeBox(box, chunkX, chunkY, chunkZ, cameraX, cameraY, cameraZ);
            }
        }
    }

    private static void rasterizeBox(int box, int chunkX, int chunkY, int chunkZ, double cameraX, double cameraY, double cameraZ) {
        double minX = SectionPos.sectionToBlockCoord(chunkX) + OccluderBoxes.minX(box) - cameraX;
        double minY = SectionPos.sectionToBlockCoord(chunkY) + OccluderBoxes.minY(box) - cameraY;
        double minZ = SectionPos.sectionToBlockCoord(chunkZ) + OccluderBoxes.minZ(box) - cameraZ;
        double maxX = SectionPos.sectionToBlockCoord(chunkX) + OccluderBoxes.maxX(box) + 1 - cameraX;
        double maxY = SectionPos.sectionToBlockCoord(chunkY) + OccluderBoxes.maxY(box) + 1 - cameraY;
        double maxZ = SectionPos.sectionToBlockCoord(chunkZ) + OccluderBoxes.maxZ(box) + 1 - cameraZ;
        int    last = OccluderBoxes.SIZE - 1;

        if (minX > 0.0 && isExposed(OccluderBoxes.minX(box) == 0, chunkX - 1, chunkY, chunkZ))
            rasterizeFace(minX, minY, minZ, minX, maxY, minZ, minX, maxY, maxZ, minX, minY, maxZ);

        if (maxX < 0.0 && isExposed(OccluderBoxes.maxX(box) == last, chunkX + 1, chunkY, chunkZ))
            rasterizeFace(maxX, minY, minZ, maxX, maxY, minZ, maxX, maxY, maxZ, maxX, minY, maxZ);

        if (minY > 0.0 && isExposed(OccluderBoxes.minY(box) == 0, chunkX, chunkY - 1, chunkZ))
            rasterizeFace(minX, minY, minZ, maxX, minY, minZ, maxX, minY, maxZ, minX, minY, maxZ);

        if (maxY < 0.0 && isExposed(OccluderBoxes.maxY(box) == last, chunkX, chunkY + 1, chunkZ))
            rasterizeFace(minX, maxY, minZ, maxX, maxY, minZ, maxX, maxY, maxZ, minX, maxY, maxZ);

        if (minZ > 0.0 && isExposed(OccluderBoxes.minZ(box) == 0, chunkX, chunkY, chunkZ - 1))
            rasterizeFace(minX, minY, minZ, maxX, minY, minZ, maxX, maxY, minZ, minX, maxY, minZ);

        if (maxZ < 0.0 && isExposed(OccluderBoxes.maxZ(box) == last, chunkX, chunkY, chunkZ + 1))
            rasterizeFace(minX, minY, maxZ, maxX, minY, maxZ, maxX, maxY, maxZ, minX, maxY, maxZ);
    }

    private static boolean isExposed(boolean onSectionBorder, int neighborX, int neighborY, int neighborZ) {
        if (!onSectionBorder || DRAWN.contains(SectionPos.asLong(neighborX, neighborY, neighborZ)))
            return true;

        RenderSection neighbor = sectionManager.txoptimizations$getRenderSection(neighborX, neighborY, neighborZ);

        if (neighbor == null)
            return false;

        int flags = neighbor.getRegion().getSectionFlags(neighbor.getSectionIndex());

        return (flags & RenderSectionFlags.MASK_IS_BUILT) != 0 && (flags & RenderSectionFlags.MASK_HAS_BLOCK_GEOMETRY) == 0;
    }

    private static void rasterizeFace(double x0, double y0, double z0, double x1, double y1, double z1, double x2, double y2, double z2, double x3, double y3, double z3) {
        FACE_X[0] = x0;
        FACE_Y[0] = y0;
        FACE_Z[0] = z0;
        FACE_X[1] = x1;
        FACE_Y[1] = y1;
        FACE_Z[1] = z1;
        FACE_X[2] = x2;
        FACE_Y[2] = y2;
        FACE_Z[2] = z2;
        FACE_X[3] = x3;
        FACE_Y[3] = y3;
        FACE_Z[3] = z3;

        double farthest = 0.0;
        double minX     = Double.POSITIVE_INFINITY;
        double minY     = Double.POSITIVE_INFINITY;
        double maxX     = Double.NEGATIVE_INFINITY;
        double maxY     = Double.NEGATIVE_INFINITY;

        for (int corner = 0; corner < CORNERS; corner ++) {
            project(FACE_X[corner], FACE_Y[corner], FACE_Z[corner]);

            if (projectedW <= NEAR)
                return;

            SCREEN_X[corner] = projectedX;
            SCREEN_Y[corner] = projectedY;
            farthest         = Math.max(farthest, projectedW);
            minX             = Math.min(minX, projectedX);
            minY             = Math.min(minY, projectedY);
            maxX             = Math.max(maxX, projectedX);
            maxY             = Math.max(maxY, projectedY);
        }

        int gridMinX = Math.max(0, (int) Math.ceil(minX));
        int gridMinY = Math.max(0, (int) Math.ceil(minY));
        int gridMaxX = Math.min(WIDTH, (int) Math.floor(maxX));
        int gridMaxY = Math.min(HEIGHT, (int) Math.floor(maxY));

        if (gridMaxX <= gridMinX || gridMaxY <= gridMinY)
            return;

        double area = 0.0;

        for (int corner = 0; corner < CORNERS; corner ++) {
            int next = (corner + 1) % CORNERS;

            area += SCREEN_X[corner] * SCREEN_Y[next] - SCREEN_X[next] * SCREEN_Y[corner];
        }

        if (area == 0.0)
            return;

        double orientation = Math.signum(area);
        int    gridWidth   = gridMaxX - gridMinX + 1;

        for (int gridY = gridMinY; gridY <= gridMaxY; gridY ++) {
            for (int gridX = gridMinX; gridX <= gridMaxX; gridX ++)
                INSIDE[(gridY - gridMinY) * gridWidth + gridX - gridMinX] = isInside(gridX, gridY, orientation);
        }

        float   depth = Math.nextUp((float) farthest);
        float[] level = PYRAMID[0];

        for (int pixelY = gridMinY; pixelY < gridMaxY; pixelY ++) {
            int row = (pixelY - gridMinY) * gridWidth;

            for (int pixelX = gridMinX; pixelX < gridMaxX; pixelX ++) {
                int corner = row + pixelX - gridMinX;

                if (INSIDE[corner] && INSIDE[corner + 1] && INSIDE[corner + gridWidth] && INSIDE[corner + gridWidth + 1]) {
                    int pixel = pixelY * WIDTH + pixelX;

                    level[pixel] = Math.min(level[pixel], depth);
                }
            }
        }
    }

    private static boolean isInside(double pointX, double pointY, double orientation) {
        for (int corner = 0; corner < CORNERS; corner ++) {
            int    next  = (corner + 1) % CORNERS;
            double edgeX = SCREEN_X[next] - SCREEN_X[corner];
            double edgeY = SCREEN_Y[next] - SCREEN_Y[corner];
            double cross = edgeX * (pointY - SCREEN_Y[corner]) - edgeY * (pointX - SCREEN_X[corner]);

            if (orientation * cross < EDGE_MARGIN * Math.sqrt(edgeX * edgeX + edgeY * edgeY))
                return false;
        }

        return true;
    }

    private static void buildPyramid() {
        int width  = WIDTH;
        int height = HEIGHT;

        for (int level = 1; level < PYRAMID.length; level ++) {
            float[] source       = PYRAMID[level - 1];
            float[] target       = PYRAMID[level];
            int     targetWidth  = (width + 1) >> 1;
            int     targetHeight = (height + 1) >> 1;

            for (int y = 0; y < targetHeight; y ++) {
                int sourceY0 = y << 1;
                int sourceY1 = Math.min(sourceY0 + 1, height - 1);

                for (int x = 0; x < targetWidth; x ++) {
                    int sourceX0 = x << 1;
                    int sourceX1 = Math.min(sourceX0 + 1, width - 1);

                    target[y * targetWidth + x] = Math.max(Math.max(source[sourceY0 * width + sourceX0], source[sourceY0 * width + sourceX1]), Math.max(source[sourceY1 * width + sourceX0], source[sourceY1 * width + sourceX1]));
                }
            }

            width  = targetWidth;
            height = targetHeight;
        }
    }

    private static void testSections(ChunkRenderListIterable lists, double cameraX, double cameraY, double cameraZ) {
        for (ChunkRenderList list : iterable(lists)) {
            RenderRegion region   = list.getRegion();
            ByteIterator sections = list.sectionsWithGeometryIterator(false);

            Arrays.fill(HIDDEN, 0L);

            if (sections != null) {
                while (sections.hasNext()) {
                    int    section = sections.nextByteAsInt();
                    double minX    = SectionPos.sectionToBlockCoord(region.getChunkX() + LocalSectionIndex.unpackX(section)) - cameraX;
                    double minY    = SectionPos.sectionToBlockCoord(region.getChunkY() + LocalSectionIndex.unpackY(section)) - cameraY;
                    double minZ    = SectionPos.sectionToBlockCoord(region.getChunkZ() + LocalSectionIndex.unpackZ(section)) - cameraZ;

                    if (isOccluded(minX, minY, minZ))
                        HIDDEN[section >>> 6] |= 1L << section;
                }
            }

            long[] current = ((RegionOcclusion) region).txoptimizations$getHiddenSections();

            if (!Arrays.equals(current, HIDDEN)) {
                System.arraycopy(HIDDEN, 0, current, 0, MASK_WORDS);
                region.clearAllCachedBatches();
            }
        }
    }

    private static boolean isOccluded(double minX, double minY, double minZ) {
        double nearest = Double.POSITIVE_INFINITY;
        double left    = Double.POSITIVE_INFINITY;
        double bottom  = Double.POSITIVE_INFINITY;
        double right   = Double.NEGATIVE_INFINITY;
        double top     = Double.NEGATIVE_INFINITY;

        for (int corner = 0; corner < 8; corner ++) {
            project(minX + ((corner & 1) == 0 ? 0.0 : SECTION_SIZE), minY + ((corner & 2) == 0 ? 0.0 : SECTION_SIZE), minZ + ((corner & 4) == 0 ? 0.0 : SECTION_SIZE));

            if (projectedW <= NEAR)
                return false;

            nearest = Math.min(nearest, projectedW);
            left    = Math.min(left, projectedX);
            bottom  = Math.min(bottom, projectedY);
            right   = Math.max(right, projectedX);
            top     = Math.max(top, projectedY);
        }

        if (right < 0.0 || top < 0.0 || left >= WIDTH || bottom >= HEIGHT)
            return false;

        int pixelMinX = Math.max(0, (int) Math.floor(left) - 1);
        int pixelMinY = Math.max(0, (int) Math.floor(bottom) - 1);
        int pixelMaxX = Math.min(WIDTH - 1, (int) Math.floor(right) + 1);
        int pixelMaxY = Math.min(HEIGHT - 1, (int) Math.floor(top) + 1);
        int level     = 0;

        while ((pixelMaxX >> level) - (pixelMinX >> level) > 1 || (pixelMaxY >> level) - (pixelMinY >> level) > 1)
            level ++;

        float[] depths     = PYRAMID[level];
        int     levelWidth = levelSize(WIDTH, level);
        float   farthest   = 0.0F;

        for (int y = pixelMinY >> level; y <= pixelMaxY >> level; y ++) {
            for (int x = pixelMinX >> level; x <= pixelMaxX >> level; x ++)
                farthest = Math.max(farthest, depths[y * levelWidth + x]);
        }

        return farthest + DEPTH_MARGIN + nearest * DEPTH_RELATIVE < nearest;
    }

    private static void project(double x, double y, double z) {
        double clipX = MATRIX[0] * x + MATRIX[4] * y + MATRIX[8] * z + MATRIX[12];
        double clipY = MATRIX[1] * x + MATRIX[5] * y + MATRIX[9] * z + MATRIX[13];
        double clipW = MATRIX[3] * x + MATRIX[7] * y + MATRIX[11] * z + MATRIX[15];

        projectedW = clipW;
        projectedX = (clipX / clipW * 0.5 + 0.5) * WIDTH;
        projectedY = (clipY / clipW * 0.5 + 0.5) * HEIGHT;
    }

    private static long sectionKey(RenderRegion region, int section) {
        return SectionPos.asLong(region.getChunkX() + LocalSectionIndex.unpackX(section), region.getChunkY() + LocalSectionIndex.unpackY(section), region.getChunkZ() + LocalSectionIndex.unpackZ(section));
    }

    private static Iterable<ChunkRenderList> iterable(ChunkRenderListIterable lists) {
        return () -> lists.iterator(false);
    }

    private static int levelSize(int size, int level) {
        int result = size;

        for (int index = 0; index < level; index ++)
            result = (result + 1) >> 1;

        return result;
    }

    private static float[][] createPyramid() {
        int levels = 1;
        int width  = WIDTH;
        int height = HEIGHT;

        while (width > 1 || height > 1) {
            width  = (width + 1) >> 1;
            height = (height + 1) >> 1;
            levels ++;
        }

        float[][] pyramid = new float[levels][];

        for (int level = 0; level < levels; level ++)
            pyramid[level] = new float[levelSize(WIDTH, level) * levelSize(HEIGHT, level)];

        return pyramid;
    }
}
