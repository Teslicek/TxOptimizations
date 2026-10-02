package com.teslicek.txoptimizations.mixin;

import com.mojang.renderpearl.api.buffers.GpuBufferSlice;
import com.mojang.renderpearl.api.commands.RenderPass;
import com.mojang.renderpearl.api.commands.RenderPassDescriptor;
import com.mojang.renderpearl.api.textures.GpuTexture;
import com.mojang.renderpearl.api.textures.GpuTextureView;
import com.mojang.renderpearl.backend.vulkan.VulkanCommandEncoder;
import com.mojang.renderpearl.backend.vulkan.VulkanGpuBuffer;
import com.mojang.renderpearl.backend.vulkan.VulkanRenderPass;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;
import org.joml.Vector4fc;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkCommandBuffer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(VulkanCommandEncoder.class)
public abstract class VulkanCommandEncoderDeferralMixin {

    @Shadow
    private VkCommandBuffer currentCommandBuffer;

    @Shadow
    private VulkanRenderPass currentRenderPass;

    @Unique
    private final LongArrayList txoptimizations$pendingRanges = new LongArrayList();

    @Unique
    private int txoptimizations$deferDepth;

    @Unique
    private GpuTexture txoptimizations$clearColorTexture;

    @Unique
    private Vector4fc txoptimizations$clearColor;

    @Unique
    private GpuTexture txoptimizations$clearDepthTexture;

    @Unique
    private double txoptimizations$clearDepth;

    @Unique
    private boolean txoptimizations$barrierPending;

    @Unique
    private long txoptimizations$uploadBuffer;

    @Unique
    private long txoptimizations$uploadStart;

    @Unique
    private long txoptimizations$uploadEnd;

    @Unique
    private long txoptimizations$readBuffer;

    @Unique
    private long txoptimizations$readStart;

    @Unique
    private long txoptimizations$readEnd;

    @Shadow
    public abstract VkCommandBuffer allocateAndBeginTransientCommandBuffer();

    @Shadow
    public abstract void clearDepthTextureUnsynced(MemoryStack stack, GpuTexture depthTexture, double clearDepth);

    @Shadow
    private void clearColorTextureUnsynced(MemoryStack stack, GpuTexture colorTexture, Vector4fc clearColor) {
        throw new AssertionError();
    }

    @Shadow
    private void memoryBarrier(MemoryStack stack) {
        throw new AssertionError();
    }

    @Overwrite
    private VkCommandBuffer commandBuffer() {
        if (this.txoptimizations$deferDepth == 0) {
            this.txoptimizations$flushClears();
            this.txoptimizations$flushBarrier();
        }

        if (this.currentCommandBuffer != null)
            return this.currentCommandBuffer;

        if (this.currentRenderPass != null)
            throw new IllegalStateException("Cannot start command buffer while inside RenderPass");

        this.currentCommandBuffer = this.allocateAndBeginTransientCommandBuffer();

        return this.currentCommandBuffer;
    }

    @Inject(method = "endCommandBuffer", at = @At("HEAD"))
    private void txoptimizations$flushBeforeEnd(CallbackInfo ci) {
        this.txoptimizations$flushClears();
        this.txoptimizations$flushBarrier();
    }

    @Inject(method = "clearDepthTexture", at = @At("HEAD"), cancellable = true)
    private void txoptimizations$deferDepthClear(GpuTexture depthTexture, double clearDepth, CallbackInfo ci) {
        this.txoptimizations$flushClears();
        this.txoptimizations$clearDepthTexture = depthTexture;
        this.txoptimizations$clearDepth        = clearDepth;
        ci.cancel();
    }

