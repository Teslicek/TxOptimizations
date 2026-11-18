package com.teslicek.txoptimizations;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.fabricmc.loader.api.FabricLoader;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

public final class TxOptimizationsMixinPlugin implements IMixinConfigPlugin {

    private static final Map<String, List<String>> COMPAT = Map.of(
        "xaerominimap", List.of("compat.XaeroMinimapCoreHudCacheMixin", "compat.XaeroMinimapTickMixin", "compat.XaeroMinimapRenderTickMixin"),
        "xaerolib", List.of("compat.XaeroLibEndFrameMixin")
    );

    @Override
    public void onLoad(String mixinPackage) {
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        return true;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
    }

    @Override
    public List<String> getMixins() {
        List<String> mixins = new ArrayList<>();

        for (Map.Entry<String, List<String>> entry : COMPAT.entrySet()) {
            if (FabricLoader.getInstance().isModLoaded(entry.getKey()))
                mixins.addAll(entry.getValue());
        }

        return mixins;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }
}
