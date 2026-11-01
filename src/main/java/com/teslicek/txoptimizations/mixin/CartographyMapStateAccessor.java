package com.teslicek.txoptimizations.mixin;

import net.minecraft.client.gui.screens.inventory.CartographyTableScreen;
import net.minecraft.client.renderer.state.MapRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(CartographyTableScreen.class)
public interface CartographyMapStateAccessor {

    @Accessor("mapRenderState")
    MapRenderState txoptimizations$mapRenderState();
}
