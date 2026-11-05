package com.teslicek.txoptimizations.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.font.GlyphProvider;
import com.teslicek.txoptimizations.FontSelection;
import com.teslicek.txoptimizations.ReloadTimeline;
import com.teslicek.txoptimizations.TextLayoutCache;
import com.teslicek.txoptimizations.UnifontCache;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.gui.font.AllMissingGlyphProvider;
import net.minecraft.client.gui.font.FontManager;
import net.minecraft.client.gui.font.FontOption;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(FontManager.class)
public abstract class FontManagerMixin {

    @Inject(method = {"apply", "updateOptions"}, at = @At("TAIL"))
    private void txoptimizations$dropTextLayouts(CallbackInfo ci) {
        TextLayoutCache.clear();
    }

    @Shadow
    private static Set<FontOption> getFontOptions(Options options) {
        throw new AssertionError();
    }

    @Inject(method = "prepare", at = @At("HEAD"))
    private void txoptimizations$startFontReload(CallbackInfoReturnable<CompletableFuture<?>> cir) {
        UnifontCache.begin();
    }

    @ModifyReturnValue(method = "prepare", at = @At("RETURN"))
    private CompletableFuture<?> txoptimizations$selectProvidersOffThread(CompletableFuture<?> preparation, @Local(argsOnly = true) Executor executor) {
        return preparation.thenApplyAsync(prepared -> {
            ReloadTimeline.mark("fonts: providers loaded");
            FontSelection.prepare(((FontManagerPreparationAccessor) prepared).txoptimizations$fontSets(), getFontOptions(Minecraft.getInstance().options));
            ReloadTimeline.mark("fonts: providers selected");

            return prepared;
        }, executor);
    }

    @Inject(method = "apply", at = @At("TAIL"))
    private void txoptimizations$finishFontReload(CallbackInfo ci) {
        FontSelection.clear();
        UnifontCache.finish();
    }

    @WrapMethod(method = "finalizeProviderLoading")
    private void txoptimizations$skipLookupOnlyWarmup(List<GlyphProvider.Conditional> list, GlyphProvider.Conditional fallback, Operation<Void> original) {
        if (fallback.provider().getClass() != AllMissingGlyphProvider.class || !FontSelection.onlyLookupProviders(list)) {
            original.call(list, fallback);

            return;
        }

        list.add(0, fallback);
    }
}
