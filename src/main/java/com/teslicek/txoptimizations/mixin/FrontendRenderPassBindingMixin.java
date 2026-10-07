package com.teslicek.txoptimizations.mixin;

import com.mojang.renderpearl.api.buffers.GpuBuffer;
import com.mojang.renderpearl.api.buffers.GpuBufferSlice;
import com.mojang.renderpearl.api.commands.RenderPassDescriptor;
import com.mojang.renderpearl.api.pipeline.ColorTargetState;
import com.mojang.renderpearl.api.pipeline.CompiledRenderPipeline;
import com.mojang.renderpearl.api.pipeline.IndexType;
import com.mojang.renderpearl.api.textures.GpuSampler;
import com.mojang.renderpearl.api.textures.GpuTextureView;
import com.mojang.renderpearl.backend.api.RenderPassBackend;
import com.mojang.renderpearl.frontend.FrontendRenderPass;
import com.mojang.renderpearl.frontend.FrontendRenderPipeline;
import com.mojang.renderpearl.util.TextureViewAndSampler;
import com.teslicek.txoptimizations.PipelinePassStamp;
import com.teslicek.txoptimizations.PipelineUniforms;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
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
    private static long txoptimizations$passes;

    @Unique
    private static final int VERTEX_BUFFER_SLOTS = 16;

    @Unique
    private IndexType txoptimizations$indexType;

    @Unique
    private long txoptimizations$pass;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void txoptimizations$numberPass(CallbackInfo ci) {
        txoptimizations$passes ++;
        this.txoptimizations$pass = txoptimizations$passes;
    }

    @Overwrite
    public void setPipeline(CompiledRenderPipeline pipeline) {
        if (pipeline == this.boundPipeline)
            return;

        if (!(pipeline instanceof FrontendRenderPipeline frontendPipeline))
            throw new IllegalArgumentException("Pipeline must be instance of FrontendCompiledRenderPipeline");

        PipelinePassStamp stamp = (PipelinePassStamp) (Object) frontendPipeline;

        if (stamp.txoptimizations$getValidatedPass() != this.txoptimizations$pass) {
            stamp.txoptimizations$setValidatedPass(this.txoptimizations$pass);
            this.txoptimizations$validateColorTargets(frontendPipeline);
        }

        this.boundPipeline = frontendPipeline;
        this.backend.setPipeline(frontendPipeline.backendRenderPipeline());

        PipelineUniforms layout = (PipelineUniforms) (Object) frontendPipeline;
        String[]         names  = layout.txoptimizations$uniformNames();
        int[]            slots  = layout.txoptimizations$uniformSlots();

        for (int index = 0; index < names.length; index ++) {
            Object value = this.uniforms.get(names[index]);

            if (value != null)
                this.backend.setUniform(slots[index], value);
        }

        this.constantsPushed = false;
    }

    @Overwrite
    public void setUniform(String name, GpuTextureView textureView, GpuSampler sampler) {
        if (textureView != null && sampler != null) {
            if (this.uniforms.get(name) instanceof TextureViewAndSampler bound && bound.view() == textureView && bound.sampler() == sampler)
                return;

            this.setUniform(name, new TextureViewAndSampler(textureView, sampler));

            return;
        }

        if (textureView != null || sampler != null)
            throw new IllegalArgumentException("textureView and sampler must both or neither be null");

        this.setUniform(name, (Object) null);
    }

    @Overwrite
    private void setUniform(String name, Object value) {
        Object previous = value == null ? this.uniforms.remove(name) : this.uniforms.put(name, value);

        if (value != null && value.equals(previous))
            return;

        if (this.boundPipeline == null)
            return;

        int uniformIndex = ((PipelineUniforms) (Object) this.boundPipeline).txoptimizations$uniformSlot(name);

        if (uniformIndex != -1)
            this.backend.setUniform(uniformIndex, value);
    }

    @Overwrite
    public void setVertexBuffer(int slot, GpuBufferSlice vertexBuffer) {
        if (vertexBuffer != null && slot >= 0 && slot < this.vertexBuffers.length && vertexBuffer.equals(this.vertexBuffers[slot]) && !vertexBuffer.buffer().isClosed())
            return;

        if (slot < 0 || slot >= VERTEX_BUFFER_SLOTS)
            throw new IllegalArgumentException("Vertex buffer slot is out of range: " + slot);

        if (vertexBuffer != null && vertexBuffer.buffer().isClosed())
            throw new IllegalStateException("Vertex buffer at slot " + slot + " has been closed!");

        if (vertexBuffer != null && (vertexBuffer.buffer().usage() & GpuBuffer.USAGE_VERTEX) == 0)
            throw new IllegalStateException("Vertex buffer at slot " + slot + " doesn't have GpuBuffer.USAGE_VERTEX flag!");

        this.vertexBuffers[slot] = vertexBuffer;
        this.backend.setVertexBuffer(slot, vertexBuffer);
    }

    @Overwrite
    public void setIndexBuffer(GpuBuffer indexBuffer, IndexType indexType) {
        if (indexBuffer != null && indexBuffer == this.indexBuffer && indexType == this.txoptimizations$indexType && !indexBuffer.isClosed())
            return;

        this.txoptimizations$indexType = indexType;
        this.indexBuffer               = indexBuffer;
        this.backend.setIndexBuffer(indexBuffer, indexType);
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
