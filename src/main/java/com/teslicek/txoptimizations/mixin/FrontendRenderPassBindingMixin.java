package com.teslicek.txoptimizations.mixin;

import com.mojang.renderpearl.api.buffers.GpuBuffer;
import com.mojang.renderpearl.api.buffers.GpuBufferSlice;
import com.mojang.renderpearl.api.pipeline.CompiledRenderPipeline;
import com.mojang.renderpearl.api.pipeline.IndexType;
import com.mojang.renderpearl.frontend.FrontendRenderPass;
import com.mojang.renderpearl.frontend.FrontendRenderPipeline;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FrontendRenderPass.class)
public abstract class FrontendRenderPassBindingMixin {

    @Shadow
    private FrontendRenderPipeline boundPipeline;

    @Shadow
    @Final
    private GpuBufferSlice[] vertexBuffers;

    @Shadow
    protected GpuBuffer indexBuffer;

    @Unique
    private IndexType txoptimizations$indexType;

    @Inject(method = "setPipeline", at = @At("HEAD"), cancellable = true)
    private void txoptimizations$keepBoundPipeline(CompiledRenderPipeline pipeline, CallbackInfo ci) {
        if (pipeline == this.boundPipeline)
            ci.cancel();
    }

    @Inject(method = "setVertexBuffer", at = @At("HEAD"), cancellable = true)
    private void txoptimizations$keepBoundVertexBuffer(int slot, GpuBufferSlice vertexBuffer, CallbackInfo ci) {
        if (vertexBuffer != null && slot >= 0 && slot < this.vertexBuffers.length && vertexBuffer.equals(this.vertexBuffers[slot]) && !vertexBuffer.buffer().isClosed())
            ci.cancel();
    }

    @Inject(method = "setIndexBuffer", at = @At("HEAD"), cancellable = true)
    private void txoptimizations$keepBoundIndexBuffer(GpuBuffer indexBuffer, IndexType indexType, CallbackInfo ci) {
        if (indexBuffer == this.indexBuffer && indexType == this.txoptimizations$indexType && !indexBuffer.isClosed()) {
            ci.cancel();
            return;
        }

        this.txoptimizations$indexType = indexType;
    }
}
