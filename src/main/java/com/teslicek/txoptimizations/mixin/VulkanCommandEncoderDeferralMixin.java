package com.teslicek.txoptimizations.mixin;

import com.mojang.renderpearl.api.buffers.GpuBufferSlice;
import com.mojang.renderpearl.api.commands.RenderPass;
import com.mojang.renderpearl.api.commands.RenderPassDescriptor;
import com.mojang.renderpearl.api.textures.GpuTexture;
import com.mojang.renderpearl.api.textures.GpuTextureView;
import com.mojang.renderpearl.backend.api.RenderPassBackend;
import com.mojang.renderpearl.backend.vulkan.VulkanCommandEncoder;
import com.mojang.renderpearl.backend.vulkan.VulkanDebug;
import com.mojang.renderpearl.backend.vulkan.VulkanDevice;
import com.mojang.renderpearl.backend.vulkan.VulkanGpuBuffer;
import com.mojang.renderpearl.backend.vulkan.VulkanRenderPass;
import com.mojang.renderpearl.backend.vulkan.checkpoints.CheckpointExtension;
import com.teslicek.txoptimizations.gpu.GpuPassProfiler;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.function.Supplier;
import org.joml.Vector4fc;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.KHRDynamicRendering;
import org.lwjgl.vulkan.VkCommandBuffer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(VulkanCommandEncoder.class)
public abstract class VulkanCommandEncoderDeferralMixin {

    @Shadow
    private VkCommandBuffer currentCommandBuffer;

    @Shadow
    private VulkanRenderPass currentRenderPass;

    @Shadow
    @Final
    private VulkanDevice device;

    @Shadow
    @Final
    private CheckpointExtension.CheckpointStorage checkpointStorage;

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

    @Unique
    private VulkanRenderPass txoptimizations$suspendedPass;

    @Unique
    private RenderPassDescriptor txoptimizations$suspendedDescriptor;

    @Unique
    private RenderPassDescriptor txoptimizations$openDescriptor;

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
        this.txoptimizations$endSuspendedPass();

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
        this.txoptimizations$endSuspendedPass();
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

    @Inject(method = "createRenderPass", at = @At("HEAD"), cancellable = true)
    private void txoptimizations$continueSuspendedPass(RenderPassDescriptor descriptor, CallbackInfoReturnable<RenderPassBackend> cir) {
        if (!this.txoptimizations$canContinue(descriptor))
            return;

        VkCommandBuffer  commandBuffer = this.currentCommandBuffer;
        Supplier<String> previousLabel = this.txoptimizations$suspendedPass.getLabel();
        VulkanDebug      debug         = this.device.instance().debug();

        debug.endDebugGroup(commandBuffer);
        this.checkpointStorage.recordCheckpoint(commandBuffer, CheckpointExtension.CheckpointType.END_RENDER_PASS, previousLabel);
        debug.beginDebugGroup(commandBuffer, descriptor.label());
        this.checkpointStorage.recordCheckpoint(commandBuffer, CheckpointExtension.CheckpointType.BEGIN_RENDER_PASS, descriptor.label());

        this.txoptimizations$suspendedPass       = null;
        this.txoptimizations$suspendedDescriptor = null;
        this.txoptimizations$openDescriptor      = descriptor;
        this.currentRenderPass                   = new VulkanRenderPass(this.device, (VulkanCommandEncoder) (Object) this, commandBuffer, this.checkpointStorage, descriptor.renderArea(), txoptimizations$outputWidth(descriptor), txoptimizations$outputHeight(descriptor), descriptor.depthAttachment() != null, descriptor.label());

        cir.setReturnValue(this.currentRenderPass);
    }

    @Inject(method = "createRenderPass", at = @At("RETURN"))
    private void txoptimizations$rememberDescriptor(RenderPassDescriptor descriptor, CallbackInfoReturnable<RenderPassBackend> cir) {
        this.txoptimizations$openDescriptor = descriptor;
    }

