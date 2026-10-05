package com.teslicek.txoptimizations.mixin.gpu;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.renderpearl.api.buffers.GpuBufferSlice;
import com.mojang.renderpearl.api.commands.GpuQueryPool;
import com.mojang.renderpearl.api.commands.RenderPassDescriptor;
import com.mojang.renderpearl.backend.api.CommandEncoderBackend;
import com.mojang.renderpearl.backend.vulkan.VulkanCommandEncoder;
import com.mojang.renderpearl.backend.vulkan.VulkanDevice;
import com.mojang.renderpearl.backend.vulkan.VulkanRenderPass;
import com.teslicek.txoptimizations.gpu.GpuPassProfiler;
import com.teslicek.txoptimizations.gpu.ProfileCounters;
import org.lwjgl.vulkan.VkDevice;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
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
        ProfileCounters.countRenderPass();

        if (GpuPassProfiler.isRunning())
            this.txoptimizations$mark(descriptor.label().get());
    }

    @Inject(method = {"clearColorTexture", "clearDepthTexture", "clearColorAndDepthTextures"}, at = @At("HEAD"))
    private void txoptimizations$profileClear(CallbackInfo ci) {
        if (GpuPassProfiler.isRunning())
            this.txoptimizations$mark("clear");
    }

    @ModifyVariable(method = {"writeToBuffer", "copyToBuffer"}, at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private GpuBufferSlice txoptimizations$profileBufferCopy(GpuBufferSlice slice) {
        if (GpuPassProfiler.isRunning())
            this.txoptimizations$mark(GpuPassProfiler.copyLabel(slice.buffer()));

        return slice;
    }

    @Inject(method = {"writeToTexture", "copyBufferToTexture", "copyTextureToBuffer", "copyTextureToTexture"}, at = @At("HEAD"))
    private void txoptimizations$profileTextureCopy(CallbackInfo ci) {
        if (GpuPassProfiler.isRunning())
            this.txoptimizations$mark(GpuPassProfiler.copyLabel());
    }

    @Inject(method = "submit", at = @At("HEAD"))
    private void txoptimizations$profileFrameEnd(CallbackInfo ci) {
        GpuPassProfiler.endFrame(this.device, (CommandEncoderBackend) this);
    }

    @WrapWithCondition(method = "writeTimestamp", at = @At(value = "INVOKE", target = "Lorg/lwjgl/vulkan/VK12;vkResetQueryPool(Lorg/lwjgl/vulkan/VkDevice;JII)V"))
    private boolean txoptimizations$skipProfilerQueryReset(VkDevice vkDevice, long queryPool, int firstQuery, int queryCount, @Local(argsOnly = true) GpuQueryPool pool) {
        return !GpuPassProfiler.ownsPool(pool);
    }

    @Unique
    private void txoptimizations$mark(String label) {
        if (this.currentRenderPass == null)
            GpuPassProfiler.mark(this.device, (CommandEncoderBackend) this, label);
    }
}
