package com.teslicek.txoptimizations;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.mojang.blaze3d.font.GlyphProvider;
import com.mojang.blaze3d.font.SpaceProvider;
import com.mojang.blaze3d.font.TrueTypeGlyphProvider;
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
import net.minecraft.client.gui.font.providers.BitmapProvider;
import net.minecraft.client.gui.font.providers.UnihexProvider;
import net.minecraft.client.gui.font.glyphs.SpecialGlyphs;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import org.jspecify.annotations.Nullable;

public final class FontSelection {

    private static final Set<Class<?>> SUPPORTED_SET_PROVIDERS = Set.of(BitmapProvider.class, UnihexProvider.class, SpaceProvider.class, TrueTypeGlyphProvider.class);

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

        Set<GlyphProvider>           usedProviders = Sets.newHashSet();
        Int2ObjectMap<GlyphProvider> owners        = ownersBySupportedGlyphs(selectedProviders, supported);

        supportedGlyphs.forEach(codepoint -> {
            if (owners != null) {
                GlyphProvider provider = owners.get(codepoint);

                record(provider, provider.getGlyph(codepoint), codepoint, usedProviders, glyphsByWidth);

                return;
            }

            for (GlyphProvider provider : selectedProviders) {
                UnbakedGlyph glyph = provider.getGlyph(codepoint);

                if (glyph != null) {
                    record(provider, glyph, codepoint, usedProviders, glyphsByWidth);

                    break;
                }
            }
        });

        return new Selection(selectedProviders.stream().filter(usedProviders::contains).toList(), glyphsByWidth);
    }

    private static void record(GlyphProvider provider, UnbakedGlyph glyph, int codepoint, Set<GlyphProvider> usedProviders, Int2ObjectMap<IntList> glyphsByWidth) {
        if (glyph == null)
            throw new IllegalStateException(provider + " lists codepoint " + codepoint + " as supported but has no glyph for it");

        usedProviders.add(provider);

        if (glyph.info() != SpecialGlyphs.MISSING)
            glyphsByWidth.computeIfAbsent(Mth.ceil(glyph.info().getAdvance(false)), width -> new IntArrayList()).add(codepoint);
    }

    private static @Nullable Int2ObjectMap<GlyphProvider> ownersBySupportedGlyphs(List<GlyphProvider> selectedProviders, Map<GlyphProvider, IntSet> supported) {
        for (GlyphProvider provider : selectedProviders) {
            if (!SUPPORTED_SET_PROVIDERS.contains(provider.getClass()))
                return null;
        }

        Int2ObjectMap<GlyphProvider> owners = new Int2ObjectOpenHashMap<>();

        for (GlyphProvider provider : selectedProviders) {
            supported.get(provider).forEach(codepoint -> owners.putIfAbsent(codepoint, provider));
        }

        return owners;
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
