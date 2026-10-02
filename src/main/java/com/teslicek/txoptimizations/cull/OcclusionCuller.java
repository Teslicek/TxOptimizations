package com.teslicek.txoptimizations.cull;

import com.teslicek.txoptimizations.mixin.cull.ChunkRenderListAccessor;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import java.util.Arrays;
import java.util.Iterator;
import net.caffeinemc.mods.sodium.client.render.chunk.ChunkRenderMatrices;
import net.caffeinemc.mods.sodium.client.render.chunk.LocalSectionIndex;
import net.caffeinemc.mods.sodium.client.render.chunk.lists.ChunkRenderList;
import net.caffeinemc.mods.sodium.client.render.chunk.lists.ChunkRenderListIterable;
import net.caffeinemc.mods.sodium.client.render.chunk.region.RenderRegion;
import net.caffeinemc.mods.sodium.client.util.iterator.ByteIterator;
import net.minecraft.core.SectionPos;
import org.joml.Matrix4f;

public final class OcclusionCuller {

    private static final int                                WIDTH           = DepthReadback.WIDTH;
    private static final int                                HEIGHT          = DepthReadback.HEIGHT;
    private static final int                                MASK_WORDS      = 4;
    private static final float                              SECTION_SIZE    = 16.0F;
    private static final float                              NEAR            = 0.1F;
    private static final float                              SCREEN_MARGIN   = 0.01F;
    private static final float                              DEPTH_RELATIVE  = 1.0e-3F;
    private static final int                                LEVEL_SPAN      = 4;
    private static final float[][]                          PYRAMID         = createPyramid();
    private static final int[]                              LEVEL_WIDTHS    = levelWidths();
    private static final float[]                            MATRIX          = new float[16];
    private static final float[]                            TESTED_DEPTH    = new float[WIDTH * HEIGHT];
    private static final float[]                            TESTED_MATRIX   = new float[16];
    private static final float[]                            CORNER_OFFSETS  = new float[32];
    private static final long[]                             HIDDEN          = new long[MASK_WORDS];
    private static final Matrix4f                           VIEW_PROJECTION = new Matrix4f();
    private static final ReferenceOpenHashSet<RenderRegion> REGIONS         = new ReferenceOpenHashSet<>();
    private static final ReferenceOpenHashSet<RenderRegion> CULLED_REGIONS  = new ReferenceOpenHashSet<>();
    private static final FilteredSectionIterator            FILTERED        = new FilteredSectionIterator();

    private static boolean culling;
    private static boolean sectionsChanged;
    private static int     testGeneration;
    private static int     testedRegionCount;
    private static long    testedVersion;
    private static double  testedX;
    private static double  testedY;
    private static double  testedZ;

    private OcclusionCuller() {
    }

    public static boolean isHidden(RenderRegion region, int sectionIndex) {
        long[] hidden = ((RegionOcclusion) region).txoptimizations$getHiddenSections();

        return (hidden[sectionIndex >>> 6] >>> sectionIndex & 1L) == 1L;
    }

    public static ByteIterator filter(ChunkRenderList list, ByteIterator sections) {
        if (sections == null || !CULLED_REGIONS.contains(list.getRegion()))
            return sections;

        long[] hidden = ((RegionOcclusion) list.getRegion()).txoptimizations$getHiddenSections();

        FILTERED.reset();

        while (sections.hasNext()) {
            int section = sections.nextByteAsInt();

            if ((hidden[section >>> 6] >>> section & 1L) == 0L)
                FILTERED.add(section);
        }

        return FILTERED.isEmpty() ? null : FILTERED;
    }

    public static void cull(ChunkRenderListIterable lists, ChunkRenderMatrices matrices, double cameraX, double cameraY, double cameraZ) {
        VIEW_PROJECTION.set(matrices.projection()).mul(matrices.modelView()).get(MATRIX);
        collectSections(lists);
        releaseAbsentRegions();

        if (!GpuWaitMeter.isGpuBound()) {
            stopCulling();

            return;
        }

        DepthReadback.describeFrame(MATRIX, cameraX, cameraY, cameraZ);

        if (DepthReadback.latestVersion() == 0L || isUnchanged())
            return;

        remember();
        buildPyramid();
        testSections(lists);
        culling = true;
    }

    public static void reset() {
        stopCulling();
    }

    private static void stopCulling() {
        for (RenderRegion region : CULLED_REGIONS)
            clearHidden(region);

        CULLED_REGIONS.clear();
        DepthReadback.reset();
        culling = false;
    }