    @Inject(method = "submitRenderPass", at = @At("HEAD"), cancellable = true)
    private void txoptimizations$suspendPass(CallbackInfo ci) {
        if (this.currentRenderPass == null || GpuPassProfiler.isRunning())
            return;

        if (this.txoptimizations$suspendedPass != null)
            throw new IllegalStateException("A render pass is already suspended");

        this.txoptimizations$suspendedPass       = this.currentRenderPass;
        this.txoptimizations$suspendedDescriptor = this.txoptimizations$openDescriptor;
        this.currentRenderPass                   = null;
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
    private boolean txoptimizations$canContinue(RenderPassDescriptor descriptor) {
        RenderPassDescriptor previous = this.txoptimizations$suspendedDescriptor;

        if (this.txoptimizations$suspendedPass == null || GpuPassProfiler.isRunning() || this.txoptimizations$barrierPending)
            return false;

        if (this.txoptimizations$clearColorTexture != null || this.txoptimizations$clearDepthTexture != null)
            return false;

        if (!descriptor.renderArea().equals(previous.renderArea()) || descriptor.colorAttachments().size() != previous.colorAttachments().size())
            return false;

        for (int index = 0; index < descriptor.colorAttachments().size(); index ++) {
            RenderPassDescriptor.Attachment<Optional<Vector4fc>> color         = descriptor.colorAttachments().get(index);
            RenderPassDescriptor.Attachment<Optional<Vector4fc>> previousColor = previous.colorAttachments().get(index);

            if (color == null || previousColor == null) {
                if (color != previousColor)
                    return false;

                continue;
            }

            if (color.textureView() != previousColor.textureView() || color.clearValue().isPresent())
                return false;
        }

        RenderPassDescriptor.Attachment<OptionalDouble> depth         = descriptor.depthAttachment();
        RenderPassDescriptor.Attachment<OptionalDouble> previousDepth = previous.depthAttachment();

        if (depth == null || previousDepth == null)
            return depth == previousDepth;

        return depth.textureView() == previousDepth.textureView() && depth.clearValue().isEmpty();
    }

    @Unique
    private void txoptimizations$endSuspendedPass() {
        VulkanRenderPass pass = this.txoptimizations$suspendedPass;

        if (pass == null)
            return;

        VkCommandBuffer commandBuffer = this.currentCommandBuffer;

        this.txoptimizations$suspendedPass       = null;
        this.txoptimizations$suspendedDescriptor = null;

        KHRDynamicRendering.vkCmdEndRenderingKHR(commandBuffer);
        this.device.instance().debug().endDebugGroup(commandBuffer);
        this.checkpointStorage.recordCheckpoint(commandBuffer, CheckpointExtension.CheckpointType.END_RENDER_PASS, pass.getLabel());

        try (MemoryStack stack = MemoryStack.stackPush()) {
            VulkanCommandEncoder.memoryBarrier(commandBuffer, stack);
        }
    }

    @Unique
    private void txoptimizations$flushClears() {
        this.txoptimizations$endSuspendedPass();

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
        this.txoptimizations$endSuspendedPass();

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
    private static int txoptimizations$outputWidth(RenderPassDescriptor descriptor) {
        int width = 0;

        for (RenderPassDescriptor.Attachment<Optional<Vector4fc>> color : descriptor.colorAttachments()) {
            if (color != null)
                width = color.textureView().getWidth(0);
        }

        if (descriptor.colorAttachments().isEmpty() && descriptor.depthAttachment() != null)
            width = descriptor.depthAttachment().textureView().getWidth(0);

        return width;
    }

    @Unique
    private static int txoptimizations$outputHeight(RenderPassDescriptor descriptor) {
        int height = 0;

        for (RenderPassDescriptor.Attachment<Optional<Vector4fc>> color : descriptor.colorAttachments()) {
            if (color != null)
                height = color.textureView().getHeight(0);
        }

        if (descriptor.colorAttachments().isEmpty() && descriptor.depthAttachment() != null)
            height = descriptor.depthAttachment().textureView().getHeight(0);

        return height;
    }

    @Unique
    private static boolean txoptimizations$coversTexture(GpuTextureView view, GpuTexture texture, RenderPass.RenderArea area) {
        return view.texture() == texture && view.baseMipLevel() == 0 && view.mipLevels() == texture.getMipLevels() && area.fillsTexture(view);
    }
}
