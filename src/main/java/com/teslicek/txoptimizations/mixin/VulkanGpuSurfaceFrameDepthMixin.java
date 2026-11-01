package com.teslicek.txoptimizations.mixin;

import com.mojang.renderpearl.backend.vulkan.VulkanGpuSurface;
import com.teslicek.txoptimizations.FrameDepth;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(VulkanGpuSurface.class)
public abstract class VulkanGpuSurfaceFrameDepthMixin {

    @ModifyConstant(method = "<init>", constant = @Constant(intValue = FrameDepth.VANILLA_SLOTS))
    private int txoptimizations$acquireSlots(int original) {
        return FrameDepth.SLOTS;
    }
}