    private static void collectSections(ChunkRenderListIterable lists) {
        REGIONS.clear();
        sectionsChanged = false;

        for (ChunkRenderList list : iterable(lists)) {
            RenderRegion    region    = list.getRegion();
            RegionOcclusion occlusion = (RegionOcclusion) region;

            REGIONS.add(region);

            if (!sectionsChanged && (occlusion.txoptimizations$getTestedGeneration() != testGeneration || !Arrays.equals(((ChunkRenderListAccessor) list).txoptimizations$sectionsWithGeometryMap(), occlusion.txoptimizations$getTestedSections())))
                sectionsChanged = true;
        }

        if (REGIONS.size() != testedRegionCount)
            sectionsChanged = true;
    }

    private static void releaseAbsentRegions() {
        Iterator<RenderRegion> iterator = CULLED_REGIONS.iterator();

        while (iterator.hasNext()) {
            RenderRegion region = iterator.next();

            if (REGIONS.contains(region))
                continue;

            clearHidden(region);
            iterator.remove();
        }
    }

    private static void clearHidden(RenderRegion region) {
        Arrays.fill(((RegionOcclusion) region).txoptimizations$getHiddenSections(), 0L);
        region.clearAllCachedBatches();
    }

    private static boolean isUnchanged() {
        if (!culling || sectionsChanged)
            return false;

        if (DepthReadback.latestVersion() == testedVersion)
            return true;

        return DepthReadback.latestX() == testedX && DepthReadback.latestY() == testedY && DepthReadback.latestZ() == testedZ && Arrays.equals(DepthReadback.latestMatrix(), TESTED_MATRIX) && Arrays.equals(DepthReadback.latestDepth(), TESTED_DEPTH);
    }

    private static void remember() {
        testedVersion = DepthReadback.latestVersion();
        testedX       = DepthReadback.latestX();
        testedY       = DepthReadback.latestY();
        testedZ       = DepthReadback.latestZ();

        System.arraycopy(DepthReadback.latestMatrix(), 0, TESTED_MATRIX, 0, TESTED_MATRIX.length);
        System.arraycopy(DepthReadback.latestDepth(), 0, TESTED_DEPTH, 0, TESTED_DEPTH.length);
        testGeneration ++;
        testedRegionCount = REGIONS.size();

        for (int corner = 0; corner < 8; corner ++) {
            float offsetX = (corner & 1) == 0 ? 0.0F : SECTION_SIZE;
            float offsetY = (corner & 2) == 0 ? 0.0F : SECTION_SIZE;
            float offsetZ = (corner & 4) == 0 ? 0.0F : SECTION_SIZE;

            for (int row = 0; row < 4; row ++)
                CORNER_OFFSETS[corner * 4 + row] = TESTED_MATRIX[row] * offsetX + TESTED_MATRIX[4 + row] * offsetY + TESTED_MATRIX[8 + row] * offsetZ;
        }
    }

    private static void testSections(ChunkRenderListIterable lists) {
        for (ChunkRenderList list : iterable(lists)) {
            RenderRegion    region    = list.getRegion();
            RegionOcclusion occlusion = (RegionOcclusion) region;
            ByteIterator    sections  = list.sectionsWithGeometryIterator(false);

            System.arraycopy(((ChunkRenderListAccessor) list).txoptimizations$sectionsWithGeometryMap(), 0, occlusion.txoptimizations$getTestedSections(), 0, MASK_WORDS);
            occlusion.txoptimizations$setTestedGeneration(testGeneration);
            Arrays.fill(HIDDEN, 0L);

            if (sections != null) {
                while (sections.hasNext()) {
                    int section = sections.nextByteAsInt();

                    if (isOccluded(region, section))
                        HIDDEN[section >>> 6] |= 1L << section;
                }
            }

            long[] current = occlusion.txoptimizations$getHiddenSections();

            if (Arrays.equals(current, HIDDEN))
                continue;

            System.arraycopy(HIDDEN, 0, current, 0, MASK_WORDS);
            region.clearAllCachedBatches();

            if ((HIDDEN[0] | HIDDEN[1] | HIDDEN[2] | HIDDEN[3]) == 0L)
                CULLED_REGIONS.remove(region);
            else
                CULLED_REGIONS.add(region);
        }
    }

