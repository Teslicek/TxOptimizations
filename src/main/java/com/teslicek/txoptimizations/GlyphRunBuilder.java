package com.teslicek.txoptimizations;

import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.mojang.renderpearl.api.textures.GpuTextureView;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.font.TextRenderable;
import net.minecraft.client.renderer.state.gui.GuiRenderState;
import net.minecraft.client.renderer.state.gui.GuiTextRenderState;

public final class GlyphRunBuilder implements Font.GlyphVisitor {

    private GuiRenderState     renderState;
    private GuiTextRenderState text;
    private GlyphRun           run;

    public void add(GuiRenderState state, GuiTextRenderState textState) {
        if (this.text != null)
            throw new IllegalStateException("Glyph runs of a text are still being built while the next text starts");

        this.renderState = state;
        this.text        = textState;

        textState.ensurePrepared().visit(this);
        this.flush();

        this.renderState = null;
        this.text        = null;
    }

    @Override
    public void acceptRenderable(TextRenderable renderable) {
        RenderPipeline pipeline    = renderable.guiPipeline();
        GpuTextureView textureView = renderable.textureView();

        if (this.run == null || !this.run.accepts(pipeline, textureView)) {
            this.flush();
            this.run = new GlyphRun(this.text.pose, this.text.scissor, pipeline, textureView);
        }

        this.run.add(renderable);
    }

    private void flush() {
        if (this.run == null)
            return;

        this.renderState.addGlyphToCurrentLayer(this.run);
        this.run = null;
    }
}
