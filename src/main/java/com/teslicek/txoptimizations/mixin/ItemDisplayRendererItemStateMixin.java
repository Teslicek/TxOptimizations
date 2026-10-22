package com.teslicek.txoptimizations.mixin;

import com.teslicek.txoptimizations.ItemDisplayItemState;
import net.minecraft.client.renderer.entity.DisplayRenderer;
import net.minecraft.client.renderer.entity.state.ItemDisplayEntityRenderState;
import net.minecraft.world.entity.Display;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(DisplayRenderer.ItemDisplayRenderer.class)
public abstract class ItemDisplayRendererItemStateMixin {

    @Inject(method = "extractRenderState(Lnet/minecraft/world/entity/Display$ItemDisplay;Lnet/minecraft/client/renderer/entity/state/ItemDisplayEntityRenderState;F)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Display$ItemDisplay;itemRenderState()Lnet/minecraft/world/entity/Display$ItemDisplay$ItemRenderState;"))
    private void txoptimizations$reuseItemState(Display.ItemDisplay entity, ItemDisplayEntityRenderState state, float partialTicks, CallbackInfo ci) {
        state.item = ((ItemDisplayItemState) entity).txoptimizations$reusedItemState();
    }
}
