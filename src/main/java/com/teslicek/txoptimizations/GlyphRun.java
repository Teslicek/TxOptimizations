package com.teslicek.txoptimizations;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.mojang.renderpearl.api.textures.FilterMode;
import com.mojang.renderpearl.api.textures.GpuTextureView;
import java.util.Arrays;
import net.minecraft.client.gui.font.TextRenderable;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.state.gui.GuiElementRenderState;
import org.joml.Matrix3x2fc;
import org.joml.Matrix4f;
import org.jspecify.annotations.Nullable;

public final class GlyphRun implements GuiElementRenderState {

    private static final int      FULL_BRIGHT    = 15728880;
    private static final int      INITIAL_GLYPHS = 16;
    private static final Matrix4f POSE           = new Matrix4f();

    private final Matrix3x2fc     pose;
    private final ScreenRectangle scissor;
    private final RenderPipeline  pipeline;
    private final GpuTextureView  textureView;
    private TextRenderable[]      glyphs = new TextRenderable[INITIAL_GLYPHS];
    private int                   count;

    GlyphRun(Matrix3x2fc pose, @Nullable ScreenRectangle scissor, RenderPipeline pipeline, GpuTextureView textureView) {
        this.pose        = pose;
        this.scissor     = scissor;
        this.pipeline    = pipeline;
        this.textureView = textureView;
    }

    boolean accepts(RenderPipeline glyphPipeline, GpuTextureView glyphTexture) {
        return glyphPipeline == this.pipeline && glyphTexture == this.textureView;
    }

    void add(TextRenderable glyph) {
        if (this.count == this.glyphs.length)
            this.glyphs = Arrays.copyOf(this.glyphs, this.count * 2);

        this.glyphs[this.count ++] = glyph;
    }

    @Override
    public void buildVertices(VertexConsumer vertexConsumer) {
        Matrix4f pose = POSE.identity().mul(this.pose);

        for (int index = 0; index < this.count; index ++)
            this.glyphs[index].render(pose, vertexConsumer, FULL_BRIGHT, true);
    }

    @Override
    public RenderPipeline pipeline() {
        return this.pipeline;
    }

    @Override
    public TextureSetup textureSetup() {
        return TextureSetup.singleTextureWithLightmap(this.textureView, RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST));
    }

    @Override
    public @Nullable ScreenRectangle scissorArea() {
        return this.scissor;
    }

    @Override
    public @Nullable ScreenRectangle bounds() {
        return null;
    }
}
