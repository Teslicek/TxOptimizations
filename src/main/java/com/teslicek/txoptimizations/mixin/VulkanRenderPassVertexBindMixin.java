package com.teslicek.txoptimizations.mixin;

import com.mojang.renderpearl.api.buffers.GpuBufferSlice;
import com.mojang.renderpearl.backend.vulkan.VulkanGpuBuffer;
import com.mojang.renderpearl.backend.vulkan.VulkanRenderPass;
import org.jspecify.annotations.Nullable;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.VK10;
import org.lwjgl.vulkan.VkCommandBuffer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(VulkanRenderPass.class)
public abstract class VulkanRenderPassVertexBindMixin {

    @Shadow
    @Final
    private VkCommandBuffer commandBuffer;

    @Overwrite
    public void setVertexBuffer(int slot, @Nullable GpuBufferSlice vertexBuffer) {
        if (vertexBuffer == null)
            return;

        try (MemoryStack stack = MemoryStack.stackPush()) {
            long values = stack.nmalloc(Long.BYTES, 2 * Long.BYTES);

            MemoryUtil.memPutLong(values, ((VulkanGpuBuffer) vertexBuffer.buffer()).vkBuffer());
            MemoryUtil.memPutLong(values + Long.BYTES, vertexBuffer.offset());
            VK10.nvkCmdBindVertexBuffers(this.commandBuffer, slot, 1, values, values + Long.BYTES);
        }
    }
}
