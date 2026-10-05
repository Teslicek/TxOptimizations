package com.teslicek.txoptimizations.mixin;

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
    private GpuBufferSlice[] txoptimizations$slices;

    @Unique
    private int txoptimizations$nextSlice;

    @Redirect(method = "draw", at = @At(value = "INVOKE", target = "Lcom/mojang/renderpearl/api/buffers/GpuBufferSlice;slice(JJ)Lcom/mojang/renderpearl/api/buffers/GpuBufferSlice;"))
    private GpuBufferSlice txoptimizations$reuseCommandSlice(GpuBufferSlice parent, long offset, long length) {
        if (offset < 0L || length < 0L || offset + length > parent.length())
            return parent.slice(offset, length);

        if (this.txoptimizations$slices == null)
            this.txoptimizations$slices = new GpuBufferSlice[CACHED_SLICES];

        long absolute = parent.offset() + offset;

        for (GpuBufferSlice slice : this.txoptimizations$slices) {
            if (slice != null && slice.buffer() == parent.buffer() && slice.offset() == absolute && slice.length() == length)
                return slice;
        }

        GpuBufferSlice slice = parent.slice(offset, length);

        this.txoptimizations$slices[this.txoptimizations$nextSlice] = slice;
        this.txoptimizations$nextSlice                              = (this.txoptimizations$nextSlice + 1) % CACHED_SLICES;

        return slice;
    }
}
