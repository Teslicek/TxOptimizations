package com.teslicek.txoptimizations;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import net.fabricmc.loader.api.FabricLoader;

public final class TxOptimizationsConfig {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path PATH = FabricLoader.getInstance().getConfigDir().resolve("txoptimizations.json");

    private static Data    data   = new Data();
    private static boolean loaded;

    private TxOptimizationsConfig() {
    }

    public static void load() {
        if (loaded)
            throw new IllegalStateException("TxOptimizations config loaded twice");

        loaded = true;

        if (Files.exists(PATH)) {
            try (Reader reader = Files.newBufferedReader(PATH)) {
                JsonElement tree = JsonParser.parseReader(reader);

                rejectNulls(tree, PATH.getFileName().toString());
                data = GSON.fromJson(tree, Data.class);
            } catch (IOException error) {
                throw new IllegalStateException("Failed to read TxOptimizations config " + PATH, error);
            }
        }

        save();
    }

    public static boolean fpsCounter() {
        return data.fpsCounter;
    }

    public static void setFpsCounter(boolean enabled) {
        data.fpsCounter = enabled;
        save();
    }

    private static void save() {
        if (!loaded)
            throw new IllegalStateException("TxOptimizations config saved before it was loaded");

        try {
            Files.createDirectories(PATH.getParent());

            try (Writer writer = Files.newBufferedWriter(PATH)) {
                GSON.toJson(data, writer);
            }
        } catch (IOException error) {
            throw new IllegalStateException("Failed to write TxOptimizations config " + PATH, error);
        }
    }

    private static void rejectNulls(JsonElement element, String path) {
        if (element.isJsonNull())
            throw new IllegalStateException("TxOptimizations config has a null value at " + path);

        if (!element.isJsonObject())
            return;

        for (Map.Entry<String, JsonElement> entry : element.getAsJsonObject().entrySet())
            rejectNulls(entry.getValue(), path + "." + entry.getKey());
    }

    private static final class Data {
        private boolean fpsCounter = true;
    }
}
