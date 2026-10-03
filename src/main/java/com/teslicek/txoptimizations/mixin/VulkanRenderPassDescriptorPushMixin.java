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
import com.teslicek.txoptimizations.TexelViewCache;
import it.unimi.dsi.fastutil.objects.ReferenceList;
import java.nio.LongBuffer;
import java.util.List;
import java.util.Objects;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.KHRPushDescriptor;
import org.lwjgl.vulkan.VK12;
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

@Mixin(VulkanRenderPass.class)
public abstract class VulkanRenderPassDescriptorPushMixin {

    @Unique
    private static final int UNIFORM_BUFFER_DESCRIPTOR = 6;

    @Unique
    private static final int COMBINED_IMAGE_SAMPLER_DESCRIPTOR = 1;

    @Unique
    private static final int UNIFORM_TEXEL_BUFFER_DESCRIPTOR = 4;

    @Unique
    private static final int SHADER_READ_ONLY_LAYOUT = 1;

    @Shadow
    @Final
    private VulkanDevice device;

    @Shadow
    @Final
    private VulkanCommandEncoder encoder;

    @Shadow
    protected VulkanRenderPipeline pipeline;

    @Shadow
    private boolean anyDescriptorDirty;

    @Shadow
    @Final
    protected ReferenceList<Object> uniforms;

    @Unique
    private Object[] txoptimizations$pushed;

    @Unique
    private long txoptimizations$pushedLayout;

    @Shadow
    private VkCommandBuffer commandBuffer() {
        throw new AssertionError();
    }

    @Overwrite
    private void pushDescriptors() {
        if (!this.anyDescriptorDirty)
            return;

        List<BindGroupLayout.UniformDescription> descriptions = this.pipeline.uniforms();
        int                                      size         = descriptions.size();
        long                                     layout       = this.pipeline.pipelineLayout();
        Object[]                                 pushed       = this.txoptimizations$pushed;
        boolean                                  partial      = pushed != null && pushed.length == size && this.txoptimizations$pushedLayout == layout;

        if (pushed == null || pushed.length != size)
            pushed = new Object[size];

        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkWriteDescriptorSet.Buffer writes = VkWriteDescriptorSet.calloc(size, stack);

            for (int index = 0; index < size; index ++) {
                Object value = this.uniforms.get(index);

                if (partial && Objects.equals(pushed[index], value))
                    continue;

                BindGroupLayout.UniformDescription description = descriptions.get(index);

                if (value == null)
                    throw new IllegalStateException("Missing uniform " + description.name() + " (should be " + description.type() + ")");

                this.txoptimizations$write(writes.get().sType$Default(), index, description, value, stack);
                pushed[index] = value;
            }

            if (writes.position() > 0)
                KHRPushDescriptor.vkCmdPushDescriptorSetKHR(this.commandBuffer(), 0, layout, 0, writes.flip());
        }

        this.txoptimizations$pushed       = pushed;
        this.txoptimizations$pushedLayout = layout;
        this.anyDescriptorDirty           = false;
    }

    @Unique
    private void txoptimizations$write(VkWriteDescriptorSet write, int binding, BindGroupLayout.UniformDescription description, Object value, MemoryStack stack) {
        UniformType type = description.type();

        write.dstBinding(binding);
        write.dstArrayElement(0);
        write.descriptorCount(1);

        if (type == UniformType.UNIFORM_BUFFER) {
            GpuBufferSlice slice = (GpuBufferSlice) value;

            write.descriptorType(UNIFORM_BUFFER_DESCRIPTOR);
            write.pBufferInfo(VkDescriptorBufferInfo.calloc(1, stack).buffer(((VulkanGpuBuffer) slice.buffer()).vkBuffer()).offset(slice.offset()).range(slice.length()));

            return;
        }

        if (type == UniformType.COMBINED_IMAGE_SAMPLER) {
            TextureViewAndSampler texture = (TextureViewAndSampler) value;

            write.descriptorType(COMBINED_IMAGE_SAMPLER_DESCRIPTOR);
            write.pImageInfo(VkDescriptorImageInfo.calloc(1, stack).sampler(((VulkanGpuSampler) texture.sampler()).vkSampler()).imageView(((VulkanGpuTextureView) texture.view()).vkImageView()).imageLayout(SHADER_READ_ONLY_LAYOUT));

            return;
        }

        if (type != UniformType.TEXEL_BUFFER)
            throw new IllegalStateException("Unsupported uniform type " + type + " for " + description.name());

        if (description.gpuFormat() == null)
            throw new IllegalStateException("Texel buffer " + description.name() + " has no format");

        write.descriptorType(UNIFORM_TEXEL_BUFFER_DESCRIPTOR);
        write.pTexelBufferView(stack.longs(this.txoptimizations$texelView((GpuBufferSlice) value, VulkanConst.toVk(description.gpuFormat()), stack)));
    }

    @Unique
    private long txoptimizations$texelView(GpuBufferSlice slice, int format, MemoryStack stack) {
        VulkanGpuBuffer buffer = (VulkanGpuBuffer) slice.buffer();
        TexelViewCache  cache  = buffer instanceof VulkanGpuBuffer.Direct direct ? (TexelViewCache) direct : null;

        if (cache != null) {
            long cached = cache.txoptimizations$findTexelView(slice.offset(), slice.length(), format);

            if (cached != 0L)
                return cached;
        }

        long view = this.txoptimizations$createTexelView(buffer.vkBuffer(), slice.offset(), slice.length(), format, stack);

        if (cache == null || !cache.txoptimizations$storeTexelView(slice.offset(), slice.length(), format, view))
            this.encoder.queueForDestroy(() -> VK12.vkDestroyBufferView(this.device.vkDevice(), view, null));

        return view;
    }

    @Unique
    private long txoptimizations$createTexelView(long buffer, long offset, long range, int format, MemoryStack stack) {
        try (MemoryStack frame = stack.push()) {
            LongBuffer             handle = frame.callocLong(1);
            VkBufferViewCreateInfo info   = VkBufferViewCreateInfo.calloc(frame).sType$Default().buffer(buffer).offset(offset).range(range).format(format);

            VulkanUtils.crashIfFailure(this.device, VK12.vkCreateBufferView(this.device.vkDevice(), info, null, handle), "Couldn't create buffer view for texel buffer");

            return handle.get(0);
        }
    }
}
