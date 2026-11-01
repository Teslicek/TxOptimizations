package com.teslicek.txoptimizations.mixin;

import com.teslicek.txoptimizations.RenderStateBuffers;
import net.minecraft.client.gui.screens.inventory.CartographyTableScreen;
import net.minecraft.client.renderer.state.MapRenderState;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(CartographyTableScreen.class)
public abstract class CartographyMapStateMixin {

    @Unique
    private final MapRenderState txoptimizations$otherMapState = new MapRenderState();

    @Redirect(method = "extractMap", at = @At(value = "FIELD", target = "Lnet/minecraft/client/gui/screens/inventory/CartographyTableScreen;mapRenderState:Lnet/minecraft/client/renderer/state/MapRenderState;", opcode = Opcodes.GETFIELD), require = 2)
    private MapRenderState txoptimizations$frameMapState(CartographyTableScreen screen) {
        return RenderStateBuffers.parity() == 0 ? ((CartographyMapStateAccessor) screen).txoptimizations$mapRenderState() : this.txoptimizations$otherMapState;
    }
}
