package com.teslicek.txoptimizations.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.blaze3d.font.GlyphProvider;
import com.teslicek.txoptimizations.FontSelection;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.IntList;
import java.util.List;
import java.util.Set;
import net.minecraft.client.gui.font.FontOption;
import net.minecraft.client.gui.font.FontSet;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(FontSet.class)
public abstract class FontSetSelectionMixin {

    @Shadow
    @Final
    private Int2ObjectMap<IntList> glyphsByWidth;

    @WrapMethod(method = "selectProviders")
    private List<GlyphProvider> txoptimizations$usePreparedSelection(List<GlyphProvider.Conditional> providers, Set<FontOption> options, Operation<List<GlyphProvider>> original) {
        FontSelection.Selection selection = FontSelection.take(providers, options);

        if (selection == null)
            return original.call(providers, options);

        this.glyphsByWidth.putAll(selection.glyphsByWidth());

        return selection.activeProviders();
    }
}
