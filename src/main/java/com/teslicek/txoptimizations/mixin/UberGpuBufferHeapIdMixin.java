package com.teslicek.txoptimizations.mixin;

import com.mojang.blaze3d.vertex.UberGpuBuffer;
import com.mojang.renderpearl.api.buffers.GpuBuffer;
import com.mojang.renderpearl.api.device.GpuDevice;
import com.teslicek.txoptimizations.SectionBufferId;
import com.teslicek.txoptimizations.SectionBuffers;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(UberGpuBuffer.UberGpuBufferHeap.class)
public abstract class UberGpuBufferHeapIdMixin implements SectionBufferId {

    @Shadow
    @Final
    private GpuBuffer gpuBuffer;

    @Unique
    private int txoptimizations$bufferId;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void txoptimizations$registerBuffer(long size, GpuDevice gpuDevice, int usage, String name, CallbackInfo ci) {
        this.txoptimizations$bufferId = SectionBuffers.register(this.gpuBuffer);
    }

    @Override
    public int txoptimizations$bufferId() {
        return this.txoptimizations$bufferId;
    }
}
