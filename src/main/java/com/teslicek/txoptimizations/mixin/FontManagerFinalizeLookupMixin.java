package com.teslicek.txoptimizations.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.blaze3d.font.GlyphProvider;
import com.teslicek.txoptimizations.FontSelection;
import java.util.List;
import net.minecraft.client.gui.font.AllMissingGlyphProvider;
import net.minecraft.client.gui.font.FontManager;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(FontManager.class)
public abstract class FontManagerFinalizeLookupMixin {

    @WrapMethod(method = "finalizeProviderLoading")
    private void txoptimizations$skipLookupOnlyWarmup(List<GlyphProvider.Conditional> list, GlyphProvider.Conditional fallback, Operation<Void> original) {
        if (fallback.provider().getClass() != AllMissingGlyphProvider.class || !FontSelection.onlyLookupProviders(list)) {
            original.call(list, fallback);

            return;
        }

        list.add(0, fallback);
    }
}
