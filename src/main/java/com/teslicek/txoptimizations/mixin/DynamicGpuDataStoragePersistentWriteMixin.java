package com.teslicek.txoptimizations.mixin;

import com.mojang.renderpearl.api.buffers.GpuBuffer;
import com.mojang.renderpearl.api.buffers.GpuBufferSlice;
import com.mojang.renderpearl.backend.vulkan.VulkanGpuBuffer;
import com.teslicek.txoptimizations.PersistentRingBuffer;
import java.nio.ByteBuffer;
import net.minecraft.client.renderer.DynamicGpuDataStorage;
import net.minecraft.client.renderer.DynamicGpuDataStorageMapped;
import net.minecraft.client.renderer.MappableRingBuffer;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(DynamicGpuDataStorageMapped.class)
public abstract class DynamicGpuDataStoragePersistentWriteMixin<T extends DynamicGpuDataStorage.DynamicGpuData> {

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
}
