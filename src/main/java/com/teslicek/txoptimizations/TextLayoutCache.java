package com.teslicek.txoptimizations;

import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import net.minecraft.client.gui.Font;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.FormattedCharSink;

public final class TextLayoutCache {

    private static final int  MAX_ENTRIES         = 4096;
    private static final int  MAX_SEQUENCE_WIDTHS = 16384;
    private static final long UNUSED_FRAMES       = 200L;

    private static final Map<Key, Entry>                   ENTRIES         = new HashMap<>();
    private static final Map<MutableComponent, Width>      WIDTHS          = new IdentityHashMap<>();
    private static final Map<FormattedCharSequence, Width> SEQUENCE_WIDTHS = new IdentityHashMap<>();
    private static final Map<String, Width>                STRING_WIDTHS   = new HashMap<>();
    private static final Map<String, SharedText>           SEQUENCES       = new HashMap<>();
    private static final Key                               PROBE           = new Key();

    private static long lastEviction;

    private TextLayoutCache() {
    }

    public static Font.PreparedText find(Font font, FormattedCharSequence text, float x, float y, int color, boolean dropShadow, int backgroundColor, boolean outline) {
        long frame = ClientClock.frame();

        evictUnused(frame);
        PROBE.set(font, text, x, y, color, dropShadow, backgroundColor, outline);

        Entry entry = ENTRIES.get(PROBE);

        if (entry == null)
            return null;

        entry.frame = frame;

        return entry.text;
    }

    public static Font.PreparedText store(Font.PreparedText prepared, Font font, FormattedCharSequence text, float x, float y, int color, boolean dropShadow, int backgroundColor, boolean outline) {
        if (!isStable(text))
            return prepared;

        if (ENTRIES.size() >= MAX_ENTRIES)
            ENTRIES.clear();

        Key key = new Key();

        key.set(font, text, x, y, color, dropShadow, backgroundColor, outline);
        ENTRIES.put(key, new Entry(prepared, ClientClock.frame()));

        return prepared;
    }

    public static int width(Font font, FormattedText text) {
        if (!(text instanceof MutableComponent component))
            return font.width(text);

        long frame = ClientClock.frame();

        evictUnused(frame);

        Width cached = WIDTHS.get(component);

        if (cached != null && cached.font == font) {
            cached.frame = frame;

            return cached.width;
        }

        if (WIDTHS.size() >= MAX_ENTRIES)
            WIDTHS.clear();

        int width = font.width(component);

        WIDTHS.put(component, new Width(font, width, frame));

        return width;
    }

    public static int width(Font font, FormattedCharSequence text) {
        long frame = ClientClock.frame();

        evictUnused(frame);

        Width cached = SEQUENCE_WIDTHS.get(text);

        if (cached != null && cached.font == font) {
            cached.frame = frame;

            return cached.width;
        }

        if (SEQUENCE_WIDTHS.size() >= MAX_SEQUENCE_WIDTHS)
            SEQUENCE_WIDTHS.clear();

        int width = font.width(text);

        SEQUENCE_WIDTHS.put(text, new Width(font, width, frame));

        return width;
    }

    public static int width(Font font, String text) {
        long frame = ClientClock.frame();

        evictUnused(frame);

        Width cached = STRING_WIDTHS.get(text);

        if (cached != null && cached.font == font) {
            cached.frame = frame;

            return cached.width;
        }

        if (STRING_WIDTHS.size() >= MAX_ENTRIES)
            STRING_WIDTHS.clear();

        int width = font.width(text);

        STRING_WIDTHS.put(text, new Width(font, width, frame));

        return width;
    }

    public static FormattedCharSequence visualOrder(Language language, String text) {
        long frame = ClientClock.frame();

        evictUnused(frame);

        SharedText cached = SEQUENCES.get(text);

        if (cached != null && cached.language == language) {
            cached.repeated |= cached.frame != frame;
            cached.frame     = frame;

            return cached;
        }

        if (SEQUENCES.size() >= MAX_ENTRIES)
            SEQUENCES.clear();

        SharedText sequence = new SharedText(language, language.getVisualOrder(FormattedText.of(text)), frame);

        SEQUENCES.put(text, sequence);

        return sequence;
    }

    public static boolean isRepeated(FormattedCharSequence text) {
        return text instanceof SharedText shared && shared.repeated;
    }

    public static void clear() {
        ENTRIES.clear();
        WIDTHS.clear();
        SEQUENCE_WIDTHS.clear();
        STRING_WIDTHS.clear();
        SEQUENCES.clear();
    }

    private static boolean isStable(FormattedCharSequence text) {
        return text.accept((position, style, codepoint) -> !style.isObfuscated() && style.getFont() instanceof FontDescription.Resource);
    }

    private static void evictUnused(long frame) {
        if (frame - lastEviction < UNUSED_FRAMES)
            return;

        lastEviction = frame;

        ENTRIES.values().removeIf(entry -> frame - entry.frame > UNUSED_FRAMES);
        WIDTHS.values().removeIf(width -> frame - width.frame > UNUSED_FRAMES);
        SEQUENCE_WIDTHS.values().removeIf(width -> frame - width.frame > UNUSED_FRAMES);
        STRING_WIDTHS.values().removeIf(width -> frame - width.frame > UNUSED_FRAMES);
        SEQUENCES.values().removeIf(sequence -> frame - sequence.frame > UNUSED_FRAMES);
    }

    private static final class Key {

        private Font                  font;
        private FormattedCharSequence text;
        private float                 x;
        private float                 y;
        private int                   color;
        private boolean               dropShadow;
        private int                   backgroundColor;
        private boolean               outline;
        private int                   hash;

        private void set(Font font, FormattedCharSequence text, float x, float y, int color, boolean dropShadow, int backgroundColor, boolean outline) {
            this.font            = font;
            this.text            = text;
            this.x               = x;
            this.y               = y;
            this.color           = color;
            this.dropShadow      = dropShadow;
            this.backgroundColor = backgroundColor;
            this.outline         = outline;

            int value = System.identityHashCode(text);

            value = 31 * value + Float.floatToIntBits(x);
            value = 31 * value + Float.floatToIntBits(y);
            value = 31 * value + color;
            value = 31 * value + backgroundColor;
            value = 31 * value + Boolean.hashCode(outline);

            this.hash = 31 * value + Boolean.hashCode(dropShadow);
        }

        @Override
        public boolean equals(Object other) {
            return other instanceof Key that && this.font == that.font && this.text == that.text && Float.compare(this.x, that.x) == 0 && Float.compare(this.y, that.y) == 0 && this.color == that.color && this.dropShadow == that.dropShadow && this.backgroundColor == that.backgroundColor && this.outline == that.outline;
        }

        @Override
        public int hashCode() {
            return this.hash;
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

    private static final class Width {

        private final Font font;
        private final int  width;
        private long       frame;

        private Width(Font font, int width, long frame) {
            this.font  = font;
            this.width = width;
            this.frame = frame;
        }
    }

    private static final class SharedText implements FormattedCharSequence {

        private final Language              language;
        private final FormattedCharSequence sequence;
        private long                        frame;
        private boolean                     repeated;

        private SharedText(Language language, FormattedCharSequence sequence, long frame) {
            this.language = language;
            this.sequence = sequence;
            this.frame    = frame;
        }

        @Override
        public boolean accept(FormattedCharSink sink) {
            return this.sequence.accept(sink);
        }
    }
}
