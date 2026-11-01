package com.teslicek.txoptimizations.mixin;

import com.mojang.renderpearl.backend.vulkan.VulkanCommandEncoder;
import com.teslicek.txoptimizations.FrameDepth;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(VulkanCommandEncoder.class)
public abstract class VulkanCommandEncoderFrameDepthMixin {

    @ModifyConstant(method = {"<init>", "destroy"}, constant = @Constant(intValue = FrameDepth.VANILLA_SLOTS))
    private int txoptimizations$submitSlots(int original) {
        return FrameDepth.SLOTS;
    }

    @ModifyConstant(method = "currentCommandPool", constant = @Constant(longValue = FrameDepth.VANILLA_SLOTS))
    private long txoptimizations$commandPoolSlots(long original) {
        return FrameDepth.SLOTS;
    }

    @ModifyConstant(method = "submit", constant = @Constant(longValue = FrameDepth.VANILLA_SLOTS))
    private long txoptimizations$submitsInFlight(long original) {
        return FrameDepth.SLOTS;
    }
}
