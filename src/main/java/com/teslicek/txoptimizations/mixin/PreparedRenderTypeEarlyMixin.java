package com.teslicek.txoptimizations.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.systems.ScissorState;
import com.mojang.renderpearl.api.buffers.GpuBuffer;
import com.mojang.renderpearl.api.buffers.GpuBufferSlice;
import com.mojang.renderpearl.api.commands.RenderPass;
import com.mojang.renderpearl.api.pipeline.IndexType;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.teslicek.txoptimizations.GpuCubeDraw;
import com.teslicek.txoptimizations.GpuCubes;
import com.teslicek.txoptimizations.TextureList;
import com.teslicek.txoptimizations.TextureUniformBinding;
import java.util.List;
import java.util.Objects;
import net.minecraft.client.renderer.StagedVertexBuffer;
import net.minecraft.client.renderer.oit.OitPipelineSet;
import net.minecraft.client.renderer.rendertype.PreparedRenderType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(value = PreparedRenderType.class, priority = 999)
public abstract class PreparedRenderTypeEarlyMixin {

    @Unique
    private static final int PRIME = 31;

    @Shadow
    @Final
    private String name;

    @Shadow
    @Final
    private RenderPipeline pipeline;

    @Shadow
    @Final
    private OitPipelineSet oitPipelineSet;

    @Shadow
    @Final
    private GpuBufferSlice dynamicTransforms;

    @Shadow
    @Final
    private ScissorState scissorState;

    @Shadow
    @Final
    private List<PreparedRenderType.Texture> textures;

    @Overwrite
    public final boolean equals(Object other) {
        if (this == other)
            return true;

        if (!(other instanceof PreparedRenderType that))
            return false;

        return this.pipeline == that.pipeline() && Objects.equals(this.dynamicTransforms, that.dynamicTransforms()) && Objects.equals(this.scissorState, that.scissorState()) && Objects.equals(this.oitPipelineSet, that.oitPipelineSet()) && Objects.equals(this.name, that.name()) && Objects.equals(this.textures, that.textures());
    }

    @Overwrite
    public final int hashCode() {
        int hash = Objects.hashCode(this.name);

        hash = hash * PRIME + Objects.hashCode(this.pipeline);
        hash = hash * PRIME + txoptimizations$hash(this.oitPipelineSet);
        hash = hash * PRIME + Objects.hashCode(this.dynamicTransforms);
        hash = hash * PRIME + Objects.hashCode(this.scissorState);

        return hash * PRIME + Objects.hashCode(this.textures);
    }

    @Unique
    private static int txoptimizations$hash(OitPipelineSet pipelines) {
        if (pipelines == null)
            return 0;

        int hash = Objects.hashCode(pipelines.depthBoundsPipeline());

        hash = hash * PRIME + Objects.hashCode(pipelines.transmittancePipeline());

        return hash * PRIME + Objects.hashCode(pipelines.accumulatePipeline());
    }

    @Overwrite
    private void draw(StagedVertexBuffer.ExecuteInfo info, RenderPass renderPass, RenderPipeline renderPipeline) {
        renderPass.pushDebugGroup(() -> "Render Type " + this.name);

        GpuCubeDraw cubes = GpuCubes.takePending();

        if (cubes == null) {
            this.txoptimizations$bind(renderPass, renderPipeline);
            this.txoptimizations$drawVertices(info, renderPass, 0, info.indexCount());
            renderPass.popDebugGroup();

            return;
        }

        if (cubes.txoptimizations$isSorted()) {
            this.txoptimizations$drawSorted(cubes, renderPass, renderPipeline);
            renderPass.popDebugGroup();

            return;
        }

        for (int segment = 0; segment < cubes.txoptimizations$segments(); segment ++) {
            int start  = cubes.txoptimizations$segmentStart(segment);
            int length = cubes.txoptimizations$segmentLength(segment);

            int kind = cubes.txoptimizations$segmentKind(segment);

            if (kind != GpuCubeDraw.RAW) {
                this.txoptimizations$bind(renderPass, GpuCubes.variant(renderPipeline));
                GpuCubes.bindCubes(renderPass, cubes.txoptimizations$batch());
                GpuCubes.drawShapes(renderPass, kind, start, length);
                continue;
            }

            if (start % 4 != 0 || length % 4 != 0)
                throw new IllegalStateException("Vertices between GPU cubes do not form whole quads: " + length + " vertices from " + start);

            this.txoptimizations$bind(renderPass, renderPipeline);
            this.txoptimizations$drawVertices(info, renderPass, start / 4 * 6, length / 4 * 6);
        }

        renderPass.popDebugGroup();
    }

    @Unique
    private void txoptimizations$drawSorted(GpuCubeDraw cubes, RenderPass renderPass, RenderPipeline renderPipeline) {
        GpuBuffer buffer = cubes.txoptimizations$batch().sortedBuffer();
        int       mode   = cubes.txoptimizations$sortedMode();

        if (mode == 1) {
            this.txoptimizations$bind(renderPass, GpuCubes.variant(renderPipeline));
            GpuCubes.bindCubes(renderPass, cubes.txoptimizations$batch());
        } else if (mode == 2) {
            this.txoptimizations$bind(renderPass, renderPipeline);
            renderPass.setVertexBuffer(0, buffer.slice(cubes.txoptimizations$vertexOffset(), cubes.txoptimizations$vertexBytes()));
        } else {
            throw new IllegalStateException("A sorted draw with GPU cubes was not sorted before drawing");
        }

        renderPass.setIndexBuffer(buffer, IndexType.INT);
        renderPass.drawIndexed(cubes.txoptimizations$indexCount(), 1, cubes.txoptimizations$firstIndex(), 0, 0);
    }

    @Unique
    private void txoptimizations$drawVertices(StagedVertexBuffer.ExecuteInfo info, RenderPass renderPass, int firstIndex, int indexCount) {
        GpuBuffer indexBuffer = info.indexBuffer();

        renderPass.setVertexBuffer(0, info.vertexBuffer().slice());
        renderPass.setIndexBuffer(indexBuffer, info.indexType());
        renderPass.drawIndexed(indexCount, 1, info.firstIndex() + firstIndex, info.baseVertex(), 0);
    }

    @Unique
    private void txoptimizations$bind(RenderPass renderPass, RenderPipeline renderPipeline) {
        renderPass.setPipeline(RenderSystem.getCompiledPipeline(renderPipeline));

        if (this.scissorState.enabled())
            renderPass.enableScissor(this.scissorState.x(), this.scissorState.y(), this.scissorState.width(), this.scissorState.height());

        RenderSystem.bindDefaultUniforms(renderPass);
        renderPass.setUniform("DynamicTransforms", this.dynamicTransforms);

        if (this.textures instanceof TextureList list) {
            TextureUniformBinding binding = (TextureUniformBinding) renderPass;

            for (int index = 0; index < list.size(); index ++)
                binding.txoptimizations$setTexture(list.get(index).name(), list.binding(index));
        } else {
            for (int index = 0; index < this.textures.size(); index ++) {
                PreparedRenderType.Texture texture = this.textures.get(index);

                renderPass.setUniform(texture.name(), texture.textureView(), texture.sampler());
            }
        }
    }
}
