package com.teslicek.txoptimizations;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.renderpearl.api.vertex.VertexFormat;
import java.util.Arrays;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Map;
import net.minecraft.client.gui.font.TextRenderable;
import org.joml.Matrix3x2fc;
import org.joml.Matrix4f;
import org.lwjgl.system.MemoryUtil;

public final class GlyphVertexCache {

    private static final int                        FULL_BRIGHT   = 15728880;
    private static final long                       UNUSED_FRAMES = 200L;
    private static final int                        MAX_ENTRIES   = 4096;
    private static final Map<TextRenderable, Entry> ENTRIES       = new IdentityHashMap<>();

    private static long lastEviction;

    private GlyphVertexCache() {
    }

    public static void build(TextRenderable[] glyphs, int count, boolean shared, Matrix3x2fc pose, Matrix4f matrix, VertexConsumer consumer) {
        if (count == 0)
            throw new IllegalArgumentException("A glyph run without glyphs cannot build vertices");

        if (!(consumer instanceof DirectVertexBuffer direct) || direct.txoptimizations$duplicatesVertices()) {
            render(glyphs, count, matrix, consumer);
            return;
        }

        long frame = ClientClock.frame();

        evictUnused(frame);

        VertexFormat format = direct.txoptimizations$format();
        Entry        entry  = ENTRIES.get(glyphs[0]);

        if (entry == null || !entry.matches(glyphs, count, pose, format)) {
            render(glyphs, count, matrix, consumer);

            if (entry != null)
                entry.free();

            if (ENTRIES.size() >= MAX_ENTRIES)
                clear();

            ENTRIES.put(glyphs[0], new Entry(shared ? glyphs : Arrays.copyOf(glyphs, count), pose, format, frame));
            return;
        }

        entry.frame = frame;

        if (entry.address != MemoryUtil.NULL) {
            long destination = direct.txoptimizations$tryReserveVertices(format, entry.vertices);

            if (destination != MemoryUtil.NULL) {
                MemoryUtil.memCopy(entry.address, destination, entry.bytes);
                return;
            }

            render(glyphs, count, matrix, consumer);
            return;
        }

        int before = direct.txoptimizations$vertexCount();

        render(glyphs, count, matrix, consumer);

        int vertices = direct.txoptimizations$vertexCount() - before;

        if (vertices > 0)
            entry.capture(direct.txoptimizations$endAddress(), vertices);
    }

    private static void render(TextRenderable[] glyphs, int count, Matrix4f matrix, VertexConsumer consumer) {
        for (int index = 0; index < count; index ++)
            glyphs[index].render(matrix, consumer, FULL_BRIGHT, true);
    }

    private static void evictUnused(long frame) {
        if (frame - lastEviction < UNUSED_FRAMES)
            return;

        lastEviction = frame;

        for (Iterator<Entry> iterator = ENTRIES.values().iterator(); iterator.hasNext(); ) {
            Entry entry = iterator.next();

            if (frame - entry.frame < UNUSED_FRAMES)
                continue;

            entry.free();
            iterator.remove();
        }
    }

    private static void clear() {
        for (Entry entry : ENTRIES.values())
            entry.free();

        ENTRIES.clear();
    }

    private static final class Entry {

        private final TextRenderable[] glyphs;
        private final float            m00;
        private final float            m01;
        private final float            m10;
        private final float            m11;
        private final float            m20;
        private final float            m21;
        private final VertexFormat     format;
        private long                   frame;
        private long                   address = MemoryUtil.NULL;
        private int                    vertices;
        private int                    bytes;

        private Entry(TextRenderable[] glyphs, Matrix3x2fc pose, VertexFormat format, long frame) {
            this.glyphs = glyphs;
            this.m00    = pose.m00();
            this.m01    = pose.m01();
            this.m10    = pose.m10();
            this.m11    = pose.m11();
            this.m20    = pose.m20();
            this.m21    = pose.m21();
            this.format = format;
            this.frame  = frame;
        }

        private boolean matches(TextRenderable[] glyphs, int count, Matrix3x2fc pose, VertexFormat format) {
            if (this.glyphs.length != count || this.format != format || !same(this.m00, pose.m00()) || !same(this.m01, pose.m01()) || !same(this.m10, pose.m10()) || !same(this.m11, pose.m11()) || !same(this.m20, pose.m20()) || !same(this.m21, pose.m21()))
                return false;

            if (this.glyphs == glyphs)
                return true;

            for (int index = 0; index < count; index ++) {
                if (this.glyphs[index] != glyphs[index])
                    return false;
            }

            return true;
        }

        private void capture(long end, int vertices) {
            int bytes = vertices * this.format.getVertexSize();

            this.address  = MemoryUtil.nmemAllocChecked(bytes);
            this.vertices = vertices;
            this.bytes    = bytes;

            MemoryUtil.memCopy(end - bytes, this.address, bytes);
        }

        private void free() {
            if (this.address == MemoryUtil.NULL)
                return;

            MemoryUtil.nmemFree(this.address);
            this.address = MemoryUtil.NULL;
        }

        private static boolean same(float cached, float current) {
            return Float.floatToRawIntBits(cached) == Float.floatToRawIntBits(current);
        }
    }
}
