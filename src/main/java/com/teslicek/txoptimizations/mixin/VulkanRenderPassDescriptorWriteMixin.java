package com.teslicek.txoptimizations.mixin;

import com.mojang.renderpearl.api.buffers.GpuBufferSlice;
import com.mojang.renderpearl.api.pipeline.BindGroupLayout;
import com.mojang.renderpearl.api.pipeline.UniformType;
import com.mojang.renderpearl.backend.vulkan.VulkanCommandEncoder;
import com.mojang.renderpearl.backend.vulkan.VulkanConst;
import com.mojang.renderpearl.backend.vulkan.VulkanDevice;
import com.mojang.renderpearl.backend.vulkan.VulkanGpuBuffer;
import com.mojang.renderpearl.backend.vulkan.VulkanGpuSampler;
import com.mojang.renderpearl.backend.vulkan.VulkanGpuTextureView;
import com.mojang.renderpearl.backend.vulkan.VulkanRenderPass;
import com.mojang.renderpearl.backend.vulkan.VulkanRenderPipeline;
import com.mojang.renderpearl.backend.vulkan.VulkanUtils;
import com.mojang.renderpearl.util.TextureViewAndSampler;
import com.teslicek.txoptimizations.DynamicStateCache;
import com.teslicek.txoptimizations.TexelViewCache;
import it.unimi.dsi.fastutil.objects.ReferenceList;
import java.util.List;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.KHRPushDescriptor;
import org.lwjgl.vulkan.VK10;
import org.lwjgl.vulkan.VkBufferViewCreateInfo;
import org.lwjgl.vulkan.VkCommandBuffer;
import org.lwjgl.vulkan.VkDescriptorBufferInfo;
import org.lwjgl.vulkan.VkDescriptorImageInfo;
import org.lwjgl.vulkan.VkWriteDescriptorSet;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(value = VulkanRenderPass.class, priority = 999)
public abstract class VulkanRenderPassDescriptorWriteMixin {

    @Unique
    private static final int UNIFORM_BUFFER_DESCRIPTOR = 6;

    @Unique
    private static final int IMAGE_SAMPLER_DESCRIPTOR = 1;

    @Unique
    private static final int TEXEL_BUFFER_DESCRIPTOR = 4;

    @Unique
    private static final int SHADER_READ_ONLY_LAYOUT = 1;

    @Shadow
    @Final
    private VulkanDevice device;

    @Shadow
    @Final
    private VulkanCommandEncoder encoder;

    @Shadow
    @Final
    private VkCommandBuffer commandBuffer;

    @Shadow
    protected VulkanRenderPipeline pipeline;

    @Shadow
    private boolean anyDescriptorDirty;

    @Shadow
    @Final
    protected ReferenceList<Object> uniforms;

