package com.teslicek.txoptimizations;

import com.mojang.renderpearl.api.pipeline.ShaderType;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.metadata.MetadataSectionType;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;

public final class AtlasFeatures {

    public static final int FONT_ATLAS_SIZE = 1024;

    private static final Identifier                             TEXT_SHADER           = Identifier.withDefaultNamespace("core/text");
    private static final List<ShaderType>                       TEXT_SHADER_STAGES    = List.of(ShaderType.VERTEX, ShaderType.FRAGMENT);
    private static final String                                 FONT_ATLAS_FEATURE    = "font_atlas_resizing";
    private static final String                                 MAP_ATLAS_FEATURE     = "map_atlas_generation";
    private static final MetadataSectionType<PackCompatibility> COMPATIBILITY_SECTION = new MetadataSectionType<>("immediatelyfast", PackCompatibility.CODEC);

    private static boolean fontAtlasResizing  = true;
    private static boolean mapAtlasGeneration = true;

    private AtlasFeatures() {
    }

    public static boolean isFontAtlasResizing() {
        return fontAtlasResizing;
    }

    public static boolean isMapAtlasGeneration() {
        return mapAtlasGeneration;
    }

    public static void updateFromResourcePacks(ResourceManager resourceManager, PackResources vanillaPack) {
        boolean fontAtlasAllowed = true;
        boolean mapAtlasAllowed  = true;

        for (PackResources pack : textShaderOverrides(resourceManager, vanillaPack)) {
            PackCompatibility compatibility = compatibility(pack);

            if (!compatibility.compatibleFeatures().contains(FONT_ATLAS_FEATURE))
                fontAtlasAllowed = false;

            if (compatibility.incompatibleFeatures().contains(MAP_ATLAS_FEATURE))
                mapAtlasAllowed = false;
        }

        fontAtlasResizing  = fontAtlasAllowed;
        mapAtlasGeneration = mapAtlasAllowed;
    }

    private static Set<PackResources> textShaderOverrides(ResourceManager resourceManager, PackResources vanillaPack) {
        Set<PackResources> overrides = new HashSet<>();

        for (ShaderType stage : TEXT_SHADER_STAGES)
            resourceManager.getResource(stage.idConverter().idToFile(TEXT_SHADER))
                .map(Resource::source)
                .filter(pack -> !pack.location().equals(vanillaPack.location()))
                .ifPresent(overrides::add);

        return overrides;
    }

    private static PackCompatibility compatibility(PackResources pack) {
        try {
            PackCompatibility compatibility = pack.getMetadataSection(COMPATIBILITY_SECTION);

            if (compatibility == null)
                return PackCompatibility.NONE;

            return compatibility;
        } catch (IOException exception) {
            throw new UncheckedIOException("Failed to read the compatibility metadata of resource pack " + pack.packId(), exception);
        }
    }

    private record PackCompatibility(List<String> compatibleFeatures, List<String> incompatibleFeatures) {

        private static final PackCompatibility        NONE  = new PackCompatibility(List.of(), List.of());
        private static final Codec<PackCompatibility> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.listOf().optionalFieldOf("compatible_features", List.of()).forGetter(PackCompatibility::compatibleFeatures),
            Codec.STRING.listOf().optionalFieldOf("incompatible_features", List.of()).forGetter(PackCompatibility::incompatibleFeatures)
        ).apply(instance, PackCompatibility::new));
    }
}
