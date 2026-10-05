package com.teslicek.txoptimizations.mixin;

import com.mojang.renderpearl.api.buffers.GpuBuffer;
import com.mojang.renderpearl.api.buffers.GpuBufferSlice;
import com.mojang.renderpearl.backend.common.BaseGpuBuffer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(BaseGpuBuffer.class)
public abstract class BaseGpuBufferSliceCacheMixin implements GpuBuffer {

    @Unique
    private GpuBufferSlice txoptimizations$wholeSlice;

    @Override
    public GpuBufferSlice slice() {
        GpuBufferSlice slice = this.txoptimizations$wholeSlice;

        if (slice == null) {
            slice                          = new GpuBufferSlice(this, 0L, this.size());
            this.txoptimizations$wholeSlice = slice;
        }

        return slice;
    }
}
