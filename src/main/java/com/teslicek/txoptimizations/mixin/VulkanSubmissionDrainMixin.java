package com.teslicek.txoptimizations.mixin;

import com.mojang.renderpearl.backend.vulkan.VulkanQueue;
import com.teslicek.txoptimizations.PresentThread;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(VulkanQueue.Submission.class)
public abstract class VulkanSubmissionDrainMixin {

    @Inject(method = "close", at = @At("HEAD"))
    private void txoptimizations$drainPresentThread(CallbackInfo ci) {
        PresentThread.drain();
    }
}
