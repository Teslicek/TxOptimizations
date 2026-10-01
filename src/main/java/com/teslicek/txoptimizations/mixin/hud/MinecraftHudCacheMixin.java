package com.teslicek.txoptimizations.mixin.hud;

import com.teslicek.txoptimizations.hud.HudCache;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.impl.client.rendering.hud.HudElementRegistryImpl;
import net.fabricmc.fabric.impl.client.rendering.hud.HudLayer;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public abstract class MinecraftHudCacheMixin {

    @Inject(method = "onGameLoadFinished", at = @At("TAIL"))
    private void txoptimizations$registerHudLayers(CallbackInfo ci) {
        HudElementRegistryImpl.RootLayer first = HudElementRegistryAccessor.txoptimizations$first();
        HudElementRegistryImpl.RootLayer last  = HudElementRegistryAccessor.txoptimizations$last();
        List<Identifier>                 ids   = new ArrayList<>();

        first.layers().stream().map(HudLayer::id).forEach(ids::add);
        HudElementRegistryAccessor.txoptimizations$vanillaElementIds().stream().filter(id -> !id.equals(first.id()) && !id.equals(last.id())).forEach(ids::add);
        last.layers().stream().map(HudLayer::id).forEach(ids::add);

        HudCache.registerLayers(ids);
    }

    @Inject(method = "setLevel", at = @At("TAIL"))
    private void txoptimizations$resetHudCache(CallbackInfo ci) {
        HudCache.reset();
    }
}
