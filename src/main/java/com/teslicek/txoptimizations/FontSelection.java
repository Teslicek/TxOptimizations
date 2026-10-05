package com.teslicek.txoptimizations;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.mojang.blaze3d.font.GlyphProvider;
import com.mojang.blaze3d.font.UnbakedGlyph;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.client.gui.font.FontOption;
import net.minecraft.client.gui.font.glyphs.SpecialGlyphs;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

public final class FontSelection {

    private static volatile Pending pending;

    private FontSelection() {
    }

    public static void prepare(Map<Identifier, List<GlyphProvider.Conditional>> fontSets, Set<FontOption> options) {
        List<Entry>                entries   = new ArrayList<>(fontSets.size());
        Map<GlyphProvider, IntSet> supported = new IdentityHashMap<>();

        for (List<GlyphProvider.Conditional> providers : fontSets.values()) {
            List<GlyphProvider.Conditional> ordered = List.copyOf(Lists.reverse(providers));

            entries.add(new Entry(ordered, select(ordered, options, supported)));
        }

        pending = new Pending(Set.copyOf(options), entries);
    }

    public static Selection take(List<GlyphProvider.Conditional> providers, Set<FontOption> options) {
        Pending current = pending;

        if (current == null || !current.options().equals(options))
            return null;

        for (Entry entry : current.entries()) {
            if (sameProviders(entry.providers(), providers))
                return entry.selection();
        }

        return null;
    }

    public static void clear() {
        pending = null;
    }

    private static Selection select(List<GlyphProvider.Conditional> providers, Set<FontOption> options, Map<GlyphProvider, IntSet> supported) {
        Int2ObjectMap<IntList> glyphsByWidth     = new Int2ObjectOpenHashMap<>();
        IntSet                 supportedGlyphs   = new IntOpenHashSet();
        List<GlyphProvider>    selectedProviders = new ArrayList<>();

        for (GlyphProvider.Conditional conditionalProvider : providers) {
            if (conditionalProvider.filter().apply(options)) {
                selectedProviders.add(conditionalProvider.provider());
                supportedGlyphs.addAll(supported.computeIfAbsent(conditionalProvider.provider(), GlyphProvider::getSupportedGlyphs));
            }
        }

        Set<GlyphProvider> usedProviders = Sets.newHashSet();

        supportedGlyphs.forEach(codepoint -> {
            for (GlyphProvider provider : selectedProviders) {
                UnbakedGlyph glyph = provider.getGlyph(codepoint);

                if (glyph != null) {
                    usedProviders.add(provider);

                    if (glyph.info() != SpecialGlyphs.MISSING)
                        glyphsByWidth.computeIfAbsent(Mth.ceil(glyph.info().getAdvance(false)), width -> new IntArrayList()).add(codepoint);

                    break;
                }
            }
        });

        return new Selection(selectedProviders.stream().filter(usedProviders::contains).toList(), glyphsByWidth);
    }

    private static boolean sameProviders(List<GlyphProvider.Conditional> expected, List<GlyphProvider.Conditional> actual) {
        if (expected.size() != actual.size())
            return false;

        for (int index = 0; index < expected.size(); index ++) {
            GlyphProvider.Conditional left  = expected.get(index);
            GlyphProvider.Conditional right = actual.get(index);

            if (left.provider() != right.provider() || left.filter() != right.filter())
                return false;
        }

        return true;
    }

    public record Selection(List<GlyphProvider> activeProviders, Int2ObjectMap<IntList> glyphsByWidth) {
    }

    private record Entry(List<GlyphProvider.Conditional> providers, Selection selection) {
    }

    private record Pending(Set<FontOption> options, List<Entry> entries) {
    }
}
