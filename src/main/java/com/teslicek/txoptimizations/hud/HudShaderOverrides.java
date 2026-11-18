package com.teslicek.txoptimizations.hud;

import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.mojang.renderpearl.api.pipeline.ShaderType;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;

public final class HudShaderOverrides {

    private static final String          VANILLA_PACK = "vanilla";
    private static final Set<Identifier> LOADING      = new HashSet<>();

    private static volatile Set<Identifier> overridden = Set.of();
    private static volatile boolean         moved;

    private HudShaderOverrides() {
    }

    public static void startLoading() {
        LOADING.clear();
    }

    public static void collect(Identifier location, Resource resource, ShaderType type) {
        if (type != ShaderType.VERTEX)
            return;

        if (!location.getNamespace().equals(Identifier.DEFAULT_NAMESPACE))
            return;

        if (resource.sourcePackId().equals(VANILLA_PACK))
            return;

        LOADING.add(type.idConverter().fileToId(location));
    }

    public static void finishLoading() {
        overridden = Set.copyOf(LOADING);
        moved      = false;
        LOADING.clear();
    }

    public static void check(RenderPipeline pipeline) {
        if (!moved && overridden.contains(pipeline.getShaders().get(ShaderType.VERTEX)))
            moved = true;
    }

    static boolean isMoved() {
        return moved;
    }
}
