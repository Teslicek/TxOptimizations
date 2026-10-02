package com.teslicek.txoptimizations.hud;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.renderpearl.api.GpuFormat;
import com.mojang.renderpearl.api.pipeline.BlendFactor;
import com.mojang.renderpearl.api.pipeline.BlendFunction;
import com.mojang.renderpearl.api.pipeline.ColorTargetState;
import com.mojang.renderpearl.api.pipeline.CompareOp;
import com.mojang.renderpearl.api.pipeline.DepthStencilState;
import com.mojang.renderpearl.api.pipeline.PrimitiveTopology;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.mojang.renderpearl.api.textures.FilterMode;
import java.util.List;
import java.util.Optional;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.BindGroupLayouts;
import net.minecraft.client.renderer.state.gui.GuiElementRenderState;
import net.minecraft.resources.Identifier;

record HudBlit(RenderTarget target, List<ScreenRectangle> areas, ScreenRectangle bounds) implements GuiElementRenderState {

    private static final int COLOR_WRITE_MASK = 7;

    private static final RenderPipeline PIPELINE = RenderPipeline.builder()
        .withLocation(Identifier.fromNamespaceAndPath("txoptimizations", "hud_blit"))
        .withBindGroupLayout(BindGroupLayouts.DYNAMIC_TRANSFORMS)
        .withBindGroupLayout(BindGroupLayouts.GLOBALS)
        .withBindGroupLayout(BindGroupLayouts.SAMPLER0)
        .withBindGroupLayout(BindGroupLayouts.PROJECTION)
        .withVertexBinding(0, DefaultVertexFormat.POSITION_TEX)
        .withColorTargetState(new ColorTargetState(Optional.of(new BlendFunction(BlendFactor.ONE, BlendFactor.ONE_MINUS_SRC_ALPHA, BlendFactor.ONE, BlendFactor.ONE_MINUS_SRC_ALPHA)), GpuFormat.RGBA8_UNORM, COLOR_WRITE_MASK))
        .withDepthStencilState(new DepthStencilState(CompareOp.ALWAYS_PASS, false))
        .withPrimitiveTopology(PrimitiveTopology.TRIANGLES)
        .withVertexShader(Identifier.fromNamespaceAndPath("txoptimizations", "shaderc/hud_rect"))
        .withFragmentShader(Identifier.fromNamespaceAndPath("txoptimizations", "shaderc/hud_blit"))
        .build();

    @Override
    public void buildVertices(VertexConsumer vertices) {
        for (ScreenRectangle area : this.areas) {
            float left   = area.left();
            float top    = area.top();
            float right  = area.right();
            float bottom = area.bottom();

            vertices.addVertex(left, top, 0.0F).setUv(0.0F, 0.0F);
            vertices.addVertex(left, bottom, 0.0F).setUv(0.0F, 0.0F);
            vertices.addVertex(right, bottom, 0.0F).setUv(0.0F, 0.0F);
            vertices.addVertex(right, bottom, 0.0F).setUv(0.0F, 0.0F);
            vertices.addVertex(right, top, 0.0F).setUv(0.0F, 0.0F);
            vertices.addVertex(left, top, 0.0F).setUv(0.0F, 0.0F);
        }
    }

    @Override
    public RenderPipeline pipeline() {
        return PIPELINE;
    }

    @Override
    public TextureSetup textureSetup() {
        return TextureSetup.singleTexture(this.target.getColorTextureView(), RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST));
    }

    @Override
    public ScreenRectangle scissorArea() {
        return null;
    }

}
