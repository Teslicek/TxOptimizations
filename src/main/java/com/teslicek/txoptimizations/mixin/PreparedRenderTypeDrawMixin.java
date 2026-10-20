package com.teslicek.txoptimizations.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.systems.ScissorState;
import com.mojang.renderpearl.api.buffers.GpuBuffer;
import com.mojang.renderpearl.api.buffers.GpuBufferSlice;
import com.mojang.renderpearl.api.commands.RenderPass;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.teslicek.txoptimizations.TextureList;
import com.teslicek.txoptimizations.TextureUniformBinding;
import java.util.List;
import net.minecraft.client.renderer.StagedVertexBuffer;
import net.minecraft.client.renderer.rendertype.PreparedRenderType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = PreparedRenderType.class, priority = 999)
public abstract class PreparedRenderTypeDrawMixin {

    @Shadow
    @Final
    private String name;

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
    private void draw(StagedVertexBuffer.ExecuteInfo info, RenderPass renderPass, RenderPipeline renderPipeline) {
        renderPass.pushDebugGroup(() -> "Render Type " + this.name);

        GpuBuffer indexBuffer = info.indexBuffer();

        renderPass.setPipeline(RenderSystem.getCompiledPipeline(renderPipeline));

        if (this.scissorState.enabled())
            renderPass.enableScissor(this.scissorState.x(), this.scissorState.y(), this.scissorState.width(), this.scissorState.height());

        RenderSystem.bindDefaultUniforms(renderPass);
        renderPass.setUniform("DynamicTransforms", this.dynamicTransforms);
        renderPass.setVertexBuffer(0, info.vertexBuffer().slice());

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

        renderPass.setIndexBuffer(indexBuffer, info.indexType());
        renderPass.drawIndexed(info.indexCount(), 1, info.firstIndex(), info.baseVertex(), 0);
        renderPass.popDebugGroup();
    }
}
