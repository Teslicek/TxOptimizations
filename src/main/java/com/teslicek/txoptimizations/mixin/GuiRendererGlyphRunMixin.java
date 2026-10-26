package com.teslicek.txoptimizations.mixin;

import com.teslicek.txoptimizations.GlyphRunBuilder;
import net.minecraft.client.gui.render.GuiRenderer;
import net.minecraft.client.renderer.state.gui.GuiRenderState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(GuiRenderer.class)
public abstract class GuiRendererGlyphRunMixin {

    @Unique
    private final GlyphRunBuilder txoptimizations$glyphRuns = new GlyphRunBuilder();

    @Shadow
    @Final
    private GuiRenderState renderState;

    @Overwrite
    private void prepareText() {
        this.renderState.forEachText(text -> this.txoptimizations$glyphRuns.add(this.renderState, text));
    }
}
