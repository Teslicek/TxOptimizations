package com.teslicek.txoptimizations.mixin;

import com.mojang.renderpearl.api.buffers.GpuBufferSlice;
import com.mojang.renderpearl.backend.vulkan.VulkanCommandEncoder;
import com.mojang.renderpearl.backend.vulkan.VulkanGpuBuffer;
import com.teslicek.txoptimizations.PreciseBarriers;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import java.nio.ByteBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkCommandBuffer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(VulkanCommandEncoder.class)
public abstract class VulkanCommandEncoderUploadBarrierMixin {

    @Shadow
    private VkCommandBuffer currentCommandBuffer;

    @Unique
    private final LongArrayList txoptimizations$pendingRanges = new LongArrayList();

    @Unique
    private boolean txoptimizations$barrierPending;

    @Unique
    private int txoptimizations$deferDepth;

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

    @Inject(method = "writeToBuffer", at = @At("RETURN"))
    private void txoptimizations$endUpload(GpuBufferSlice destination, ByteBuffer data, CallbackInfo ci) {
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

    @Inject(method = "copyToBuffer", at = @At("HEAD"))
    private void txoptimizations$beginCopy(GpuBufferSlice source, GpuBufferSlice target, CallbackInfo ci) {
        long sourceBuffer = ((VulkanGpuBuffer) source.buffer()).vkBuffer();
        long targetBuffer = ((VulkanGpuBuffer) target.buffer()).vkBuffer();
        long targetStart  = target.offset();
        long targetEnd    = targetStart + source.length();

        if (this.txoptimizations$overlapsPending(sourceBuffer, source.offset(), source.offset() + source.length()) || this.txoptimizations$overlapsPending(targetBuffer, targetStart, targetEnd))
            this.txoptimizations$flushBarrier();

        this.txoptimizations$uploadBuffer = targetBuffer;
        this.txoptimizations$uploadStart  = targetStart;
        this.txoptimizations$uploadEnd    = targetEnd;
        this.txoptimizations$readBuffer   = sourceBuffer;
        this.txoptimizations$readStart    = source.offset();
        this.txoptimizations$readEnd      = source.offset() + source.length();
        this.txoptimizations$deferDepth ++;
    }

    @Inject(method = "copyToBuffer", at = @At("RETURN"))
    private void txoptimizations$endCopy(GpuBufferSlice source, GpuBufferSlice target, CallbackInfo ci) {
        this.txoptimizations$deferDepth --;
    }

    @Inject(method = "writeTimestamp", at = @At("HEAD"))
    private void txoptimizations$beginTimestamp(CallbackInfo ci) {
        this.txoptimizations$deferDepth ++;
    }

    @Inject(method = "writeTimestamp", at = @At("RETURN"))
    private void txoptimizations$endTimestamp(CallbackInfo ci) {
        this.txoptimizations$deferDepth --;
    }

    @Inject(method = "commandBuffer", at = @At("HEAD"))
    private void txoptimizations$flushBeforeCommand(CallbackInfoReturnable<VkCommandBuffer> cir) {
        if (this.txoptimizations$deferDepth == 0)
            this.txoptimizations$flushBarrier();
    }

    @Inject(method = "endCommandBuffer", at = @At("HEAD"))
    private void txoptimizations$flushBeforeEnd(CallbackInfo ci) {
        this.txoptimizations$flushBarrier();
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
    private void txoptimizations$flushBarrier() {
        if (!this.txoptimizations$barrierPending)
            return;

        if (this.currentCommandBuffer == null)
            throw new IllegalStateException("Upload barrier pending without a recording command buffer");

        this.txoptimizations$barrierPending = false;
        this.txoptimizations$pendingRanges.clear();

        PreciseBarriers.afterUpload(this.currentCommandBuffer);
    }
}
