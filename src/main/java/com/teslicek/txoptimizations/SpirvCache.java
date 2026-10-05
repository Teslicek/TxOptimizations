package com.teslicek.txoptimizations;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.renderpearl.api.pipeline.ShaderSource;
import com.mojang.renderpearl.api.pipeline.ShaderType;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderDefines;
import net.minecraft.client.renderer.ShaderManager;
import net.minecraft.resources.Identifier;
import org.lwjgl.Version;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.util.shaderc.ShadercIncludeResult;

public final class SpirvCache {

    private static final String DIRECTORY   = "txoptimizations/spirv";
    private static final int    SPIRV_MAGIC = 0x07230203;
    private static final int    MIN_SIZE    = 20;

    private static WeakReference<ShaderManager.Configs> includesOwner = new WeakReference<>(null);
    private static byte[]                               includesDigest;

    private SpirvCache() {
    }

    public static Path file(boolean zeroToOne, boolean drawParameters, String name, String source, ShaderType type, ShaderDefines defines, ShaderManager.Configs configs) {
        MessageDigest digest = sha256();

        update(digest, "txoptimizations-spirv-1");
        update(digest, FabricLoader.getInstance().getModContainer("minecraft").orElseThrow().getMetadata().getVersion().getFriendlyString());
        update(digest, Version.getVersion());
        update(digest, Boolean.toString(zeroToOne));
        update(digest, Boolean.toString(drawParameters));
        update(digest, Boolean.toString(RenderSystem.getDevice().getDeviceInfo().hintsAndWorkarounds().isExplicitDepthRequired()));
        update(digest, name);
        update(digest, type.name());
        update(digest, source);

        for (Map.Entry<String, String> macro : defines.values().entrySet()) {
            update(digest, macro.getKey());
            update(digest, macro.getValue());
        }

        update(digest, "flags");

        for (String flag : defines.flags())
            update(digest, flag);

        digest.update(includesDigest(configs));

        return Minecraft.getInstance().gameDirectory.toPath().resolve(DIRECTORY).resolve(HexFormat.of().formatHex(digest.digest()) + ".spv");
    }

    public static ByteBuffer read(Path file) {
        try (FileChannel channel = FileChannel.open(file, StandardOpenOption.READ)) {
            long size = channel.size();

            if (size < MIN_SIZE || size % 4 != 0 || size > Integer.MAX_VALUE)
                throw new IllegalStateException("Cached SPIR-V " + file + " has invalid size " + size);

            ByteBuffer spirv = MemoryUtil.memAlloc((int) size);

            try {
                while (spirv.hasRemaining()) {
                    if (channel.read(spirv) < 0)
                        throw new IllegalStateException("Cached SPIR-V " + file + " ended at " + spirv.position() + " of " + size + " bytes");
                }

                spirv.flip();

                if (MemoryUtil.memGetInt(MemoryUtil.memAddress(spirv)) != SPIRV_MAGIC)
                    throw new IllegalStateException("Cached SPIR-V " + file + " does not start with the SPIR-V magic number");

                return spirv;
            } catch (RuntimeException | IOException exception) {
                MemoryUtil.memFree(spirv);
                throw exception;
            }
        } catch (NoSuchFileException exception) {
            return null;
        } catch (IOException exception) {
            throw new UncheckedIOException("Could not read cached SPIR-V " + file, exception);
        }
    }

    public static void write(Path file, ByteBuffer spirv) {
        try {
            Files.createDirectories(file.getParent());

            Path temporary = Files.createTempFile(file.getParent(), file.getFileName().toString(), ".tmp");

            try (FileChannel channel = FileChannel.open(temporary, StandardOpenOption.WRITE)) {
                ByteBuffer view = spirv.duplicate();

                while (view.hasRemaining())
                    channel.write(view);
            }

            Files.move(temporary, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException exception) {
            throw new UncheckedIOException("Could not write cached SPIR-V " + file, exception);
        }
    }

    private static synchronized byte[] includesDigest(ShaderManager.Configs configs) {
        if (includesOwner.get() == configs)
            return includesDigest;

        List<Map.Entry<Identifier, ShaderSource.CachedIncludeSource>> includes = new ArrayList<>(configs.includeSources().entrySet());

        includes.sort(Comparator.comparing(include -> include.getKey().toString()));

        MessageDigest digest = sha256();

        for (Map.Entry<Identifier, ShaderSource.CachedIncludeSource> include : includes) {
            ShadercIncludeResult result = ShadercIncludeResult.create(include.getValue().includeResultPtr());

            update(digest, include.getKey().toString());
            update(digest, result.source_nameString());
            digest.update(longBytes(result.content_length()));
            digest.update(result.content());
        }

        includesOwner  = new WeakReference<>(configs);
        includesDigest = digest.digest();

        return includesDigest;
    }

    private static void update(MessageDigest digest, String value) {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);

        digest.update(longBytes(bytes.length));
        digest.update(bytes);
    }

    private static byte[] longBytes(long value) {
        return ByteBuffer.allocate(Long.BYTES).putLong(value).array();
    }

    private static MessageDigest sha256() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }
}
