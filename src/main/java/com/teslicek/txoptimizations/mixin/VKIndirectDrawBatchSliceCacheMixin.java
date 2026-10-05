package com.teslicek.txoptimizations.mixin;

import com.mojang.renderpearl.api.buffers.GpuBuffer;
import com.mojang.renderpearl.api.buffers.GpuBufferSlice;
import net.caffeinemc.mods.sodium.client.gpu.device.batch.VKIndirectDrawBatch;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = VKIndirectDrawBatch.class, remap = false)
public abstract class VKIndirectDrawBatchSliceCacheMixin {

    @Unique
    private static final int CACHED_SLICES = 8;

    @Unique
    private GpuBuffer[] txoptimizations$buffers;

    @Unique
    private long[] txoptimizations$offsets;

    @Unique
    private long[] txoptimizations$lengths;

    @Unique
    private GpuBufferSlice[] txoptimizations$slices;

    @Unique
    private int txoptimizations$nextSlice;

    @Redirect(method = "draw", at = @At(value = "INVOKE", target = "Lcom/mojang/renderpearl/api/buffers/GpuBufferSlice;slice(JJ)Lcom/mojang/renderpearl/api/buffers/GpuBufferSlice;"))
    private GpuBufferSlice txoptimizations$reuseCommandSlice(GpuBufferSlice parent, long offset, long length) {
        if (offset < 0L || length < 0L || offset + length > parent.length())
            return parent.slice(offset, length);

        if (this.txoptimizations$slices == null) {
            this.txoptimizations$buffers = new GpuBuffer[CACHED_SLICES];
            this.txoptimizations$offsets = new long[CACHED_SLICES];
            this.txoptimizations$lengths = new long[CACHED_SLICES];
            this.txoptimizations$slices  = new GpuBufferSlice[CACHED_SLICES];
        }

        GpuBuffer buffer   = parent.buffer();
        long      absolute = parent.offset() + offset;

        for (int index = 0; index < CACHED_SLICES; index ++) {
            if (this.txoptimizations$buffers[index] == buffer && this.txoptimizations$offsets[index] == absolute && this.txoptimizations$lengths[index] == length)
                return this.txoptimizations$slices[index];
        }

        GpuBufferSlice slice = parent.slice(offset, length);
        int            next  = this.txoptimizations$nextSlice;

        this.txoptimizations$buffers[next] = buffer;
        this.txoptimizations$offsets[next] = absolute;
        this.txoptimizations$lengths[next] = length;
        this.txoptimizations$slices[next]  = slice;
        this.txoptimizations$nextSlice     = (next + 1) % CACHED_SLICES;

        return slice;
    }
}
