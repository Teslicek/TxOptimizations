package com.teslicek.txoptimizations;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.Map;
import java.util.stream.Collectors;
import net.fabricmc.loader.api.FabricLoader;

public final class TxOptimizationsConfig {

    private static final Gson GSON      = new GsonBuilder().setPrettyPrinting().create();
    private static final Path PATH      = FabricLoader.getInstance().getConfigDir().resolve("txoptimizations.json");
    private static final Path TEMPORARY = PATH.resolveSibling("txoptimizations.json.tmp");

    private static Data    data   = new Data();
    private static boolean loaded;

    private TxOptimizationsConfig() {
    }

    public static void load() {
        if (loaded)
            throw new IllegalStateException("TxOptimizations config loaded twice");

        loaded = true;

        if (!Files.exists(PATH)) {
            save();

            return;
        }

        String text = read();

        try {
            JsonElement tree = JsonParser.parseString(text);

            if (!tree.isJsonObject())
                throw new IllegalStateException("TxOptimizations config " + PATH + " does not hold a JSON object; delete it to start from defaults. Contents: " + printable(text));

            rejectNulls(tree, PATH.getFileName().toString());
            data = GSON.fromJson(tree, Data.class);
        } catch (JsonParseException error) {
            throw new IllegalStateException("TxOptimizations config " + PATH + " is not valid JSON; delete it to start from defaults. Contents: " + printable(text), error);
        }
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

        ByteBuffer bytes = ByteBuffer.wrap(GSON.toJson(data).getBytes(StandardCharsets.UTF_8));

        try {
            Files.createDirectories(PATH.getParent());

            try (FileChannel channel = FileChannel.open(TEMPORARY, StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING)) {
                while (bytes.hasRemaining())
                    channel.write(bytes);

                channel.force(true);
            }

            Files.move(TEMPORARY, PATH, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException error) {
            throw new IllegalStateException("Failed to write TxOptimizations config " + PATH, error);
        }
    }

    private static String read() {
        try {
            return Files.readString(PATH);
        } catch (IOException error) {
            throw new IllegalStateException("Failed to read TxOptimizations config " + PATH, error);
        }
    }

    private static String printable(String text) {
        return text.chars().mapToObj(character -> character < 0x20 && character != '\n' ? String.format("\\u%04x", character) : String.valueOf((char) character)).collect(Collectors.joining());
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
