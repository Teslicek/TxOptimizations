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

    private static final int    MAX_ENTRIES         = 4096;
    private static final int    MAX_SEQUENCE_WIDTHS = 16384;
    private static final long   UNUSED_FRAMES       = 200L;
    private static final Caches EXTRACT             = new Caches(false);
    private static final Caches RENDER              = new Caches(true);

    private TextLayoutCache() {
    }

    public static int generation() {
        return current().generation;
    }

    public static int entryClears() {
        return current().entryClears;
    }

    public static Font.PreparedText find(Font font, FormattedCharSequence text, float x, float y, int color, boolean dropShadow, int backgroundColor, boolean includeEmpty, boolean outline) {
        return current().find(font, text, x, y, color, dropShadow, backgroundColor, includeEmpty, outline);
    }

    public static boolean ensure(Font font, FormattedCharSequence text, float x, float y, int color, boolean dropShadow, int backgroundColor, boolean outline) {
        Caches caches = current();

        if (caches.find(font, text, x, y, color, dropShadow, backgroundColor, false, outline) != null)
            return true;

        if (!isStable(text))
            return false;

        caches.put(outline ? font.prepare8xTextOutline(text, x, y, color) : font.prepareText(text, x, y, color, dropShadow, false, backgroundColor), font, text, x, y, color, dropShadow, backgroundColor, false, outline);

        return true;
    }

    public static Font.PreparedText store(Font.PreparedText prepared, Font font, FormattedCharSequence text, float x, float y, int color, boolean dropShadow, int backgroundColor, boolean includeEmpty, boolean outline) {
        if (!isStable(text))
            return prepared;

        return current().put(prepared, font, text, x, y, color, dropShadow, backgroundColor, includeEmpty, outline);
    }

    public static Font.PreparedText storeSegmented(Font.PreparedText prepared, Font font, FormattedCharSequence text, float x, float y, int color, boolean dropShadow, int backgroundColor, boolean includeEmpty) {
        if (!isStable(text))
            return prepared;

        return current().put(new SegmentedText(prepared), font, text, x, y, color, dropShadow, backgroundColor, includeEmpty, false);
    }

    public static int width(Font font, FormattedText text) {
        if (!(text instanceof MutableComponent component))
            return font.width(text);

        return current().width(font, component);
    }

    public static int width(Font font, FormattedCharSequence text) {
        return current().width(font, text);
    }

    public static int width(Font font, String text) {
        return current().width(font, text);
    }

    public static FormattedCharSequence visualOrder(Language language, String text) {
        return current().visualOrder(language, text);
    }

    public static boolean isRepeated(FormattedCharSequence text) {
        return text instanceof SharedText shared && shared.repeated || current().held.containsKey(text);
    }

    public static FormattedCharSequence hold(FormattedCharSequence text) {
        return current().hold(text);
    }

    public static void clear() {
        if (RenderWorker.isCurrent())
            throw new IllegalStateException("Text layouts are cleared from the render worker");

        RenderWorker.awaitFrame();
        EXTRACT.clear();
        RENDER.clear();
    }

    private static Caches current() {
        return RenderWorker.isCurrent() || ParallelFeatures.isWorkerThread() ? RENDER : EXTRACT;
    }

    private static boolean isStable(FormattedCharSequence text) {
        return text.accept((position, style, codepoint) -> !style.isObfuscated() && style.getFont() instanceof FontDescription.Resource);
    }

    private static final class Caches {

        private final Map<Key, Key>                     entries        = new HashMap<>();
        private final Map<MutableComponent, Width>      widths         = new IdentityHashMap<>();
        private final Map<FormattedCharSequence, Width> sequenceWidths = new IdentityHashMap<>();
        private final Map<String, Width>                stringWidths   = new HashMap<>();
        private final Map<String, SharedText>           sequences      = new HashMap<>();
        private final Map<FormattedCharSequence, Held>  held           = new IdentityHashMap<>();
        private final Key                               probe          = new Key();
        private final boolean                           render;
        private long                                    lastEviction;
        private int                                     generation;
        private int                                     entryClears;

        private Caches(boolean render) {
            this.render = render;
        }

        private Font.PreparedText find(Font font, FormattedCharSequence text, float x, float y, int color, boolean dropShadow, int backgroundColor, boolean includeEmpty, boolean outline) {
            long frame = ClientClock.frame();

            this.evictUnused(frame);
            this.probe.set(font, text, x, y, color, dropShadow, backgroundColor, includeEmpty, outline);

            Key entry = this.entries.get(this.probe);

            if (entry == null)
                return null;

            entry.frame = frame;

            return entry.prepared;
        }

        private Font.PreparedText put(Font.PreparedText prepared, Font font, FormattedCharSequence text, float x, float y, int color, boolean dropShadow, int backgroundColor, boolean includeEmpty, boolean outline) {
            if (this.entries.size() >= MAX_ENTRIES) {
                this.entries.clear();
                this.entryClears ++;
            }

            Key key = new Key();

            key.set(font, text, x, y, color, dropShadow, backgroundColor, includeEmpty, outline);
            key.prepared = prepared;
            key.frame    = ClientClock.frame();
            this.entries.put(key, key);

            return prepared;
        }

        private int width(Font font, MutableComponent component) {
            long frame = ClientClock.frame();

            this.evictUnused(frame);

            Width cached = this.widths.get(component);

            if (cached != null && cached.font == font) {
                cached.frame = frame;

                return cached.width;
            }

            if (this.widths.size() >= MAX_ENTRIES)
                this.widths.clear();

            int width = font.width(component);

            this.widths.put(component, new Width(font, width, frame));

            return width;
        }

        private int width(Font font, FormattedCharSequence text) {
            long frame = ClientClock.frame();

            this.evictUnused(frame);

            Width cached = this.sequenceWidths.get(text);

            if (cached != null && cached.font == font) {
                cached.frame = frame;

                return cached.width;
            }

            if (this.sequenceWidths.size() >= MAX_SEQUENCE_WIDTHS)
                this.sequenceWidths.clear();

            int width = font.width(text);

            this.sequenceWidths.put(text, new Width(font, width, frame));

            return width;
        }

        private int width(Font font, String text) {
            long frame = ClientClock.frame();

            this.evictUnused(frame);

            Width cached = this.stringWidths.get(text);

            if (cached != null && cached.font == font) {
                cached.frame = frame;

                return cached.width;
            }

            if (this.stringWidths.size() >= MAX_ENTRIES)
                this.stringWidths.clear();

            int width = font.width(text);

            this.stringWidths.put(text, new Width(font, width, frame));

            return width;
        }

        private FormattedCharSequence visualOrder(Language language, String text) {
            long frame = ClientClock.frame();

            this.evictUnused(frame);

            SharedText cached = this.sequences.get(text);

            if (cached != null && cached.language == language) {
                cached.repeated |= cached.frame != frame;
                cached.frame     = frame;

                return cached;
            }

            if (this.sequences.size() >= MAX_ENTRIES)
                this.sequences.clear();

            SharedText sequence = new SharedText(language, language.getVisualOrder(FormattedText.of(text)), frame);

            this.sequences.put(text, sequence);

            return sequence;
        }

        private FormattedCharSequence hold(FormattedCharSequence text) {
            long frame = ClientClock.frame();
            Held held  = this.held.get(text);

            this.evictUnused(frame);

            if (held != null) {
                held.frame = frame;
                return text;
            }

            if (this.held.size() >= MAX_ENTRIES)
                this.held.clear();

            this.held.put(text, new Held(frame));

            return text;
        }

        private void clear() {
            this.entries.clear();
            this.widths.clear();
            this.sequenceWidths.clear();
            this.stringWidths.clear();
            this.sequences.clear();
            this.held.clear();
            this.generation ++;
        }

        private void evictUnused(long frame) {
            if (frame - this.lastEviction < UNUSED_FRAMES || this.render != RenderWorker.isCurrent())
                return;

            this.lastEviction = frame;

            this.entries.values().removeIf(entry -> frame - entry.frame > UNUSED_FRAMES);
            this.widths.values().removeIf(width -> frame - width.frame > UNUSED_FRAMES);
            this.sequenceWidths.values().removeIf(width -> frame - width.frame > UNUSED_FRAMES);
            this.stringWidths.values().removeIf(width -> frame - width.frame > UNUSED_FRAMES);
            this.sequences.values().removeIf(sequence -> frame - sequence.frame > UNUSED_FRAMES);
            this.held.values().removeIf(held -> frame - held.frame > UNUSED_FRAMES);
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
        private boolean               includeEmpty;
        private boolean               outline;
        private int                   hash;
        private Font.PreparedText     prepared;
        private long                  frame;

        private void set(Font font, FormattedCharSequence text, float x, float y, int color, boolean dropShadow, int backgroundColor, boolean includeEmpty, boolean outline) {
            this.font            = font;
            this.text            = text;
            this.x               = x;
            this.y               = y;
            this.color           = color;
            this.dropShadow      = dropShadow;
            this.backgroundColor = backgroundColor;
            this.includeEmpty    = includeEmpty;
            this.outline         = outline;

            int value = System.identityHashCode(text);

            value = 31 * value + Float.floatToIntBits(x);
            value = 31 * value + Float.floatToIntBits(y);
            value = 31 * value + color;
            value = 31 * value + backgroundColor;
            value = 31 * value + Boolean.hashCode(outline);
            value = 31 * value + Boolean.hashCode(includeEmpty);

            this.hash = 31 * value + Boolean.hashCode(dropShadow);
        }

        @Override
        public boolean equals(Object other) {
            return other instanceof Key that && this.font == that.font && this.text == that.text && Float.compare(this.x, that.x) == 0 && Float.compare(this.y, that.y) == 0 && this.color == that.color && this.dropShadow == that.dropShadow && this.backgroundColor == that.backgroundColor && this.includeEmpty == that.includeEmpty && this.outline == that.outline;
        }

        @Override
        public int hashCode() {
            return this.hash;
        }
    }

    private static final class Held {

        private long frame;

        private Held(long frame) {
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
