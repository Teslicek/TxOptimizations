package com.teslicek.txoptimizations.mixin;

import com.mojang.renderpearl.api.buffers.GpuBuffer;
import com.mojang.renderpearl.api.buffers.GpuBufferSlice;
import com.mojang.renderpearl.backend.vulkan.VulkanGpuBuffer;
import com.teslicek.txoptimizations.LastWrittenData;
import com.teslicek.txoptimizations.NativeWrites;
import com.teslicek.txoptimizations.PersistentRingBuffer;
import com.teslicek.txoptimizations.SectionCommandStorage;
import com.teslicek.txoptimizations.SectionDrawList;
import com.teslicek.txoptimizations.SectionInfoStorage;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.function.LongConsumer;
import net.minecraft.client.renderer.DynamicGpuDataStorage;
import net.minecraft.client.renderer.DynamicGpuDataStorageMapped;
import net.minecraft.client.renderer.MappableRingBuffer;
import net.minecraft.util.Mth;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(value = DynamicGpuDataStorageMapped.class, priority = 999)
public abstract class DynamicGpuDataStoragePersistentWriteMixin<T extends DynamicGpuDataStorage.DynamicGpuData> implements SectionCommandStorage, SectionInfoStorage, LastWrittenData {

    @Unique
    private static final int HELD_BUFFERS = 8;

    @Unique
    private static final int SLICE_POOLS = 3;

    @Shadow
    @Final
    private static Logger LOGGER;

    @Shadow
    @Final
    private int blockSize;

    @Shadow
    private MappableRingBuffer ringBuffer;

    @Shadow
    private int nextBlock;

    @Shadow
    private int capacity;

    @Shadow
    private @Nullable T lastData;

    @Shadow
    @Final
    private String label;

    @Unique
    private final GpuBuffer[] txoptimizations$sliceBuffers = new GpuBuffer[SLICE_POOLS];

    @Unique
    private final GpuBufferSlice[][] txoptimizations$slices = new GpuBufferSlice[SLICE_POOLS][];

    @Unique
    private int txoptimizations$nextSlicePool;

    @Unique
    private final GpuBuffer[] txoptimizations$heldBuffers = new GpuBuffer[HELD_BUFFERS];

    @Unique
    private final long[] txoptimizations$heldBuilds = new long[HELD_BUFFERS];

    @Unique
    private final int[] txoptimizations$heldOffsets = new int[HELD_BUFFERS];

    @Unique
    private final int[] txoptimizations$heldBytes = new int[HELD_BUFFERS];

    @Shadow
    protected abstract void resizeBuffers(int newCapacity);

    @Override
    public DynamicGpuDataStorage.@Nullable DynamicGpuData txoptimizations$lastData() {
        return this.lastData;
    }

    @Overwrite
    public GpuBufferSlice writeData(T gpuData) {
        this.txoptimizations$forgetHeldDraws();

        if (this.lastData != null && this.lastData.equals(gpuData))
            return this.txoptimizations$blockSlice(this.ringBuffer.currentBuffer(), this.nextBlock - 1);

        if (this.nextBlock >= this.capacity) {
            int newCapacity = this.capacity * 2;

            LOGGER.info("Resizing {}, capacity limit of {} reached during a single frame. New capacity will be {}.", this.label, this.capacity, newCapacity);
            this.resizeBuffers(newCapacity);
        }

        int       offset = this.nextBlock * this.blockSize;
        GpuBuffer buffer = this.ringBuffer.currentBuffer();

        if (buffer instanceof VulkanGpuBuffer.Direct) {
            ByteBuffer data = ((PersistentRingBuffer) this.ringBuffer).txoptimizations$mapCurrent();

            data.limit(offset + this.blockSize).position(offset);
            gpuData.write(data);
        } else {
            try (GpuBufferSlice.MappedView view = buffer.slice(offset, this.blockSize).map(false, true)) {
                gpuData.write(view.data());
            }
        }

        GpuBufferSlice slice = this.txoptimizations$blockSlice(buffer, this.nextBlock);

        this.nextBlock ++;
        this.lastData = gpuData;

        return slice;
    }

    @Override
    public GpuBufferSlice[] txoptimizations$writeSectionCommands(SectionDrawList draws) {
        int lists = draws.groupCount();

        if (lists == 0)
            return new GpuBufferSlice[0];

        int              totalCount = draws.commandCount();
        int              offset     = this.txoptimizations$reserve(totalCount, SectionDrawList.COMMAND_BYTES, "section draw command");
        int              bytes      = totalCount * this.blockSize;
        GpuBuffer        buffer     = this.ringBuffer.currentBuffer();
        GpuBufferSlice[] result     = new GpuBufferSlice[lists];

        for (int list = 0, listOffset = offset; list < lists; list ++) {
            int listBytes = draws.drawCount(draws.outputGroup(list)) * this.blockSize;

            result[list]  = buffer.slice(listOffset, listBytes);
            listOffset   += listBytes;
        }

        this.txoptimizations$writeUnlessHeld(buffer, offset, bytes, draws.build(), draws::writeCommands);
        this.nextBlock += totalCount;

        return result;
    }

