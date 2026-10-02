package com.teslicek.txoptimizations;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.util.FormattedCharSequence;

public final class TextLayoutCache {

    private static final int  MAX_ENTRIES   = 4096;
    private static final long UNUSED_FRAMES = 200L;

    private static final Map<Key, Entry> ENTRIES = new HashMap<>();
    private static final Key             PROBE   = new Key();

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

    public static void clear() {
        ENTRIES.clear();
    }

    private static boolean isStable(FormattedCharSequence text) {
        return text.accept((position, style, codepoint) -> !style.isObfuscated() && style.getFont() instanceof FontDescription.Resource);
    }

    private static void evictUnused(long frame) {
        if (frame - lastEviction < UNUSED_FRAMES)
            return;

        lastEviction = frame;

        Iterator<Entry> iterator = ENTRIES.values().iterator();

        while (iterator.hasNext()) {
            if (frame - iterator.next().frame > UNUSED_FRAMES)
                iterator.remove();
        }
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
}