    @Overwrite
    private void pushDescriptors() {
        if (!this.anyDescriptorDirty)
            return;

        if (!DynamicStateCache.changesDescriptors(this.commandBuffer.address(), this.pipeline.pipelineLayout(), this.uniforms)) {
            this.anyDescriptorDirty = false;
            return;
        }

        List<BindGroupLayout.UniformDescription> uniforms = this.pipeline.uniforms();
        int                                      count    = uniforms.size();

        try (MemoryStack stack = MemoryStack.stackPush()) {
            long writes = stack.ncalloc(VkWriteDescriptorSet.ALIGNOF, count, VkWriteDescriptorSet.SIZEOF);

            for (int index = 0; index < count; index ++) {
                BindGroupLayout.UniformDescription uniform = uniforms.get(index);
                long                               write   = writes + (long) index * VkWriteDescriptorSet.SIZEOF;

                VkWriteDescriptorSet.nsType(write, VK10.VK_STRUCTURE_TYPE_WRITE_DESCRIPTOR_SET);
                VkWriteDescriptorSet.ndstBinding(write, index);
                VkWriteDescriptorSet.ndstArrayElement(write, 0);
                VkWriteDescriptorSet.ndescriptorCount(write, 1);

                if (uniform.type() == UniformType.UNIFORM_BUFFER) {
                    GpuBufferSlice buffer = (GpuBufferSlice) this.uniforms.get(index);

                    if (buffer == null)
                        throw new IllegalStateException("Missing uniform " + uniform.name() + " (should be " + uniform.type() + ")");

                    long info = stack.ncalloc(VkDescriptorBufferInfo.ALIGNOF, 1, VkDescriptorBufferInfo.SIZEOF);

                    VkDescriptorBufferInfo.nbuffer(info, ((VulkanGpuBuffer) buffer.buffer()).vkBuffer());
                    VkDescriptorBufferInfo.noffset(info, buffer.offset());
                    VkDescriptorBufferInfo.nrange(info, buffer.length());
                    VkWriteDescriptorSet.ndescriptorType(write, UNIFORM_BUFFER_DESCRIPTOR);
                    MemoryUtil.memPutAddress(write + VkWriteDescriptorSet.PBUFFERINFO, info);
                } else if (uniform.type() == UniformType.COMBINED_IMAGE_SAMPLER) {
                    TextureViewAndSampler value = (TextureViewAndSampler) this.uniforms.get(index);

                    if (value == null)
                        throw new IllegalStateException("Missing sampler " + uniform.name());

                    long info = stack.ncalloc(VkDescriptorImageInfo.ALIGNOF, 1, VkDescriptorImageInfo.SIZEOF);

                    VkDescriptorImageInfo.nsampler(info, ((VulkanGpuSampler) value.sampler()).vkSampler());
                    VkDescriptorImageInfo.nimageView(info, ((VulkanGpuTextureView) value.view()).vkImageView());
                    VkDescriptorImageInfo.nimageLayout(info, SHADER_READ_ONLY_LAYOUT);
                    VkWriteDescriptorSet.ndescriptorType(write, IMAGE_SAMPLER_DESCRIPTOR);
                    MemoryUtil.memPutAddress(write + VkWriteDescriptorSet.PIMAGEINFO, info);
                } else if (uniform.type() == UniformType.TEXEL_BUFFER) {
                    GpuBufferSlice value = (GpuBufferSlice) this.uniforms.get(index);

                    if (value == null)
                        throw new IllegalStateException("Missing uniform " + uniform.name() + " (should be " + uniform.type() + ")");

                    long view = stack.ncalloc(Long.BYTES, 1, Long.BYTES);

                    MemoryUtil.memPutLong(view, this.txoptimizations$texelView(stack, value, VulkanConst.toVk(uniform.gpuFormat())));
                    VkWriteDescriptorSet.ndescriptorType(write, TEXEL_BUFFER_DESCRIPTOR);
                    MemoryUtil.memPutAddress(write + VkWriteDescriptorSet.PTEXELBUFFERVIEW, view);
                }
            }

            KHRPushDescriptor.nvkCmdPushDescriptorSetKHR(this.commandBuffer, VK10.VK_PIPELINE_BIND_POINT_GRAPHICS, this.pipeline.pipelineLayout(), 0, count, writes);
        }

        this.anyDescriptorDirty = false;
    }

    @Unique
    private long txoptimizations$texelView(MemoryStack stack, GpuBufferSlice slice, int format) {
        VulkanGpuBuffer buffer = (VulkanGpuBuffer) slice.buffer();
        TexelViewCache  cache  = buffer instanceof VulkanGpuBuffer.Direct direct ? (TexelViewCache) direct : null;

        if (cache != null) {
            long cached = cache.txoptimizations$findTexelView(slice.offset(), slice.length(), format);

            if (cached != 0L)
                return cached;
        }

        long view;

        try (MemoryStack frame = stack.push()) {
            long info   = frame.ncalloc(VkBufferViewCreateInfo.ALIGNOF, 1, VkBufferViewCreateInfo.SIZEOF);
            long handle = frame.ncalloc(Long.BYTES, 1, Long.BYTES);

            VkBufferViewCreateInfo.nsType(info, VK10.VK_STRUCTURE_TYPE_BUFFER_VIEW_CREATE_INFO);
            VkBufferViewCreateInfo.nbuffer(info, buffer.vkBuffer());
            VkBufferViewCreateInfo.noffset(info, slice.offset());
            VkBufferViewCreateInfo.nrange(info, slice.length());
            VkBufferViewCreateInfo.nformat(info, format);
            VulkanUtils.crashIfFailure(this.device, VK10.nvkCreateBufferView(this.device.vkDevice(), info, MemoryUtil.NULL, handle), "Couldn't create buffer view for texel buffer");

            view = MemoryUtil.memGetLong(handle);
        }

        if (cache != null && cache.txoptimizations$storeTexelView(slice.offset(), slice.length(), format, view))
            return view;

        long destroyed = view;

        this.encoder.queueForDestroy(() -> VK10.vkDestroyBufferView(this.device.vkDevice(), destroyed, null));

        return view;
    }
}
