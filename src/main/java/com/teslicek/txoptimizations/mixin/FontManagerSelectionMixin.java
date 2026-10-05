package com.teslicek.txoptimizations.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.teslicek.txoptimizations.FontSelection;
import com.teslicek.txoptimizations.ReloadTimeline;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.gui.font.FontManager;
import net.minecraft.client.gui.font.FontOption;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FontManager.class)
public abstract class FontManagerSelectionMixin {

    @Shadow
    private static Set<FontOption> getFontOptions(Options options) {
        throw new AssertionError();
    }

    @ModifyReturnValue(method = "prepare", at = @At("RETURN"))
    private CompletableFuture<?> txoptimizations$selectProvidersOffThread(CompletableFuture<?> preparation, @Local(argsOnly = true) Executor executor) {
        return preparation.thenComposeAsync(prepared -> {
            ReloadTimeline.mark("fonts: providers loaded");

            return FontSelection.prepare(((FontManagerPreparationAccessor) prepared).txoptimizations$fontSets(), getFontOptions(Minecraft.getInstance().options), executor).thenApply(selected -> {
                ReloadTimeline.mark("fonts: providers selected");

                return prepared;
            });
        }, executor);
    }

    @Inject(method = "apply", at = @At("TAIL"))
    private void txoptimizations$dropSelections(CallbackInfo ci) {
        FontSelection.clear();
    }
}
