package com.teslicek.txoptimizations;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.resources.Identifier;

public final class BlocksAtlasSprites {

    private static final String       ATLAS_DEFINITION = "assets/minecraft/atlases/blocks.json";
    private static final Identifier   BLOCKS_ATLAS     = Identifier.withDefaultNamespace("textures/atlas/blocks.png");
    private static final List<String> PREFIXES         = readPrefixes();

    private BlocksAtlasSprites() {
    }

    public static boolean isAddedDuplicate(Identifier sprite, Identifier atlas, Identifier otherAtlas) {
        if (!sprite.getNamespace().equals(Identifier.DEFAULT_NAMESPACE) || !atlas.equals(BLOCKS_ATLAS) && !otherAtlas.equals(BLOCKS_ATLAS))
            return false;

        for (String prefix : PREFIXES) {
            if (sprite.getPath().startsWith(prefix))
                return true;
        }

        return false;
    }

    private static List<String> readPrefixes() {
        Path definition = FabricLoader.getInstance().getModContainer("txoptimizations").orElseThrow().findPath(ATLAS_DEFINITION)
            .orElseThrow(() -> new IllegalStateException("TxOptimizations has no " + ATLAS_DEFINITION));
        List<String> prefixes = new ArrayList<>();

        try (Reader reader = Files.newBufferedReader(definition)) {
            for (JsonElement source : JsonParser.parseReader(reader).getAsJsonObject().getAsJsonArray("sources"))
                prefixes.add(source.getAsJsonObject().get("prefix").getAsString());
        } catch (IOException exception) {
            throw new UncheckedIOException("Could not read " + ATLAS_DEFINITION, exception);
        }

        if (prefixes.isEmpty())
            throw new IllegalStateException(ATLAS_DEFINITION + " adds no sprite folders");

        return List.copyOf(prefixes);
    }
}
