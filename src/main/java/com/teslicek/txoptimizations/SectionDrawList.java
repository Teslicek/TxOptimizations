package com.teslicek.txoptimizations;

import com.mojang.renderpearl.api.buffers.GpuBuffer;
import com.mojang.renderpearl.api.pipeline.IndexType;
import it.unimi.dsi.fastutil.HashCommon;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.minecraft.client.renderer.DynamicGpuData;
import org.jspecify.annotations.Nullable;
import org.lwjgl.system.MemoryUtil;

public final class SectionDrawList {

    public static final int COMMAND_BYTES = 5 * Integer.BYTES;
    public static final int SECTION_BYTES = 3 * Integer.BYTES + Float.BYTES;

    private static final int GROUP_HASH_SEED = 173;
    private static final int NONE            = -1;
    private static final int INSTANCE_COUNT  = 1;
    private static final int DRAW_INTS       = 5;
    private static final int GROUP           = 0;
    private static final int INDEX_COUNT     = 1;
    private static final int FIRST_INDEX     = 2;
    private static final int BASE_VERTEX     = 3;
    private static final int BASE_INSTANCE   = 4;
    private static final int SECTION_INTS    = 4;

    private static final int COMMAND_INDEX_COUNT    = 0;
    private static final int COMMAND_INSTANCE_COUNT = 1;
    private static final int COMMAND_FIRST_INDEX    = 2;
    private static final int COMMAND_BASE_VERTEX    = 3;
    private static final int COMMAND_BASE_INSTANCE  = 4;
    private static final int INITIAL_GROUPS  = 16;
    private static final int INITIAL_DRAWS   = 4096;

    private static final IndexType[] INDEX_TYPES       = IndexType.values();
    private static final int[]       INDEX_TYPE_HASHES = indexTypeHashes();

    private GpuBuffer[]           vertexBuffers = new GpuBuffer[INITIAL_GROUPS];
    private @Nullable GpuBuffer[] indexBuffers  = new GpuBuffer[INITIAL_GROUPS];
    private @Nullable IndexType[] indexTypes    = new IndexType[INITIAL_GROUPS];
    private int[]                 groupLayers   = new int[INITIAL_GROUPS];
    private int[]                 groupStarts   = new int[INITIAL_GROUPS];
    private int[]                 groupCursors  = new int[INITIAL_GROUPS];
    private int[]                 drawCounts    = new int[INITIAL_GROUPS];
    private int[]                 order         = new int[INITIAL_GROUPS];
    private int[]                 cacheKeys     = new int[INITIAL_GROUPS * 2];
    private int[]                 cacheValues   = new int[INITIAL_GROUPS * 2];
    private int[]                 draws         = new int[INITIAL_DRAWS * DRAW_INTS];
    private int[]                 sections      = new int[INITIAL_DRAWS * SECTION_INTS];
    private int[][]               layerGroups   = new int[0][];
    private int[]                 layerCounts   = new int[0];
    private int[]                 layerEnds     = new int[0];
    private boolean[]             reversed      = new boolean[0];
    private int[]                 memoHashes    = new int[0];
    private int[]                 memoVertices  = new int[0];
    private int[]                 memoIndices   = new int[0];
    private int[]                 memoTypes     = new int[0];
    private int[]                 memoGroups    = new int[0];
    private int                   groups;
    private int                   cacheSize;
    private int                   drawInts;
    private int                   sectionInts;
    private int                   largestIndexCount;
    private int                   lastTranslucentHash;
    private boolean               respectTranslucentOrder;

    public void begin(int layers, boolean respectTranslucentOrder) {
        if (this.layerGroups.length != layers) {
            this.layerGroups  = new int[layers][INITIAL_GROUPS];
            this.layerCounts  = new int[layers];
            this.layerEnds    = new int[layers];
            this.reversed     = new boolean[layers];
            this.memoHashes   = new int[layers];
            this.memoVertices = new int[layers];
            this.memoIndices  = new int[layers];
            this.memoTypes    = new int[layers];
            this.memoGroups   = new int[layers];
        }

        Arrays.fill(this.layerCounts, 0);
        Arrays.fill(this.memoGroups, NONE);
        Arrays.fill(this.reversed, false);
        Arrays.fill(this.cacheValues, 0);

        this.groups                  = 0;
        this.cacheSize               = 0;
        this.drawInts                = 0;
        this.sectionInts             = 0;
        this.largestIndexCount       = 0;
        this.lastTranslucentHash     = 0;
        this.respectTranslucentOrder = respectTranslucentOrder;
    }

