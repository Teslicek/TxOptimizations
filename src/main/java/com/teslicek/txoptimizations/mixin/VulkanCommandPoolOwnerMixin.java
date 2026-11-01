package com.teslicek.txoptimizations.mixin;

import com.mojang.renderpearl.backend.vulkan.VulkanCommandEncoder;
import com.teslicek.txoptimizations.RenderWorker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(VulkanCommandEncoder.class)
public abstract class VulkanCommandPoolOwnerMixin {

    @Inject(method = "allocateAndBeginTransientCommandBuffer", at = @At("HEAD"), require = 1)
    private void txoptimizations$requirePoolOwner(CallbackInfoReturnable<?> cir) {
        if (RenderWorker.isBusy() && !RenderWorker.isCurrent())
            throw new IllegalStateException("Thread " + Thread.currentThread().getName() + " allocated a command buffer while the render job owns the command pool");
    }
}
