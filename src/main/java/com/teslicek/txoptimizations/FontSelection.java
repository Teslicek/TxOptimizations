package com.teslicek.txoptimizations;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.font.GlyphProvider;
import com.mojang.blaze3d.font.UnbakedGlyph;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import net.minecraft.client.gui.font.FontOption;
import net.minecraft.client.gui.font.glyphs.SpecialGlyphs;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;

public final class FontSelection {

    private static final int CHUNK_SIZE = 4096;

    private static volatile Pending pending;

    private FontSelection() {
    }

    public static CompletableFuture<Void> prepare(Map<Identifier, List<GlyphProvider.Conditional>> fontSets, Set<FontOption> options, Executor executor) {
        List<CompletableFuture<Entry>> entries = new ArrayList<>(fontSets.size());
        Set<FontOption>                copied  = Set.copyOf(options);

        for (List<GlyphProvider.Conditional> providers : fontSets.values()) {
            List<GlyphProvider.Conditional> ordered = List.copyOf(Lists.reverse(providers));

            entries.add(select(ordered, copied, executor).thenApply(selection -> new Entry(ordered, selection)));
        }

        return Util.sequence(entries).thenAccept(prepared -> pending = new Pending(copied, prepared));
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

    private static CompletableFuture<Selection> select(List<GlyphProvider.Conditional> providers, Set<FontOption> options, Executor executor) {
        IntSet              supportedGlyphs   = new IntOpenHashSet();
        List<GlyphProvider> selectedProviders = new ArrayList<>();

        for (GlyphProvider.Conditional conditionalProvider : providers) {
            if (conditionalProvider.filter().apply(options)) {
                selectedProviders.add(conditionalProvider.provider());
                supportedGlyphs.addAll(conditionalProvider.provider().getSupportedGlyphs());
            }
        }

        IntArrayList codepoints = new IntArrayList(supportedGlyphs.size());

        supportedGlyphs.forEach(codepoints::add);

        List<CompletableFuture<Chunk>> chunks = new ArrayList<>();

        for (int from = 0; from < codepoints.size(); from += CHUNK_SIZE) {
            int start = from;
            int end   = Math.min(from + CHUNK_SIZE, codepoints.size());

            chunks.add(CompletableFuture.supplyAsync(() -> selectChunk(codepoints, start, end, selectedProviders), executor));
        }

        return Util.sequence(chunks).thenApply(selected -> merge(selected, selectedProviders));
    }

    private static Chunk selectChunk(IntList codepoints, int start, int end, List<GlyphProvider> selectedProviders) {
        Int2ObjectMap<IntList> glyphsByWidth = new Int2ObjectOpenHashMap<>();
        boolean[]              used          = new boolean[selectedProviders.size()];

        for (int index = start; index < end; index ++) {
            int codepoint = codepoints.getInt(index);

            for (int provider = 0; provider < selectedProviders.size(); provider ++) {
                UnbakedGlyph glyph = selectedProviders.get(provider).getGlyph(codepoint);

                if (glyph != null) {
                    used[provider] = true;

                    if (glyph.info() != SpecialGlyphs.MISSING)
                        glyphsByWidth.computeIfAbsent(Mth.ceil(glyph.info().getAdvance(false)), width -> new IntArrayList()).add(codepoint);

                    break;
                }
            }
        }

        return new Chunk(glyphsByWidth, used);
    }

    private static Selection merge(List<Chunk> chunks, List<GlyphProvider> selectedProviders) {
        Int2ObjectMap<IntList> glyphsByWidth = new Int2ObjectOpenHashMap<>();
        boolean[]              used          = new boolean[selectedProviders.size()];

        for (Chunk chunk : chunks) {
            for (Int2ObjectMap.Entry<IntList> width : chunk.glyphsByWidth().int2ObjectEntrySet())
                glyphsByWidth.computeIfAbsent(width.getIntKey(), key -> new IntArrayList()).addAll(width.getValue());

            for (int provider = 0; provider < used.length; provider ++)
                used[provider] |= chunk.used()[provider];
        }

        Set<GlyphProvider> usedProviders = new HashSet<>();

        for (int provider = 0; provider < used.length; provider ++) {
            if (used[provider])
                usedProviders.add(selectedProviders.get(provider));
        }

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

    private record Chunk(Int2ObjectMap<IntList> glyphsByWidth, boolean[] used) {
    }

    private record Entry(List<GlyphProvider.Conditional> providers, Selection selection) {
    }

    private record Pending(Set<FontOption> options, List<Entry> entries) {
    }
}
