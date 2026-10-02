package com.teslicek.txoptimizations.mixin.cull;

import com.mojang.renderpearl.backend.vulkan.VulkanCommandEncoder;
import com.teslicek.txoptimizations.cull.GpuWaitMeter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(VulkanCommandEncoder.class)
public abstract class VulkanCommandEncoderWaitMixin {

    @Inject(method = "awaitSubmitCompletion", at = @At("HEAD"))
    private void txoptimizations$beginGpuWait(long submitIndex, long timeout, CallbackInfoReturnable<Boolean> cir) {
        if (timeout != 0L)
            GpuWaitMeter.beginWait();
    }

    @Inject(method = "awaitSubmitCompletion", at = @At("RETURN"))
    private void txoptimizations$endGpuWait(long submitIndex, long timeout, CallbackInfoReturnable<Boolean> cir) {
        if (timeout != 0L)
            GpuWaitMeter.endWait();
    }
}
