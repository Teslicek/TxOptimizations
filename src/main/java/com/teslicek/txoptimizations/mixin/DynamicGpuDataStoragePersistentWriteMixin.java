package com.teslicek.txoptimizations.mixin;

import com.mojang.renderpearl.api.buffers.GpuBuffer;
import com.mojang.renderpearl.api.buffers.GpuBufferSlice;
import com.mojang.renderpearl.backend.vulkan.VulkanGpuBuffer;
import com.teslicek.txoptimizations.NativeWrites;
import com.teslicek.txoptimizations.PersistentRingBuffer;
import com.teslicek.txoptimizations.SectionCommandStorage;
import com.teslicek.txoptimizations.SectionDrawList;
import com.teslicek.txoptimizations.SectionInfoStorage;
import java.nio.ByteBuffer;
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
public abstract class DynamicGpuDataStoragePersistentWriteMixin<T extends DynamicGpuDataStorage.DynamicGpuData> implements SectionCommandStorage, SectionInfoStorage {

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
    private GpuBufferSlice txoptimizations$lastSlice;

    @Unique
    private int txoptimizations$lastSliceBlock;

    @Shadow
    protected abstract void resizeBuffers(int newCapacity);

    @Overwrite
    public GpuBufferSlice writeData(T gpuData) {
        if (this.lastData != null && this.lastData.equals(gpuData)) {
            GpuBuffer current = this.ringBuffer.currentBuffer();

            if (this.txoptimizations$lastSlice != null && this.txoptimizations$lastSliceBlock == this.nextBlock - 1 && this.txoptimizations$lastSlice.buffer() == current)
                return this.txoptimizations$lastSlice;

            return current.slice((this.nextBlock - 1) * this.blockSize, this.blockSize);
        }

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

        GpuBufferSlice slice = buffer.slice(offset, this.blockSize);

        this.txoptimizations$lastSlice      = slice;
        this.txoptimizations$lastSliceBlock = this.nextBlock;
        this.nextBlock ++;
        this.lastData = gpuData;

        return slice;
    }

    @Override
    @SuppressWarnings("unchecked")
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

        this.txoptimizations$write(buffer, offset, bytes, draws::writeCommands);
        this.nextBlock += totalCount;
        this.lastData   = (T) draws.lastCommand();

        return result;
    }

    @Override
    @SuppressWarnings("unchecked")
    public GpuBufferSlice txoptimizations$writeSectionInfos(SectionDrawList draws) {
        int count = draws.sectionCount();

        if (count == 0)
            return new GpuBufferSlice(null, 0L, 0L);

        int       offset = this.txoptimizations$reserve(count, SectionDrawList.SECTION_BYTES, "section info");
        int       bytes  = count * this.blockSize;
        GpuBuffer buffer = this.ringBuffer.currentBuffer();

        this.txoptimizations$write(buffer, offset, bytes, draws::writeSectionInfos);
        this.nextBlock += count;
        this.lastData   = (T) draws.sectionInfo(count - 1);

        return buffer.slice(offset, bytes);
    }

    @Unique
    private int txoptimizations$reserve(int count, int blockBytes, String kind) {
        if (this.blockSize != blockBytes)
            throw new IllegalStateException(this.label + " holds blocks of " + this.blockSize + " bytes but a " + kind + " is " + blockBytes);

        if (this.nextBlock + count > this.capacity) {
            int newCapacity = Mth.smallestEncompassingPowerOfTwo(Math.max(this.capacity + 1, count));

            LOGGER.info("Resizing {}, capacity limit of {} reached during a single frame. New capacity will be {}.", this.label, this.capacity, newCapacity);
            this.resizeBuffers(newCapacity);
        }

        return this.nextBlock * this.blockSize;
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
