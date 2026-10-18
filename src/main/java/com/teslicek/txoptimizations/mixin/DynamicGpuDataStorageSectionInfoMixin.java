package com.teslicek.txoptimizations.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.renderpearl.api.buffers.GpuBuffer;
import com.mojang.renderpearl.api.buffers.GpuBufferSlice;
import com.teslicek.txoptimizations.NativeWrites;
import com.teslicek.txoptimizations.SectionDrawList;
import com.teslicek.txoptimizations.SectionInfoStorage;
import java.nio.ByteBuffer;
import net.minecraft.client.renderer.DynamicGpuDataStorageNonMapped;
import net.minecraft.util.Mth;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(DynamicGpuDataStorageNonMapped.class)
public abstract class DynamicGpuDataStorageSectionInfoMixin implements SectionInfoStorage {

    @Shadow
    @Final
    private static Logger LOGGER;

    @Shadow
    @Final
    private int blockSize;

    @Shadow
    private GpuBuffer gpuBuffer;

    @Shadow
    private ByteBuffer cpuBuffer;

    @Shadow
    private int nextBlock;

    @Shadow
    private int capacity;

    @Shadow
    @Final
    private String label;

    @Shadow
    protected abstract void resizeBuffers(int newCapacity);

    @Override
    public GpuBufferSlice txoptimizations$writeSectionInfos(SectionDrawList draws) {
        int count = draws.sectionCount();

        if (count == 0)
            return new GpuBufferSlice(null, 0L, 0L);

        if (this.blockSize != SectionDrawList.SECTION_BYTES)
            throw new IllegalStateException(this.label + " holds blocks of " + this.blockSize + " bytes but a section info is " + SectionDrawList.SECTION_BYTES);

        if (this.nextBlock + count > this.capacity) {
            int newCapacity = Mth.smallestEncompassingPowerOfTwo(Math.max(this.capacity + 1, count));

            LOGGER.info("Resizing {}, capacity limit of {} reached during a single frame. New capacity will be {}.", this.label, this.capacity, newCapacity);
            this.resizeBuffers(newCapacity);
        }

        int firstOffset = this.nextBlock * this.blockSize;
        int bytes       = count * this.blockSize;

        draws.writeSectionInfos(NativeWrites.at(this.cpuBuffer, firstOffset, bytes));
        this.cpuBuffer.position(firstOffset + bytes);

        ByteBuffer     cpuSlice = this.cpuBuffer.slice(firstOffset, bytes);
        GpuBufferSlice result   = this.gpuBuffer.slice(firstOffset, bytes);

        RenderSystem.getDevice().createCommandEncoder().writeToBuffer(result, cpuSlice);
        this.nextBlock += count;

        return result;
    }
}
