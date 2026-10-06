package com.teslicek.txoptimizations.mixin;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.renderpearl.api.buffers.GpuBufferSlice;
import com.mojang.renderpearl.backend.vulkan.Destroyable;
import com.mojang.renderpearl.backend.vulkan.VulkanCommandEncoder;
import com.mojang.renderpearl.backend.vulkan.VulkanGpuBuffer;
import com.mojang.renderpearl.backend.vulkan.VulkanRenderPass;
import com.teslicek.txoptimizations.TexelViewCache;
import java.nio.LongBuffer;
import org.lwjgl.vulkan.VK10;
import org.lwjgl.vulkan.VkAllocationCallbacks;
import org.lwjgl.vulkan.VkBufferViewCreateInfo;
import org.lwjgl.vulkan.VkDevice;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(VulkanRenderPass.class)
public abstract class VulkanRenderPassTexelViewMixin {

    @Unique
    private boolean txoptimizations$texelViewCached;

    @WrapOperation(method = "pushDescriptors", at = @At(value = "INVOKE", target = "Lorg/lwjgl/vulkan/VK12;vkCreateBufferView(Lorg/lwjgl/vulkan/VkDevice;Lorg/lwjgl/vulkan/VkBufferViewCreateInfo;Lorg/lwjgl/vulkan/VkAllocationCallbacks;Ljava/nio/LongBuffer;)I"))
    private int txoptimizations$reuseTexelView(VkDevice device, VkBufferViewCreateInfo info, VkAllocationCallbacks allocator, LongBuffer view, Operation<Integer> original, @Local GpuBufferSlice slice) {
        this.txoptimizations$texelViewCached = false;

        if (!(slice.buffer() instanceof VulkanGpuBuffer.Direct direct))
            return original.call(device, info, allocator, view);

        TexelViewCache cache  = (TexelViewCache) direct;
        long           handle = cache.txoptimizations$findTexelView(info.offset(), info.range(), info.format());

        if (handle != 0L) {
            view.put(0, handle);
            this.txoptimizations$texelViewCached = true;

            return VK10.VK_SUCCESS;
        }

        int result = original.call(device, info, allocator, view);

        if (result == VK10.VK_SUCCESS)
            this.txoptimizations$texelViewCached = cache.txoptimizations$storeTexelView(info.offset(), info.range(), info.format(), view.get(0));

        return result;
    }

    @WrapWithCondition(method = "pushDescriptors", at = @At(value = "INVOKE", target = "Lcom/mojang/renderpearl/backend/vulkan/VulkanCommandEncoder;queueForDestroy(Lcom/mojang/renderpearl/backend/vulkan/Destroyable;)V"))
    private boolean txoptimizations$keepCachedTexelView(VulkanCommandEncoder encoder, Destroyable destroyable) {
        return !this.txoptimizations$texelViewCached;
    }
}