    public int addSection(int x, int y, int z, float visibility) {
        int start = this.sectionInts;

        if (start + SECTION_INTS > this.sections.length)
            this.sections = Arrays.copyOf(this.sections, this.sections.length * 2);

        this.sections[start]     = x;
        this.sections[start + 1] = y;
        this.sections[start + 2] = z;
        this.sections[start + 3] = Float.floatToRawIntBits(visibility);
        this.sectionInts         = start + SECTION_INTS;

        return start / SECTION_INTS;
    }

    public void shareIndexBuffer(int indexCount) {
        if (indexCount > this.largestIndexCount)
            this.largestIndexCount = indexCount;
    }

    public int group(int layer, boolean translucent, int vertexBuffer, int indexBuffer, int indexType) {
        boolean ordered = translucent && this.respectTranslucentOrder;

        if (!ordered && this.memoGroups[layer] != NONE && this.memoVertices[layer] == vertexBuffer && this.memoIndices[layer] == indexBuffer && this.memoTypes[layer] == indexType)
            return this.memoGroups[layer];

        int hash = 31 * GROUP_HASH_SEED + SectionBuffers.hash(vertexBuffer);

        if (indexBuffer != DrawRecords.NO_BUFFER) {
            hash = 31 * hash + SectionBuffers.hash(indexBuffer);
            hash = 31 * hash + INDEX_TYPE_HASHES[indexType];
        }

        if (ordered) {
            boolean sameAsLast = hash == this.lastTranslucentHash;

            this.lastTranslucentHash = hash;

            if (sameAsLast)
                return this.lastGroup(layer);

            return this.addGroup(layer, true, hash, vertexBuffer, indexBuffer, indexType);
        }

        int cached = this.cachedGroup(hash);
        int group  = cached != NONE ? cached : this.addGroup(layer, translucent, hash, vertexBuffer, indexBuffer, indexType);

        this.memoHashes[layer]   = hash;
        this.memoVertices[layer] = vertexBuffer;
        this.memoIndices[layer]  = indexBuffer;
        this.memoTypes[layer]    = indexType;
        this.memoGroups[layer]   = group;

        return group;
    }

    public void add(int group, int indexCount, int firstIndex, int baseVertex, int baseInstance) {
        int draw = this.drawInts;

        if (draw + DRAW_INTS > this.draws.length)
            this.draws = Arrays.copyOf(this.draws, this.draws.length * 2);

        this.draws[draw + GROUP]         = group;
        this.draws[draw + INDEX_COUNT]   = indexCount;
        this.draws[draw + FIRST_INDEX]   = firstIndex;
        this.draws[draw + BASE_VERTEX]   = baseVertex;
        this.draws[draw + BASE_INSTANCE] = baseInstance;
        this.drawInts                    = draw + DRAW_INTS;
        this.drawCounts[group] ++;
    }

    public SectionDrawList finish() {
        int index = 0;

        for (int layer = 0; layer < this.layerGroups.length; layer ++) {
            int[]   layerGroups = this.layerGroups[layer];
            int     count       = this.layerCounts[layer];
            boolean reversed    = this.reversed[layer];

            for (int position = 0; position < count; position ++)
                this.order[index ++] = layerGroups[reversed ? count - 1 - position : position];

            this.layerEnds[layer] = index;
        }

        if (index != this.groups)
            throw new IllegalStateException(this.groups + " section draw groups were made but the layers list " + index);

        this.placeCommands();

        return this;
    }

    public int largestIndexCount() {
        return this.largestIndexCount;
    }

    public int sectionCount() {
        return this.sectionInts / SECTION_INTS;
    }

    public int commandCount() {
        return this.drawInts / DRAW_INTS;
    }

    public int groupCount() {
        return this.groups;
    }

    public int outputGroup(int index) {
        return this.order[index];
    }

    public int layerEnd(int layer) {
        return this.layerEnds[layer];
    }

    public int layerGroupCount(int layer) {
        return this.layerCounts[layer];
    }

    public int layerGroup(int layer, int index) {
        return this.layerGroups[layer][index];
    }

    public GpuBuffer vertexBuffer(int group) {
        return this.vertexBuffers[group];
    }

