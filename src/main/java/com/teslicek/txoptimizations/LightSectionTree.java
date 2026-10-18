package com.teslicek.txoptimizations;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.Arrays;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.chunk.DataLayer;

public final class LightSectionTree {

    public static final int NO_TOP = Integer.MIN_VALUE;

    private static final int  REGION_COLUMNS = 256;
    private static final long REGION_MASK    = ~(15L << 42 | 15L << 20 | 0xFFFFFL);

    private final Long2ObjectOpenHashMap<Region> regions;
    private final int                            minY;
    private final int                            height;
    private final long                           build;

    public LightSectionTree(int minY, int height) {
        this(new Long2ObjectOpenHashMap<>(), minY, height, 0L);
    }

    private LightSectionTree(Long2ObjectOpenHashMap<Region> regions, int minY, int height, long build) {
        this.regions = regions;
        this.minY    = minY;
        this.height  = height;
        this.build   = build;
    }

    public DataLayer layer(long sectionNode) {
        Region region = this.regions.get(sectionNode & REGION_MASK);

        if (region == null)
            return null;

        DataLayer[] layers = region.columns[column(sectionNode)];

        if (layers == null)
            return null;

        int index = SectionPos.y(sectionNode) - this.minY;

        if (index < 0 || index >= layers.length)
            return null;

        return layers[index];
    }

    public int top(long zeroNode) {
        Region region = this.regions.get(zeroNode & REGION_MASK);

        if (region == null)
            return NO_TOP;

        return region.top(column(zeroNode));
    }

    public LightSectionTree with(LongSet changed, LightSnapshotMap source) {
        long                              next     = this.build + 1L;
        Long2ObjectOpenHashMap<Region>    regions  = this.regions.clone();
        Long2ObjectOpenHashMap<DataLayer> sections = source.txoptimizations$sections();
        ObjectArrayList<Region>           touched  = new ObjectArrayList<>();
        LongIterator                      iterator = changed.iterator();

        while (iterator.hasNext()) {
            long sectionNode = iterator.nextLong();
            int  index       = SectionPos.y(sectionNode) - this.minY;

            if (index < 0 || index >= this.height)
                throw new IllegalStateException("Light section " + SectionPos.of(sectionNode) + " is outside the light section range");

            long   key    = sectionNode & REGION_MASK;
            Region region = regions.get(key);

            if (region == null || region.build != next) {
                region = region == null ? new Region(key, next, new DataLayer[REGION_COLUMNS][], null) : region.copy(next);
                regions.put(key, region);
                touched.add(region);
            }

            int column = column(sectionNode);

            if (!region.owns(column))
                region.own(column, source.txoptimizations$top(SectionPos.getZeroNode(sectionNode)));

            region.set(column, index, sections.get(sectionNode), this.height);
        }

        for (int i = 0; i < touched.size(); i++) {
            Region region = touched.get(i);

            region.dropEmptyColumns();

            if (region.isEmpty())
                regions.remove(region.key);
        }

        return new LightSectionTree(regions, this.minY, this.height, next);
    }

    private static int column(long sectionNode) {
        return (int) (sectionNode >>> 16) & 0xF0 | (int) (sectionNode >>> 42) & 15;
    }

    private static final class Region {

        private final long          key;
        private final long          build;
        private final DataLayer[][] columns;
        private final long[]        owned = new long[REGION_COLUMNS / Long.SIZE];
        private int[]               tops;

        private Region(long key, long build, DataLayer[][] columns, int[] tops) {
            this.key     = key;
            this.build   = build;
            this.columns = columns;
            this.tops    = tops;
        }

        private Region copy(long build) {
            return new Region(this.key, build, this.columns.clone(), this.tops == null ? null : this.tops.clone());
        }

        private int top(int column) {
            return this.tops == null ? NO_TOP : this.tops[column];
        }

        private boolean owns(int column) {
            return (this.owned[column >>> 6] & 1L << column) != 0L;
        }

        private void own(int column, int top) {
            DataLayer[] layers = this.columns[column];

            this.owned[column >>> 6] |= 1L << column;

            if (layers != null)
                this.columns[column] = layers.clone();

            if (this.tops == null) {
                if (top == NO_TOP)
                    return;

                this.tops = new int[REGION_COLUMNS];
                Arrays.fill(this.tops, NO_TOP);
            }

            this.tops[column] = top;
        }

        private void set(int column, int index, DataLayer layer, int height) {
            DataLayer[] layers = this.columns[column];

            if (layers == null) {
                if (layer == null)
                    return;

                layers               = new DataLayer[height];
                this.columns[column] = layers;
            }

            layers[index] = layer;
        }

        private void dropEmptyColumns() {
            for (int column = 0; column < REGION_COLUMNS; column++) {
                if (this.owns(column) && isEmpty(this.columns[column]))
                    this.columns[column] = null;
            }
        }

        private boolean isEmpty() {
            for (int column = 0; column < REGION_COLUMNS; column++) {
                if (this.columns[column] != null || this.top(column) != NO_TOP)
                    return false;
            }

            return true;
        }

        private static boolean isEmpty(DataLayer[] layers) {
            if (layers == null)
                return true;

            for (DataLayer layer : layers) {
                if (layer != null)
                    return false;
            }

            return true;
        }
    }
}
