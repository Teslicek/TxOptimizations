package com.teslicek.txoptimizations.mixin.hud;

import com.teslicek.txoptimizations.hud.HudCache;
import net.minecraft.client.renderer.state.gui.BlitRenderState;
import net.minecraft.client.renderer.state.gui.GuiElementRenderState;
import net.minecraft.client.renderer.state.gui.GuiItemRenderState;
import net.minecraft.client.renderer.state.gui.GuiRenderState;
import net.minecraft.client.renderer.state.gui.GuiTextRenderState;
import net.minecraft.client.renderer.state.gui.ScreenArea;
import net.minecraft.client.renderer.state.gui.pip.PictureInPictureRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiRenderState.class)
public abstract class GuiRenderStateHudCacheMixin {

    @Inject(method = "addText", at = @At("HEAD"), cancellable = true)
    private void txoptimizations$deferText(GuiTextRenderState text, CallbackInfo ci) {
        this.txoptimizations$deferUncached(text, ci);
    }

    @Inject(method = "addPicturesInPictureState", at = @At("HEAD"), cancellable = true)
    private void txoptimizations$deferPicture(PictureInPictureRenderState picture, CallbackInfo ci) {
        this.txoptimizations$deferUncached(picture, ci);
    }

    @Inject(method = "addItem", at = @At("HEAD"), cancellable = true)
    private void txoptimizations$deferItem(GuiItemRenderState item, CallbackInfo ci) {
        this.txoptimizations$deferUncached(item, ci);
    }

    @Inject(method = "addGuiElement", at = @At("HEAD"), cancellable = true)
    private void txoptimizations$deferElement(GuiElementRenderState element, CallbackInfo ci) {
        if (!HudCache.isRendering())
            return;

        if (!HudCache.isCurrentUncached()) {
            HudCache.checkBlend(element.pipeline());
            HudCache.include(element.bounds());

            return;
        }

        HudCache.defer(element);
        ci.cancel();
    }

    @Inject(method = "addBlitToCurrentLayer", at = @At("HEAD"))
    private void txoptimizations$includeBlit(BlitRenderState blit, CallbackInfo ci) {
        if (HudCache.isRendering())
            HudCache.include(blit.bounds());
    }

    @Unique
    private void txoptimizations$deferUncached(ScreenArea submission, CallbackInfo ci) {
        if (!HudCache.isRendering())
            return;

        if (!HudCache.isCurrentUncached()) {
            HudCache.include(submission.bounds());

            return;
        }

        HudCache.defer(submission);
        ci.cancel();
    }
}