    public @Nullable GpuBuffer indexBuffer(int group) {
        return this.indexBuffers[group];
    }

    public @Nullable IndexType indexType(int group) {
        return this.indexTypes[group];
    }

    public int drawCount(int group) {
        return this.drawCounts[group];
    }

    public void writeCommands(long address) {
        MemoryUtil.memIntBuffer(address, this.drawInts).put(this.draws, 0, this.drawInts);
    }

    public void countDraws(long[] layerDraws, long[] layerTriangles) {
        for (int group = 0; group < this.groups; group ++) {
            int layer = this.groupLayers[group];
            int start = this.groupStarts[group];
            int end   = start + this.drawCounts[group];

            layerDraws[layer] += this.drawCounts[group];

            for (int slot = start; slot < end; slot ++)
                layerTriangles[layer] += this.draws[slot * DRAW_INTS + COMMAND_INDEX_COUNT] / 3;
        }
    }

    public void writeSectionInfos(long address) {
        MemoryUtil.memIntBuffer(address, this.sectionInts).put(this.sections, 0, this.sectionInts);
    }

    public DynamicGpuData.ChunkSectionInfo sectionInfo(int index) {
        int start = index * SECTION_INTS;

        return new DynamicGpuData.ChunkSectionInfo(this.sections[start], this.sections[start + 1], this.sections[start + 2], Float.intBitsToFloat(this.sections[start + 3]));
    }

    public List<DynamicGpuData.IndexedDraw> indexedDraws(int group) {
        int                              count    = this.drawCounts[group];
        int                              start    = this.groupStarts[group];
        boolean                          reversed = this.reversed[this.groupLayers[group]];
        List<DynamicGpuData.IndexedDraw> draws    = new ArrayList<>(count);

        for (int position = 0; position < count; position ++) {
            int command = (reversed ? start + count - 1 - position : start + position) * DRAW_INTS;

            draws.add(new DynamicGpuData.IndexedDraw(this.draws[command + COMMAND_INDEX_COUNT], INSTANCE_COUNT, this.draws[command + COMMAND_FIRST_INDEX], this.draws[command + COMMAND_BASE_VERTEX], this.draws[command + COMMAND_BASE_INSTANCE]));
        }

        return draws;
    }

    private void placeCommands() {
        int slot = 0;

        for (int index = 0; index < this.groups; index ++) {
            int group = this.order[index];
            int count = this.drawCounts[group];

            this.groupStarts[group]  = slot;
            this.groupCursors[group] = this.reversed[this.groupLayers[group]] ? slot + count : slot;
            slot                    += count;
        }

        int[] draws   = this.draws;
        int   records = this.drawInts / DRAW_INTS;

        for (int record = 0; record < records; record ++) {
            int base  = record * DRAW_INTS;
            int group = draws[base + GROUP];

            draws[base + GROUP] = ~(this.reversed[this.groupLayers[group]] ? -- this.groupCursors[group] : this.groupCursors[group] ++);
        }

        for (int record = 0; record < records; record ++) {
            int base = record * DRAW_INTS;

            if (draws[base + GROUP] >= 0)
                continue;

            int destination  = ~draws[base + GROUP];
            int indexCount   = draws[base + INDEX_COUNT];
            int firstIndex   = draws[base + FIRST_INDEX];
            int baseVertex   = draws[base + BASE_VERTEX];
            int baseInstance = draws[base + BASE_INSTANCE];

            while (true) {
                int target = destination * DRAW_INTS;

                if (destination == record) {
                    putCommand(draws, target, indexCount, firstIndex, baseVertex, baseInstance);
                    break;
                }

                int nextDestination  = ~draws[target + GROUP];
                int nextIndexCount   = draws[target + INDEX_COUNT];
                int nextFirstIndex   = draws[target + FIRST_INDEX];
                int nextBaseVertex   = draws[target + BASE_VERTEX];
                int nextBaseInstance = draws[target + BASE_INSTANCE];

                putCommand(draws, target, indexCount, firstIndex, baseVertex, baseInstance);

                destination  = nextDestination;
                indexCount   = nextIndexCount;
                firstIndex   = nextFirstIndex;
                baseVertex   = nextBaseVertex;
                baseInstance = nextBaseInstance;
            }
        }
    }

    private int lastGroup(int layer) {
        int count = this.layerCounts[layer];

        if (count == 0)
            throw new IllegalStateException("Translucent section geometry in layer " + layer + " continues a draw group before any was made");

        return this.layerGroups[layer][count - 1];
    }

