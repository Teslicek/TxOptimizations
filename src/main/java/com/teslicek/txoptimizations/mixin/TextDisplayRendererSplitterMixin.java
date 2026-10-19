package com.teslicek.txoptimizations.mixin;

import net.minecraft.client.renderer.entity.DisplayRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.TextDisplayEntityRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Display;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(DisplayRenderer.TextDisplayRenderer.class)
public abstract class TextDisplayRendererSplitterMixin extends DisplayRenderer<Display.TextDisplay, Display.TextDisplay.TextRenderState, TextDisplayEntityRenderState> {

    @Unique
    private final Display.TextDisplay.LineSplitter txoptimizations$splitter = this::splitLines;

    protected TextDisplayRendererSplitterMixin(EntityRendererProvider.Context context) {
        super(context);
    }

    @Shadow
    private Display.TextDisplay.CachedInfo splitLines(Component input, int width) {
        throw new AssertionError();
    }

    @Overwrite
    public void extractRenderState(Display.TextDisplay entity, TextDisplayEntityRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.textRenderState = entity.textRenderState();
        state.cachedInfo      = entity.cacheDisplay(this.txoptimizations$splitter);
    }
}
