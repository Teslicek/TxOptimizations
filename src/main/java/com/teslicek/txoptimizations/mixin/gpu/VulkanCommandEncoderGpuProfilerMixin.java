package com.teslicek.txoptimizations.mixin.gpu;

import com.mojang.renderpearl.api.commands.RenderPassDescriptor;
import com.mojang.renderpearl.backend.api.CommandEncoderBackend;
import com.mojang.renderpearl.backend.vulkan.VulkanCommandEncoder;
import com.mojang.renderpearl.backend.vulkan.VulkanDevice;
import com.mojang.renderpearl.backend.vulkan.VulkanRenderPass;
import com.teslicek.txoptimizations.gpu.GpuPassProfiler;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(VulkanCommandEncoder.class)
public abstract class VulkanCommandEncoderGpuProfilerMixin {

    @Shadow
    @Final
    private VulkanDevice device;

    @Shadow
    private VulkanRenderPass currentRenderPass;

    @Inject(method = "createRenderPass", at = @At("HEAD"))
    private void txoptimizations$profileRenderPass(RenderPassDescriptor descriptor, CallbackInfoReturnable<?> cir) {
        if (GpuPassProfiler.isRunning())
            this.txoptimizations$mark(descriptor.label().get());
    }

    @Inject(method = {"clearColorTexture", "clearDepthTexture", "clearColorAndDepthTextures"}, at = @At("HEAD"))
    private void txoptimizations$profileClear(CallbackInfo ci) {
        if (GpuPassProfiler.isRunning())
            this.txoptimizations$mark("clear");
    }

    @Inject(method = {"writeToBuffer", "copyToBuffer", "writeToTexture", "copyBufferToTexture", "copyTextureToBuffer", "copyTextureToTexture"}, at = @At("HEAD"))
    private void txoptimizations$profileCopy(CallbackInfo ci) {
        if (GpuPassProfiler.isRunning())
            this.txoptimizations$mark("copy");
    }

    @Inject(method = "submit", at = @At("HEAD"))
    private void txoptimizations$profileFrameEnd(CallbackInfo ci) {
        GpuPassProfiler.endFrame(this.device, (CommandEncoderBackend) this);
    }

    @Unique
    private void txoptimizations$mark(String label) {
        if (this.currentRenderPass == null)
            GpuPassProfiler.mark(this.device, (CommandEncoderBackend) this, label);
    }
}