    @Override
    public GpuBufferSlice txoptimizations$writeSectionInfos(SectionDrawList draws) {
        int count = draws.sectionCount();

        if (count == 0)
            return new GpuBufferSlice(null, 0L, 0L);

        int       offset = this.txoptimizations$reserve(count, SectionDrawList.SECTION_BYTES, "section info");
        int       bytes  = count * this.blockSize;
        GpuBuffer buffer = this.ringBuffer.currentBuffer();

        this.txoptimizations$writeUnlessHeld(buffer, offset, bytes, draws.build(), draws::writeSectionInfos);
        this.nextBlock += count;

        return buffer.slice(offset, bytes);
    }

    @Unique
    private int txoptimizations$reserve(int count, int blockBytes, String kind) {
        if (this.blockSize != blockBytes)
            throw new IllegalStateException(this.label + " holds blocks of " + this.blockSize + " bytes but a " + kind + " is " + blockBytes);

        if (this.nextBlock + count > this.capacity) {
            int newCapacity = Mth.smallestEncompassingPowerOfTwo(Math.max(this.capacity + 1, count));

            LOGGER.info("Resizing {}, capacity limit of {} reached during a single frame. New capacity will be {}.", this.label, this.capacity, newCapacity);
            this.txoptimizations$forgetHeldDraws();
            this.resizeBuffers(newCapacity);
        }

        return this.nextBlock * this.blockSize;
    }

    @Unique
    private void txoptimizations$writeUnlessHeld(GpuBuffer buffer, int offset, int bytes, long build, LongConsumer writer) {
        GpuBuffer[] buffers = this.txoptimizations$heldBuffers;
        int         free    = -1;

        for (int slot = 0; slot < buffers.length; slot ++) {
            if (buffers[slot] == buffer) {
                if (this.txoptimizations$heldBuilds[slot] == build && this.txoptimizations$heldOffsets[slot] == offset && this.txoptimizations$heldBytes[slot] == bytes)
                    return;

                free = slot;
                break;
            }

            if (buffers[slot] == null && free == -1)
                free = slot;
        }

        this.txoptimizations$write(buffer, offset, bytes, writer);

        if (free == -1) {
            this.txoptimizations$forgetHeldDraws();
            free = 0;
        }

        buffers[free]                          = buffer;
        this.txoptimizations$heldBuilds[free]  = build;
        this.txoptimizations$heldOffsets[free] = offset;
        this.txoptimizations$heldBytes[free]   = bytes;
    }

    @Unique
    private GpuBufferSlice txoptimizations$blockSlice(GpuBuffer buffer, int block) {
        GpuBufferSlice[] slices = this.txoptimizations$slicesFor(buffer);

        if (block >= slices.length) {
            slices = Arrays.copyOf(slices, Math.max(block + 1, this.capacity));

            this.txoptimizations$slices[this.txoptimizations$poolOf(buffer)] = slices;
        }

        GpuBufferSlice slice = slices[block];

        if (slice == null) {
            slice         = buffer.slice((long) block * this.blockSize, this.blockSize);
            slices[block] = slice;
        }

        return slice;
    }

    @Unique
    private GpuBufferSlice[] txoptimizations$slicesFor(GpuBuffer buffer) {
        int pool = this.txoptimizations$poolOf(buffer);

        if (pool != -1)
            return this.txoptimizations$slices[pool];

        pool = this.txoptimizations$nextSlicePool;

        GpuBufferSlice[] slices = new GpuBufferSlice[this.capacity];

        this.txoptimizations$sliceBuffers[pool] = buffer;
        this.txoptimizations$slices[pool]       = slices;
        this.txoptimizations$nextSlicePool      = (pool + 1) % SLICE_POOLS;

        return slices;
    }

    @Unique
    private int txoptimizations$poolOf(GpuBuffer buffer) {
        for (int pool = 0; pool < SLICE_POOLS; pool ++) {
            if (this.txoptimizations$sliceBuffers[pool] == buffer)
                return pool;
        }

        return -1;
    }

    @Unique
    private void txoptimizations$forgetHeldDraws() {
        if (this.txoptimizations$heldBuffers[0] != null)
            Arrays.fill(this.txoptimizations$heldBuffers, null);
    }

    @Unique
    private void txoptimizations$write(GpuBuffer buffer, int offset, int bytes, LongConsumer writer) {
        if (buffer instanceof VulkanGpuBuffer.Direct) {
            writer.accept(NativeWrites.at(((PersistentRingBuffer) this.ringBuffer).txoptimizations$mapCurrent(), offset, bytes));

            return;
        }

        try (GpuBufferSlice.MappedView view = buffer.slice(offset, bytes).map(false, true)) {
            writer.accept(NativeWrites.at(view.data(), 0, bytes));
        }
    }
}
