package com.teslicek.txoptimizations.mixin;

import com.mojang.renderpearl.backend.vulkan.VulkanGpuBuffer;
import com.mojang.renderpearl.backend.vulkan.VulkanGpuTexture;
import com.mojang.renderpearl.backend.vulkan.VulkanGpuTextureView;
import com.teslicek.txoptimizations.RenderWorker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({VulkanGpuTexture.class, VulkanGpuTextureView.class, VulkanGpuBuffer.Direct.class})
public abstract class VulkanResourceCloseMainThreadMixin {

    @Inject(method = "close", at = @At("HEAD"), require = 1)
    private void txoptimizations$waitForRenderJob(CallbackInfo ci) {
        RenderWorker.awaitFromMain();
    }
}
