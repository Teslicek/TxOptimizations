package com.teslicek.txoptimizations.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.blaze3d.font.GlyphProvider;
import com.teslicek.txoptimizations.FontSelection;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntList;
import java.util.List;
import java.util.Set;
import net.minecraft.client.gui.font.FontOption;
import net.minecraft.client.gui.font.FontSet;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(FontSet.class)
public abstract class FontSetSelectionMixin {

    @Shadow
    @Final
    private Int2ObjectMap<IntList> glyphsByWidth;

    @Unique
    private List<GlyphProvider.Conditional> txoptimizations$selectedFrom;

    @Unique
    private Set<FontOption> txoptimizations$selectedFor;

    @Unique
    private List<GlyphProvider> txoptimizations$selected;

    @Unique
    private Int2ObjectMap<IntList> txoptimizations$selectedWidths;

    @WrapMethod(method = "selectProviders")
    private List<GlyphProvider> txoptimizations$usePreparedSelection(List<GlyphProvider.Conditional> providers, Set<FontOption> options, Operation<List<GlyphProvider>> original) {
        if (providers.equals(this.txoptimizations$selectedFrom) && options.equals(this.txoptimizations$selectedFor)) {
            this.glyphsByWidth.putAll(this.txoptimizations$selectedWidths);

            return this.txoptimizations$selected;
        }

        FontSelection.Selection selection = FontSelection.take(providers, options);
        List<GlyphProvider>     selected;

        if (selection == null) {
            selected = original.call(providers, options);
        } else {
            this.glyphsByWidth.putAll(selection.glyphsByWidth());
            selected = selection.activeProviders();
        }

        this.txoptimizations$selectedFrom   = List.copyOf(providers);
        this.txoptimizations$selectedFor    = Set.copyOf(options);
        this.txoptimizations$selected       = selected;
        this.txoptimizations$selectedWidths = new Int2ObjectOpenHashMap<>(this.glyphsByWidth);

        return selected;
    }
}
