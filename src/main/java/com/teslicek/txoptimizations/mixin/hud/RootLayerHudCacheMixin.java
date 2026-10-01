package com.teslicek.txoptimizations.mixin.hud;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.teslicek.txoptimizations.hud.HudCache;
import com.teslicek.txoptimizations.hud.HudLayerTimer;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.fabricmc.fabric.impl.client.rendering.hud.HudElementRegistryImpl;
import net.fabricmc.fabric.impl.client.rendering.hud.HudLayer;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(HudElementRegistryImpl.RootLayer.class)
public abstract class RootLayerHudCacheMixin {

    @WrapMethod(method = "extractRenderState")
    private void txoptimizations$cacheLayers(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker, HudElement vanillaElement, Operation<Void> original) {
        if (!HudCache.isRendering()) {
            original.call(graphics, deltaTracker, vanillaElement);

            return;
        }

        HudElementRegistryImpl.RootLayer root = (HudElementRegistryImpl.RootLayer) (Object) this;

        if (root != HudElementRegistryAccessor.txoptimizations$first() && root != HudElementRegistryAccessor.txoptimizations$last()) {
            HudLayerTimer layer = HudCache.layer(root.id());

            if (!layer.shouldRender(HudCache.currentPass()))
                return;

            HudCache.begin(layer);
            original.call(graphics, deltaTracker, vanillaElement);
            HudCache.end(layer);

            return;
        }

        for (HudLayer child : root.layers()) {
            HudLayerTimer layer = HudCache.layer(child.id());

            if (child.isRemoved() || !layer.shouldRender(HudCache.currentPass()))
                continue;

            HudCache.begin(layer);
            child.element(vanillaElement).extractRenderState(graphics, deltaTracker);
            HudCache.end(layer);
        }
    }
}
