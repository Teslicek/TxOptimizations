package com.teslicek.txoptimizations.mixin;

import com.mojang.renderpearl.backend.vulkan.VulkanCommandEncoder;
import com.teslicek.txoptimizations.PreciseBarriers;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkCommandBuffer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(VulkanCommandEncoder.class)
public abstract class VulkanCommandEncoderPassBarrierMixin {

    @Shadow
    private VkCommandBuffer currentCommandBuffer;

    @Redirect(method = "submitRenderPass", at = @At(value = "INVOKE", target = "Lcom/mojang/renderpearl/backend/vulkan/VulkanCommandEncoder;memoryBarrier(Lorg/lwjgl/system/MemoryStack;)V"))
    private void txoptimizations$attachmentBarrier(VulkanCommandEncoder encoder, MemoryStack stack) {
        PreciseBarriers.afterRenderPass(this.currentCommandBuffer);
    }
}