    private static boolean isOccluded(RenderRegion region, int section) {
        float   originX = (float) (SectionPos.sectionToBlockCoord(region.getChunkX() + LocalSectionIndex.unpackX(section)) - testedX);
        float   originY = (float) (SectionPos.sectionToBlockCoord(region.getChunkY() + LocalSectionIndex.unpackY(section)) - testedY);
        float   originZ = (float) (SectionPos.sectionToBlockCoord(region.getChunkZ() + LocalSectionIndex.unpackZ(section)) - testedZ);
        float[] matrix  = TESTED_MATRIX;
        float   baseX   = matrix[0] * originX + matrix[4] * originY + matrix[8] * originZ + matrix[12];
        float   baseY   = matrix[1] * originX + matrix[5] * originY + matrix[9] * originZ + matrix[13];
        float   baseZ   = matrix[2] * originX + matrix[6] * originY + matrix[10] * originZ + matrix[14];
        float   baseW   = matrix[3] * originX + matrix[7] * originY + matrix[11] * originZ + matrix[15];
        float   nearest = Float.NEGATIVE_INFINITY;
        float   left    = Float.POSITIVE_INFINITY;
        float   bottom  = Float.POSITIVE_INFINITY;
        float   right   = Float.NEGATIVE_INFINITY;
        float   top     = Float.NEGATIVE_INFINITY;

        for (int offset = 0; offset < CORNER_OFFSETS.length; offset += 4) {
            float w = baseW + CORNER_OFFSETS[offset + 3];

            if (w <= NEAR)
                return false;

            float inverseW = 1.0F / w;
            float screenX  = ((baseX + CORNER_OFFSETS[offset]) * inverseW * 0.5F + 0.5F) * WIDTH;
            float screenY  = ((baseY + CORNER_OFFSETS[offset + 1]) * inverseW * 0.5F + 0.5F) * HEIGHT;

            nearest = Math.max(nearest, (baseZ + CORNER_OFFSETS[offset + 2]) * inverseW);
            left    = Math.min(left, screenX);
            bottom  = Math.min(bottom, screenY);
            right   = Math.max(right, screenX);
            top     = Math.max(top, screenY);
        }

        if (right + SCREEN_MARGIN < 0.0F || top + SCREEN_MARGIN < 0.0F || left - SCREEN_MARGIN >= WIDTH || bottom - SCREEN_MARGIN >= HEIGHT)
            return false;

        int pixelMinX = Math.max((int) (left - SCREEN_MARGIN), 0);
        int pixelMinY = Math.max((int) (bottom - SCREEN_MARGIN), 0);
        int pixelMaxX = Math.min((int) (right + SCREEN_MARGIN), WIDTH - 1);
        int pixelMaxY = Math.min((int) (top + SCREEN_MARGIN), HEIGHT - 1);
        int level     = 0;

        while ((pixelMaxX >> level) - (pixelMinX >> level) >= LEVEL_SPAN || (pixelMaxY >> level) - (pixelMinY >> level) >= LEVEL_SPAN)
            level ++;

        float[] depths     = PYRAMID[level];
        int     levelWidth = LEVEL_WIDTHS[level];
        float   threshold  = nearest * (1.0F + DEPTH_RELATIVE);

        for (int y = pixelMinY >> level; y <= pixelMaxY >> level; y ++) {
            for (int x = pixelMinX >> level; x <= pixelMaxX >> level; x ++) {
                if (depths[y * levelWidth + x] <= threshold)
                    return false;
            }
        }

        return true;
    }

    private static void buildPyramid() {
        System.arraycopy(TESTED_DEPTH, 0, PYRAMID[0], 0, TESTED_DEPTH.length);

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

                    target[y * targetWidth + x] = Math.min(Math.min(source[sourceY0 * width + sourceX0], source[sourceY0 * width + sourceX1]), Math.min(source[sourceY1 * width + sourceX0], source[sourceY1 * width + sourceX1]));
                }
            }

            width  = targetWidth;
            height = targetHeight;
        }
    }

    private static Iterable<ChunkRenderList> iterable(ChunkRenderListIterable lists) {
        return () -> lists.iterator(false);
    }

    private static int[] levelWidths() {
        int[] widths = new int[PYRAMID.length];
        int   width  = WIDTH;

        for (int level = 0; level < widths.length; level ++) {
            widths[level] = width;
            width         = (width + 1) >> 1;
        }

        return widths;
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

        width  = WIDTH;
        height = HEIGHT;

        for (int level = 0; level < levels; level ++) {
            pyramid[level] = new float[width * height];
            width          = (width + 1) >> 1;
            height         = (height + 1) >> 1;
        }

        return pyramid;
    }
}
