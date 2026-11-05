package com.teslicek.txoptimizations;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackSelectionConfig;
import net.minecraft.server.packs.PathPackResources;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackCompatibility;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.server.packs.repository.RepositorySource;
import net.minecraft.world.flag.FeatureFlagSet;

public final class ModResourcePack {

    private static final String MOD_ID = "txoptimizations";

    private ModResourcePack() {
    }

    public static RepositorySource[] withSource(RepositorySource[] vanilla) {
        RepositorySource[] sources = new RepositorySource[vanilla.length + 1];

        sources[0] = ModResourcePack::loadPacks;
        System.arraycopy(vanilla, 0, sources, 1, vanilla.length);

        return sources;
    }

    private static void loadPacks(Consumer<Pack> packs) {
        ModContainer     mod      = FabricLoader.getInstance().getModContainer(MOD_ID).orElseThrow(() -> new IllegalStateException("The " + MOD_ID + " mod container is missing"));
        Path             root     = assetsRoot(mod.getRootPaths());
        PackLocationInfo location = new PackLocationInfo(MOD_ID, Component.literal(mod.getMetadata().getName()), PackSource.BUILT_IN, Optional.empty());
        Pack.Metadata    metadata = new Pack.Metadata(Component.literal(mod.getMetadata().getDescription()), PackCompatibility.COMPATIBLE, FeatureFlagSet.of(), List.of());

        packs.accept(new Pack(location, new PathPackResources.PathResourcesSupplier(root), metadata, new PackSelectionConfig(true, Pack.Position.BOTTOM, true)));
    }

    private static Path assetsRoot(List<Path> roots) {
        for (Path root : roots) {
            if (Files.isDirectory(root.resolve("assets").resolve(MOD_ID)))
                return root;
        }

        throw new IllegalStateException("No root of the " + MOD_ID + " mod holds assets/" + MOD_ID + ": " + roots);
    }
}