    @Inject(method = "clearColorAndDepthTextures(Lcom/mojang/renderpearl/api/textures/GpuTexture;Lorg/joml/Vector4fc;Lcom/mojang/renderpearl/api/textures/GpuTexture;D)V", at = @At("HEAD"), cancellable = true)
    private void txoptimizations$deferColorAndDepthClear(GpuTexture colorTexture, Vector4fc clearColor, GpuTexture depthTexture, double clearDepth, CallbackInfo ci) {
        this.txoptimizations$flushClears();
        this.txoptimizations$clearColorTexture = colorTexture;
        this.txoptimizations$clearColor        = clearColor;
        this.txoptimizations$clearDepthTexture = depthTexture;
        this.txoptimizations$clearDepth        = clearDepth;
        ci.cancel();
    }

    @ModifyVariable(method = "createRenderPass", at = @At("HEAD"), argsOnly = true)
    private RenderPassDescriptor txoptimizations$clearOnLoad(RenderPassDescriptor descriptor) {
        if (this.txoptimizations$clearColorTexture == null && this.txoptimizations$clearDepthTexture == null)
            return descriptor;

        RenderPass.RenderArea                                      area   = descriptor.renderArea();
        List<RenderPassDescriptor.Attachment<Optional<Vector4fc>>> colors = new ArrayList<>(descriptor.colorAttachments());
        RenderPassDescriptor.Attachment<OptionalDouble>            depth  = descriptor.depthAttachment();

        for (int index = 0; index < colors.size(); index ++) {
            RenderPassDescriptor.Attachment<Optional<Vector4fc>> color = colors.get(index);

            if (color == null || this.txoptimizations$clearColorTexture == null || !txoptimizations$coversTexture(color.textureView(), this.txoptimizations$clearColorTexture, area))
                continue;

            if (color.clearValue().isEmpty())
                colors.set(index, new RenderPassDescriptor.Attachment<>(color.textureView(), Optional.of(this.txoptimizations$clearColor)));

            this.txoptimizations$clearColorTexture = null;
        }

        if (depth != null && this.txoptimizations$clearDepthTexture != null && txoptimizations$coversTexture(depth.textureView(), this.txoptimizations$clearDepthTexture, area)) {
            if (depth.clearValue().isEmpty())
                depth = new RenderPassDescriptor.Attachment<>(depth.textureView(), OptionalDouble.of(this.txoptimizations$clearDepth));

            this.txoptimizations$clearDepthTexture = null;
        }

        this.txoptimizations$flushClears();

        return new RenderPassDescriptor(descriptor.label(), colors, depth, area);
    }

    @Inject(method = "writeToBuffer", at = @At("HEAD"))
    private void txoptimizations$beginUpload(GpuBufferSlice destination, ByteBuffer data, CallbackInfo ci) {
        long buffer = ((VulkanGpuBuffer) destination.buffer()).vkBuffer();
        long start  = destination.offset();
        long end    = start + data.remaining();

        if (this.txoptimizations$overlapsPending(buffer, start, end))
            this.txoptimizations$flushBarrier();

        this.txoptimizations$uploadBuffer = buffer;
        this.txoptimizations$uploadStart  = start;
        this.txoptimizations$uploadEnd    = end;
        this.txoptimizations$readBuffer   = 0L;
        this.txoptimizations$deferDepth ++;
    }

    @Inject(method = "copyToBuffer", at = @At("HEAD"))
    private void txoptimizations$beginCopy(GpuBufferSlice source, GpuBufferSlice target, CallbackInfo ci) {
        long sourceBuffer = ((VulkanGpuBuffer) source.buffer()).vkBuffer();
        long targetBuffer = ((VulkanGpuBuffer) target.buffer()).vkBuffer();
        long sourceStart  = source.offset();
        long sourceEnd    = sourceStart + source.length();
        long targetStart  = target.offset();
        long targetEnd    = targetStart + source.length();

        if (this.txoptimizations$overlapsPending(sourceBuffer, sourceStart, sourceEnd) || this.txoptimizations$overlapsPending(targetBuffer, targetStart, targetEnd))
            this.txoptimizations$flushBarrier();

        this.txoptimizations$uploadBuffer = targetBuffer;
        this.txoptimizations$uploadStart  = targetStart;
        this.txoptimizations$uploadEnd    = targetEnd;
        this.txoptimizations$readBuffer   = sourceBuffer;
        this.txoptimizations$readStart    = sourceStart;
        this.txoptimizations$readEnd      = sourceEnd;
        this.txoptimizations$deferDepth ++;
    }

