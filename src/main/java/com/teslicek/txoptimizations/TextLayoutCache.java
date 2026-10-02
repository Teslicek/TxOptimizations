package com.teslicek.txoptimizations;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.util.FormattedCharSequence;

public final class TextLayoutCache {

    private static final int  MAX_ENTRIES   = 4096;
    private static final long UNUSED_FRAMES = 200L;

    private static final Map<Key, Entry> ENTRIES = new HashMap<>();

    private static long lastEviction;

    private TextLayoutCache() {
    }

    public static Font.PreparedText get(Font font, FormattedCharSequence text, float x, float y, int color, boolean dropShadow, int backgroundColor, boolean outline, Supplier<Font.PreparedText> prepare) {
        long  frame = ClientClock.frame();
        Key   key   = new Key(font, text, x, y, color, dropShadow, backgroundColor, outline);
        Entry entry = ENTRIES.get(key);

        evictUnused(frame);

        if (entry != null) {
            entry.frame = frame;

            return entry.text;
        }

        Font.PreparedText prepared = prepare.get();

        if (!isStable(text))
            return prepared;

        if (ENTRIES.size() >= MAX_ENTRIES)
            ENTRIES.clear();

        ENTRIES.put(key, new Entry(prepared, frame));

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

    private record Key(Font font, FormattedCharSequence text, float x, float y, int color, boolean dropShadow, int backgroundColor, boolean outline) {

        @Override
        public boolean equals(Object other) {
            return other instanceof Key that && this.font == that.font && this.text == that.text && Float.compare(this.x, that.x) == 0 && Float.compare(this.y, that.y) == 0 && this.color == that.color && this.dropShadow == that.dropShadow && this.backgroundColor == that.backgroundColor && this.outline == that.outline;
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

    private static final class Entry {

        private final Font.PreparedText text;
        private long                    frame;

        private Entry(Font.PreparedText text, long frame) {
            this.text  = text;
            this.frame = frame;
        }
    }
}
