package com.teslicek.txoptimizations.mixin;

import com.mojang.renderpearl.api.commands.RenderPass;
import com.mojang.renderpearl.api.commands.RenderPassDescriptor;
import com.mojang.renderpearl.api.textures.GpuTexture;
import com.mojang.renderpearl.api.textures.GpuTextureView;
import com.mojang.renderpearl.backend.vulkan.VulkanCommandEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;
import org.joml.Vector4fc;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkCommandBuffer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(VulkanCommandEncoder.class)
public abstract class VulkanCommandEncoderDeferredClearMixin {

    @Unique
    private GpuTexture txoptimizations$clearColorTexture;

    @Unique
    private Vector4fc txoptimizations$clearColor;

    @Unique
    private GpuTexture txoptimizations$clearDepthTexture;

    @Unique
    private double txoptimizations$clearDepth;

    @Unique
    private int txoptimizations$keepDepth;

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

    @Inject(method = {"writeTimestamp", "writeToBuffer", "copyToBuffer"}, at = @At("HEAD"))
    private void txoptimizations$keepClears(CallbackInfo ci) {
        this.txoptimizations$keepDepth ++;
    }

    @Inject(method = {"writeTimestamp", "writeToBuffer", "copyToBuffer"}, at = @At("RETURN"))
    private void txoptimizations$releaseClears(CallbackInfo ci) {
        this.txoptimizations$keepDepth --;
    }

    @Inject(method = "commandBuffer", at = @At("HEAD"))
    private void txoptimizations$flushBeforeCommand(CallbackInfoReturnable<VkCommandBuffer> cir) {
        if (this.txoptimizations$keepDepth == 0)
            this.txoptimizations$flushClears();
    }

    @Inject(method = "endCommandBuffer", at = @At("HEAD"))
    private void txoptimizations$flushBeforeEnd(CallbackInfo ci) {
        this.txoptimizations$flushClears();
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
    private static boolean txoptimizations$coversTexture(GpuTextureView view, GpuTexture texture, RenderPass.RenderArea area) {
        return view.texture() == texture && view.baseMipLevel() == 0 && view.mipLevels() == texture.getMipLevels() && area.fillsTexture(view);
    }
}
