package com.teslicek.txoptimizations;

import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.util.FormattedCharSequence;

public final class TextLayoutCache {

    private static final int  MAX_ENTRIES   = 4096;
    private static final long UNUSED_FRAMES = 200L;
    private static final int  MAX_FONTS     = 1 << 16;
    private static final long BOLD          = 1L << 37;
    private static final long ITALIC        = 1L << 38;
    private static final long UNDERLINED    = 1L << 39;
    private static final long STRIKETHROUGH = 1L << 40;
    private static final long FIRST         = 1L << 41;
    private static final long HAS_COLOR     = 1L << 24;
    private static final long HAS_SHADOW    = 1L << 25;

    private static final Map<IdentityKey, Entry>                BY_IDENTITY = new HashMap<>();
    private static final Map<ContentKey, Entry>                 BY_CONTENT  = new HashMap<>();
    private static final Object2IntOpenHashMap<FontDescription> FONT_IDS    = new Object2IntOpenHashMap<>();
    private static final LongArrayList                          CHARACTERS  = new LongArrayList();

    private static long lastEviction;

    private TextLayoutCache() {
    }

    public static Font.PreparedText get(Font font, FormattedCharSequence text, float x, float y, int color, boolean dropShadow, int backgroundColor, boolean outline, Supplier<Font.PreparedText> prepare) {
        long        frame    = ClientClock.frame();
        IdentityKey identity = new IdentityKey(font, text, x, y, color, dropShadow, backgroundColor, outline);
        Entry       entry    = BY_IDENTITY.get(identity);

        evictUnused(frame);

        if (entry != null) {
            entry.frame = frame;

            return entry.text;
        }

        if (!describe(text))
            return prepare.get();

        ContentKey content = new ContentKey(font, CHARACTERS.toLongArray(), x, y, color, dropShadow, backgroundColor, outline);

        entry = BY_CONTENT.get(content);

        if (entry != null) {
            entry.frame = frame;

            return entry.text;
        }

        if (BY_CONTENT.size() >= MAX_ENTRIES || BY_IDENTITY.size() >= MAX_ENTRIES)
            clear();

        entry = new Entry(prepare.get(), frame);
        BY_CONTENT.put(content, entry);
        BY_IDENTITY.put(identity, entry);

        return entry.text;
    }

    public static void clear() {
        BY_IDENTITY.clear();
        BY_CONTENT.clear();
    }

    private static boolean describe(FormattedCharSequence text) {
        CHARACTERS.clear();

        return text.accept((position, style, codepoint) -> {
            if (style.isObfuscated() || !(style.getFont() instanceof FontDescription.Resource))
                return false;

            CHARACTERS.add(codepoint | (long) fontId(style.getFont()) << 21 | flags(style, position));
            CHARACTERS.add(colors(style));

            return true;
        });
    }

    private static int fontId(FontDescription font) {
        int id = FONT_IDS.getOrDefault(font, -1);

        if (id != -1)
            return id;

        if (FONT_IDS.size() >= MAX_FONTS)
            throw new IllegalStateException("More than " + MAX_FONTS + " distinct fonts in world text");

        id = FONT_IDS.size();
        FONT_IDS.put(font, id);

        return id;
    }

    private static long flags(Style style, int position) {
        long flags = 0L;

        if (style.isBold())
            flags |= BOLD;

        if (style.isItalic())
            flags |= ITALIC;

        if (style.isUnderlined())
            flags |= UNDERLINED;

        if (style.isStrikethrough())
            flags |= STRIKETHROUGH;

        if (position == 0)
            flags |= FIRST;

        return flags;
    }

    private static long colors(Style style) {
        TextColor color  = style.getColor();
        Integer   shadow = style.getShadowColor();
        long      value  = 0L;

        if (color != null)
            value |= HAS_COLOR | color.getValue() & 0xFFFFFFL;

        if (shadow != null)
            value |= HAS_SHADOW | (shadow & 0xFFFFFFFFL) << 26;

        return value;
    }

    private static void evictUnused(long frame) {
        if (frame - lastEviction < UNUSED_FRAMES)
            return;

        lastEviction = frame;
        BY_IDENTITY.values().removeIf(entry -> frame - entry.frame > UNUSED_FRAMES);
        BY_CONTENT.values().removeIf(entry -> frame - entry.frame > UNUSED_FRAMES);
    }

    private record IdentityKey(Font font, FormattedCharSequence text, float x, float y, int color, boolean dropShadow, int backgroundColor, boolean outline) {

        @Override
        public boolean equals(Object other) {
            return other instanceof IdentityKey that && this.font == that.font && this.text == that.text && Float.compare(this.x, that.x) == 0 && Float.compare(this.y, that.y) == 0 && this.color == that.color && this.dropShadow == that.dropShadow && this.backgroundColor == that.backgroundColor && this.outline == that.outline;
        }

        @Override
        public int hashCode() {
            int hash = System.identityHashCode(this.text);

            hash = 31 * hash + Float.floatToIntBits(this.x);
            hash = 31 * hash + Float.floatToIntBits(this.y);
            hash = 31 * hash + this.color;
            hash = 31 * hash + this.backgroundColor;
            hash = 31 * hash + Boolean.hashCode(this.outline);

            return 31 * hash + Boolean.hashCode(this.dropShadow);
        }
    }

    private record ContentKey(Font font, long[] characters, float x, float y, int color, boolean dropShadow, int backgroundColor, boolean outline) {

        @Override
        public boolean equals(Object other) {
            return other instanceof ContentKey that && this.font == that.font && Arrays.equals(this.characters, that.characters) && Float.compare(this.x, that.x) == 0 && Float.compare(this.y, that.y) == 0 && this.color == that.color && this.dropShadow == that.dropShadow && this.backgroundColor == that.backgroundColor && this.outline == that.outline;
        }

        @Override
        public int hashCode() {
            int hash = Arrays.hashCode(this.characters);

            hash = 31 * hash + Float.floatToIntBits(this.x);
            hash = 31 * hash + Float.floatToIntBits(this.y);
            hash = 31 * hash + this.color;
            hash = 31 * hash + this.backgroundColor;
            hash = 31 * hash + Boolean.hashCode(this.outline);

            return 31 * hash + Boolean.hashCode(this.dropShadow);
        }
    }

    private static final class Entry {

        private final Font.PreparedText text;
        private long                    frame;

        private Entry(Font.PreparedText text, long frame) {
            this.text  = text;
            this.frame = frame;
        }
    }
}
