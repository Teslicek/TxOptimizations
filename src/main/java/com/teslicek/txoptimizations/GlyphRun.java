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
import org.jspecify.annotations.Nullable;

public final class GlyphRun implements GuiElementRenderState {

    private static final int INITIAL_GLYPHS = 16;

    private final Matrix3x2fc     pose;
    private final ScreenRectangle scissor;
    private final RenderPipeline  pipeline;
    private final GpuTextureView  textureView;
    private final TextureSetup    textureSetup;
    private final boolean         shared;
    private TextRenderable[]      glyphs;
    private int                   count;

    GlyphRun(Matrix3x2fc pose, @Nullable ScreenRectangle scissor, RenderPipeline pipeline, GpuTextureView textureView) {
        this(pose, scissor, pipeline, textureView, new TextRenderable[INITIAL_GLYPHS], 0, false);
    }

    GlyphRun(Matrix3x2fc pose, @Nullable ScreenRectangle scissor, SegmentedText.Segment segment) {
        this(pose, scissor, segment.pipeline(), segment.textureView(), segment.glyphs(), segment.glyphs().length, true);
    }

    private GlyphRun(Matrix3x2fc pose, @Nullable ScreenRectangle scissor, RenderPipeline pipeline, GpuTextureView textureView, TextRenderable[] glyphs, int count, boolean shared) {
        this.pose         = pose;
        this.scissor      = scissor;
        this.pipeline     = pipeline;
        this.textureView  = textureView;
        this.textureSetup = TextureSetup.singleTextureWithLightmap(textureView, RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST));
        this.shared       = shared;
        this.glyphs       = glyphs;
        this.count        = count;
    }

    boolean accepts(RenderPipeline glyphPipeline, GpuTextureView glyphTexture) {
        return glyphPipeline == this.pipeline && glyphTexture == this.textureView;
    }

    void add(TextRenderable glyph) {
        if (this.shared)
            throw new IllegalStateException("A glyph run of a segmented text cannot take more glyphs");

        if (this.count == this.glyphs.length)
            this.glyphs = Arrays.copyOf(this.glyphs, this.count * 2);

        this.glyphs[this.count ++] = glyph;
    }

    @Override
    public void buildVertices(VertexConsumer vertexConsumer) {
        GlyphVertexCache.build(this.glyphs, this.count, this.shared, this.pose, vertexConsumer);
    }

    @Override
    public RenderPipeline pipeline() {
        return this.pipeline;
    }

    @Override
    public TextureSetup textureSetup() {
        return this.textureSetup;
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
