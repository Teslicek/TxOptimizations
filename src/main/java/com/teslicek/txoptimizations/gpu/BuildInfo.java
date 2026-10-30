package com.teslicek.txoptimizations.gpu;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Properties;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;

final class BuildInfo {

    private static final String MOD_ID   = "txoptimizations";
    private static final String RESOURCE = "/txoptimizations-build.properties";
    private static final String SUMMARY  = summary();

    private BuildInfo() {
    }

    static String describe() {
        return SUMMARY;
    }

    private static String summary() {
        ModContainer mod        = FabricLoader.getInstance().getModContainer(MOD_ID).orElseThrow(() -> new IllegalStateException("Mod " + MOD_ID + " is not loaded"));
        Properties   properties = new Properties();

        try (InputStream input = BuildInfo.class.getResourceAsStream(RESOURCE)) {
            if (input == null)
                throw new IllegalStateException("The jar holds no " + RESOURCE);

            properties.load(input);
        } catch (IOException exception) {
            throw new UncheckedIOException("Could not read " + RESOURCE, exception);
        }

        return "TxOptimizations " + mod.getMetadata().getVersion().getFriendlyString() + ", commit " + required(properties, "commit") + (Boolean.parseBoolean(required(properties, "dirty")) ? " with uncommitted changes" : "") + ", built " + required(properties, "built") + ", jar sha1 " + sha1(mod.getOrigin().getPaths());
    }

    private static String required(Properties properties, String key) {
        String value = properties.getProperty(key);

        if (value == null)
            throw new IllegalStateException(RESOURCE + " has no " + key);

        return value;
    }

    private static String sha1(List<Path> paths) {
        if (paths.size() != 1 || !Files.isRegularFile(paths.getFirst()))
            return "not a single jar (" + paths + ")";

        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-1").digest(Files.readAllBytes(paths.getFirst())));
        } catch (IOException exception) {
            throw new UncheckedIOException("Could not hash " + paths.getFirst(), exception);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("The JVM has no SHA-1", exception);
        }
    }
}
