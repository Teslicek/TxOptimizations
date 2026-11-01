package com.teslicek.txoptimizations;

import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.mojang.renderpearl.api.textures.GpuTextureView;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.font.TextRenderable;
import net.minecraft.client.gui.font.glyphs.BakedSheetGlyph;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import org.jspecify.annotations.Nullable;

public final class SegmentedText implements Font.PreparedText {

    private final Font.PreparedText prepared;
    private Segment[]               segments;
    private boolean                 segmented;

    SegmentedText(Font.PreparedText prepared) {
        this.prepared = prepared;
    }

    @Override
    public void visit(Font.GlyphVisitor visitor) {
        this.prepared.visit(visitor);
    }

    @Override
    public @Nullable ScreenRectangle bounds() {
        return this.prepared.bounds();
    }

    public Segment @Nullable [] ensureSegments() {
        if (this.segmented)
            return this.segments;

        Segmenter segmenter = new Segmenter();

        this.prepared.visit(segmenter);

        this.segments  = segmenter.finish();
        this.segmented = true;

        return this.segments;
    }

    public record Segment(RenderPipeline pipeline, GpuTextureView textureView, TextRenderable[] glyphs) {
    }

    private static final class Segmenter implements Font.GlyphVisitor {

        private final List<Segment>        segments = new ArrayList<>();
        private final List<TextRenderable> glyphs   = new ArrayList<>();
        private RenderPipeline             pipeline;
        private GpuTextureView             textureView;
        private boolean                    stable   = true;

        @Override
        public void acceptRenderable(TextRenderable renderable) {
            if (!this.stable)
                return;

            if (!(renderable instanceof BakedSheetGlyph.GlyphInstance) && !(renderable instanceof BakedSheetGlyph.EffectInstance)) {
                this.stable = false;
                return;
            }

            RenderPipeline pipeline    = renderable.guiPipeline();
            GpuTextureView textureView = renderable.textureView();

            if (pipeline != this.pipeline || textureView != this.textureView) {
                this.flush();
                this.pipeline    = pipeline;
                this.textureView = textureView;
            }

            this.glyphs.add(renderable);
        }

        private Segment @Nullable [] finish() {
            if (!this.stable)
                return null;

            this.flush();

            return this.segments.toArray(new Segment[0]);
        }

        private void flush() {
            if (this.glyphs.isEmpty())
                return;

            this.segments.add(new Segment(this.pipeline, this.textureView, this.glyphs.toArray(new TextRenderable[0])));
            this.glyphs.clear();
        }
    }
}
