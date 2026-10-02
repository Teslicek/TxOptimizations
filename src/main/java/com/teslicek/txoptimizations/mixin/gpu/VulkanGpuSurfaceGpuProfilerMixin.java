package com.teslicek.txoptimizations.mixin.gpu;

import com.mojang.renderpearl.backend.api.CommandEncoderBackend;
import com.mojang.renderpearl.backend.vulkan.VulkanDevice;
import com.mojang.renderpearl.backend.vulkan.VulkanGpuSurface;
import com.teslicek.txoptimizations.gpu.GpuPassProfiler;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(VulkanGpuSurface.class)
public abstract class VulkanGpuSurfaceGpuProfilerMixin {

    @Shadow
    @Final
    private VulkanDevice device;

    @Inject(method = "blitFromTexture", at = @At("HEAD"))
    private void txoptimizations$profileScreenCopy(CommandEncoderBackend encoder, CallbackInfo ci) {
        if (GpuPassProfiler.isRunning())
            GpuPassProfiler.mark(this.device, encoder, "screen copy and wait for screen image");
    }
}
