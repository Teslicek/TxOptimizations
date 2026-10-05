package com.teslicek.txoptimizations.mixin;

import com.mojang.renderpearl.backend.vulkan.VulkanRenderPass;
import com.teslicek.txoptimizations.PushConstantCall;
import java.nio.ByteBuffer;
import org.lwjgl.vulkan.VkCommandBuffer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(VulkanRenderPass.class)
public abstract class VulkanRenderPassPushConstantMixin {

    @Redirect(method = "pushConstants", at = @At(value = "INVOKE", target = "Lorg/lwjgl/vulkan/VK12;vkCmdPushConstants(Lorg/lwjgl/vulkan/VkCommandBuffer;JIILjava/nio/ByteBuffer;)V"))
    private void txoptimizations$pushWithoutAddressWrapper(VkCommandBuffer commandBuffer, long layout, int stageFlags, int offset, ByteBuffer values) {
        PushConstantCall.push(commandBuffer, layout, stageFlags, offset, values);
    }
}
