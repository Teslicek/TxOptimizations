package com.teslicek.txoptimizations;

import com.mojang.blaze3d.font.GlyphProvider;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

public final class UnifontCache {

    private static volatile Map<Key, Entry>           previous = Map.of();
    private static volatile @Nullable Map<Key, Entry> current;

    private UnifontCache() {
    }

    public static void begin() {
        current = new ConcurrentHashMap<>();
    }

    public static void finish() {
        Map<Key, Entry> loaded = current;

        if (loaded == null)
            throw new IllegalStateException("Fonts were applied without a font reload in progress");

        previous = Map.copyOf(loaded);
        current  = null;
    }

    public static @Nullable GlyphProvider find(Identifier hexFile, List<?> sizeOverrides, byte[] digest) {
        Entry cached = previous.get(new Key(hexFile, sizeOverrides));

        if (cached == null || !Arrays.equals(cached.digest(), digest))
            return null;

        remember(hexFile, sizeOverrides, digest, cached.provider());

        return cached.provider();
    }

    public static void remember(Identifier hexFile, List<?> sizeOverrides, byte[] digest, GlyphProvider provider) {
        Map<Key, Entry> loaded = current;

        if (loaded == null)
            throw new IllegalStateException("Unifont " + hexFile + " was loaded without a font reload in progress");

        loaded.put(new Key(hexFile, sizeOverrides), new Entry(digest, provider));
    }

    public static byte[] digest(byte[] contents) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(contents);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }

    private record Key(Identifier hexFile, List<?> sizeOverrides) {
    }

    private record Entry(byte[] digest, GlyphProvider provider) {
    }
}
