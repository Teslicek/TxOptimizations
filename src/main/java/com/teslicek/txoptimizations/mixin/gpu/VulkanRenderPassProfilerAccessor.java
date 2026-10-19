package com.teslicek.txoptimizations.mixin.gpu;

import com.mojang.renderpearl.backend.vulkan.VulkanDevice;
import com.mojang.renderpearl.backend.vulkan.VulkanRenderPass;
import org.lwjgl.vulkan.VkCommandBuffer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(VulkanRenderPass.class)
public interface VulkanRenderPassProfilerAccessor {

    @Accessor("device")
    VulkanDevice txoptimizations$device();

    @Accessor("commandBuffer")
    VkCommandBuffer txoptimizations$commandBuffer();
}
