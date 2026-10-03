package com.teslicek.txoptimizations.mixin;

import com.mojang.renderpearl.api.buffers.GpuBuffer;
import com.mojang.renderpearl.api.buffers.GpuBufferSlice;
import com.mojang.renderpearl.api.commands.RenderPassDescriptor;
import com.mojang.renderpearl.api.pipeline.ColorTargetState;
import com.mojang.renderpearl.api.pipeline.CompiledRenderPipeline;
import com.mojang.renderpearl.api.pipeline.IndexType;
import com.mojang.renderpearl.backend.api.RenderPassBackend;
import com.mojang.renderpearl.frontend.FrontendRenderPass;
import com.mojang.renderpearl.frontend.FrontendRenderPipeline;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntMaps;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.joml.Vector4fc;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
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

    @Shadow
    @Final
    private RenderPassBackend backend;

    @Shadow
    @Final
    private List<RenderPassDescriptor.Attachment<Optional<Vector4fc>>> colorAttachments;

    @Shadow
    @Final
    protected HashMap<String, Object> uniforms;

    @Shadow
    private boolean constantsPushed;

    @Unique
    private final Set<FrontendRenderPipeline> txoptimizations$validatedPipelines = new ReferenceOpenHashSet<>();

    @Unique
    private IndexType txoptimizations$indexType;

    @Overwrite
    public void setPipeline(CompiledRenderPipeline pipeline) {
        if (pipeline == this.boundPipeline)
            return;

        if (!(pipeline instanceof FrontendRenderPipeline frontendPipeline))
            throw new IllegalArgumentException("Pipeline must be instance of FrontendCompiledRenderPipeline");

        if (this.txoptimizations$validatedPipelines.add(frontendPipeline))
            this.txoptimizations$validateColorTargets(frontendPipeline);

        this.boundPipeline = frontendPipeline;
        this.backend.setPipeline(frontendPipeline.backendRenderPipeline());

        for (Object2IntMap.Entry<String> uniform : Object2IntMaps.fastIterable(frontendPipeline.uniformIndices())) {
            Object value = this.uniforms.get(uniform.getKey());

            if (value != null)
                this.backend.setUniform(uniform.getIntValue(), value);
        }

        this.constantsPushed = false;
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

    @Unique
    private void txoptimizations$validateColorTargets(FrontendRenderPipeline pipeline) {
        List<ColorTargetState> colorTargetStates = pipeline.colorTargetStates();

        if (colorTargetStates.size() != this.colorAttachments.size())
            throw new IllegalStateException("Render pass color attachment count must match pipeline color target state count.");

        for (int i = 0; i < this.colorAttachments.size(); i ++) {
            RenderPassDescriptor.Attachment<Optional<Vector4fc>> attachment = this.colorAttachments.get(i);

            if (attachment == null)
                continue;

            ColorTargetState colorTargetState = colorTargetStates.get(i);

            if (colorTargetState == null || colorTargetState.format() != attachment.textureView().texture().getFormat())
                throw new IllegalStateException("Render pass color attachment " + i + " format doesn't match pipeline format.");
        }
    }
}