    @Inject(method = "writeTimestamp", at = @At("HEAD"))
    private void txoptimizations$beginTimestamp(CallbackInfo ci) {
        this.txoptimizations$deferDepth ++;
    }

    @Inject(method = {"writeTimestamp", "writeToBuffer", "copyToBuffer"}, at = @At("RETURN"))
    private void txoptimizations$endDeferred(CallbackInfo ci) {
        this.txoptimizations$deferDepth --;
    }

    @Redirect(method = {"writeToBuffer", "copyToBuffer"}, at = @At(value = "INVOKE", target = "Lcom/mojang/renderpearl/backend/vulkan/VulkanCommandEncoder;memoryBarrier(Lorg/lwjgl/system/MemoryStack;)V"))
    private void txoptimizations$deferUploadBarrier(VulkanCommandEncoder encoder, MemoryStack stack) {
        this.txoptimizations$pendingRanges.add(this.txoptimizations$uploadBuffer);
        this.txoptimizations$pendingRanges.add(this.txoptimizations$uploadStart);
        this.txoptimizations$pendingRanges.add(this.txoptimizations$uploadEnd);

        if (this.txoptimizations$readBuffer != 0L) {
            this.txoptimizations$pendingRanges.add(this.txoptimizations$readBuffer);
            this.txoptimizations$pendingRanges.add(this.txoptimizations$readStart);
            this.txoptimizations$pendingRanges.add(this.txoptimizations$readEnd);
        }

        this.txoptimizations$barrierPending = true;
    }

    @Unique
    private void txoptimizations$flushClears() {
        GpuTexture color = this.txoptimizations$clearColorTexture;
        GpuTexture depth = this.txoptimizations$clearDepthTexture;

        if (color == null && depth == null)
            return;

        this.txoptimizations$clearColorTexture = null;
        this.txoptimizations$clearDepthTexture = null;

        try (MemoryStack stack = MemoryStack.stackPush()) {
            if (color != null)
                this.clearColorTextureUnsynced(stack, color, this.txoptimizations$clearColor);

            if (depth != null)
                this.clearDepthTextureUnsynced(stack, depth, this.txoptimizations$clearDepth);

            this.memoryBarrier(stack);
        }
    }

    @Unique
    private void txoptimizations$flushBarrier() {
        if (!this.txoptimizations$barrierPending)
            return;

        if (this.currentCommandBuffer == null)
            throw new IllegalStateException("Upload barrier pending without a recording command buffer");

        this.txoptimizations$barrierPending = false;
        this.txoptimizations$pendingRanges.clear();

        try (MemoryStack stack = MemoryStack.stackPush()) {
            VulkanCommandEncoder.memoryBarrier(this.currentCommandBuffer, stack);
        }
    }

    @Unique
    private boolean txoptimizations$overlapsPending(long buffer, long start, long end) {
        LongArrayList ranges = this.txoptimizations$pendingRanges;

        for (int index = 0; index < ranges.size(); index += 3) {
            if (ranges.getLong(index) == buffer && ranges.getLong(index + 1) < end && start < ranges.getLong(index + 2))
                return true;
        }

        return false;
    }

    @Unique
    private static boolean txoptimizations$coversTexture(GpuTextureView view, GpuTexture texture, RenderPass.RenderArea area) {
        return view.texture() == texture && view.baseMipLevel() == 0 && view.mipLevels() == texture.getMipLevels() && area.fillsTexture(view);
    }
}