    private int addGroup(int layer, boolean translucent, int hash, int vertexBuffer, int indexBuffer, int indexType) {
        int     group       = this.groups;
        boolean customIndex = indexBuffer != DrawRecords.NO_BUFFER;

        if (group == this.vertexBuffers.length)
            this.growGroups(group * 2);

        this.vertexBuffers[group] = SectionBuffers.buffer(vertexBuffer);
        this.indexBuffers[group]  = customIndex ? SectionBuffers.buffer(indexBuffer) : null;
        this.indexTypes[group]    = customIndex ? INDEX_TYPES[indexType] : null;
        this.groupLayers[group]   = layer;
        this.drawCounts[group]    = 0;
        this.groups               = group + 1;
        this.reversed[layer]      = translucent && this.respectTranslucentOrder;

        int count = this.layerCounts[layer];

        if (count == this.layerGroups[layer].length)
            this.layerGroups[layer] = Arrays.copyOf(this.layerGroups[layer], count * 2);

        this.layerGroups[layer][count] = group;
        this.layerCounts[layer]        = count + 1;

        this.cacheGroup(hash, group);

        return group;
    }

    private void growGroups(int capacity) {
        this.vertexBuffers = Arrays.copyOf(this.vertexBuffers, capacity);
        this.indexBuffers  = Arrays.copyOf(this.indexBuffers, capacity);
        this.indexTypes    = Arrays.copyOf(this.indexTypes, capacity);
        this.groupLayers   = Arrays.copyOf(this.groupLayers, capacity);
        this.groupStarts   = Arrays.copyOf(this.groupStarts, capacity);
        this.groupCursors  = Arrays.copyOf(this.groupCursors, capacity);
        this.drawCounts    = Arrays.copyOf(this.drawCounts, capacity);
        this.order         = Arrays.copyOf(this.order, capacity);
    }

    private int cachedGroup(int hash) {
        int mask = this.cacheValues.length - 1;

        for (int slot = HashCommon.mix(hash) & mask; this.cacheValues[slot] != 0; slot = slot + 1 & mask) {
            if (this.cacheKeys[slot] == hash)
                return this.cacheValues[slot] - 1;
        }

        return NONE;
    }

    private void cacheGroup(int hash, int group) {
        int mask = this.cacheValues.length - 1;
        int slot = HashCommon.mix(hash) & mask;

        while (this.cacheValues[slot] != 0) {
            if (this.cacheKeys[slot] == hash) {
                this.cacheValues[slot] = group + 1;
                this.forgetMemos(hash);
                return;
            }

            slot = slot + 1 & mask;
        }

        this.cacheKeys[slot]   = hash;
        this.cacheValues[slot] = group + 1;
        this.cacheSize ++;

        if (this.cacheSize * 2 > this.cacheValues.length)
            this.rehash(this.cacheValues.length * 2);
    }

    private void forgetMemos(int hash) {
        for (int layer = 0; layer < this.memoGroups.length; layer ++) {
            if (this.memoHashes[layer] == hash)
                this.memoGroups[layer] = NONE;
        }
    }

    private void rehash(int capacity) {
        int[] keys   = this.cacheKeys;
        int[] values = this.cacheValues;

        this.cacheKeys   = new int[capacity];
        this.cacheValues = new int[capacity];
        this.cacheSize   = 0;

        for (int slot = 0; slot < values.length; slot ++) {
            if (values[slot] != 0)
                this.cacheGroup(keys[slot], values[slot] - 1);
        }
    }

    private static void putCommand(int[] draws, int command, int indexCount, int firstIndex, int baseVertex, int baseInstance) {
        draws[command + COMMAND_INDEX_COUNT]    = indexCount;
        draws[command + COMMAND_INSTANCE_COUNT] = INSTANCE_COUNT;
        draws[command + COMMAND_FIRST_INDEX]    = firstIndex;
        draws[command + COMMAND_BASE_VERTEX]    = baseVertex;
        draws[command + COMMAND_BASE_INSTANCE]  = baseInstance;
    }

    private static int[] indexTypeHashes() {
        int[] hashes = new int[INDEX_TYPES.length];

        for (IndexType type : INDEX_TYPES)
            hashes[type.ordinal()] = type.hashCode();

        return hashes;
    }
}
