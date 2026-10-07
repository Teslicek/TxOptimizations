package com.teslicek.txoptimizations.mixin;

import com.mojang.blaze3d.platform.Window;
import java.util.Optional;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.LoadingOverlay;
import net.minecraft.server.packs.resources.ReloadInstance;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LoadingOverlay.class)
public class HideReloadOverlayMixin {

    @Shadow
    @Final
    private boolean fadeIn;

    @Shadow
    @Final
    private Minecraft minecraft;

    @Shadow
    @Final
    private ReloadInstance reload;

    @Shadow
    @Final
    private Consumer<Optional<Throwable>> onFinish;

    @Shadow
    private long fadeOutStart;

    @Inject(method = "extractRenderState", at = @At("HEAD"), cancellable = true)
    private void txoptimizations$skipReloadRender(GuiGraphicsExtractor extractor, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        if (!this.fadeIn) {
            if (this.fadeOutStart != -1L) {
                this.minecraft.gui.setOverlay(null);
                ci.cancel();
            }

            return;
        }

        if (this.minecraft.level == null && this.minecraft.gui.screen() == null)
            return;

        if (this.minecraft.level == null)
            this.minecraft.gui.screen().extractRenderStateWithTooltipAndSubtitles(extractor, mouseX, mouseY, partialTick);

        if (this.reload.isDone()) {
            try {
                this.reload.checkExceptions();
                this.onFinish.accept(Optional.empty());
            } catch (Throwable error) {
                this.onFinish.accept(Optional.of(error));
            }

            if (this.minecraft.gui.screen() != null) {
                Window window = this.minecraft.getWindow();
                this.minecraft.gui.screen().init(window.getGuiScaledWidth(), window.getGuiScaledHeight());
            }

            this.minecraft.gui.setOverlay(null);
        }

        ci.cancel();
    }
}
