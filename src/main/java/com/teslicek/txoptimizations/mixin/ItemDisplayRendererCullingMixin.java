package com.teslicek.txoptimizations.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.teslicek.txoptimizations.DisplayCulling;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(targets = "net.minecraft.client.renderer.entity.DisplayRenderer$ItemDisplayRenderer")
public abstract class ItemDisplayRendererCullingMixin {

    @Redirect(method = "submitInner", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/item/ItemStackRenderState;submit(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;III)V"))
    private void txoptimizations$submitVisibleItem(ItemStackRenderState item, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int lightCoords, int overlayCoords, int outlineColor) {
        if (DisplayCulling.isHidden(item, poseStack.last().pose()))
            return;

        item.submit(poseStack, submitNodeCollector, lightCoords, overlayCoords, outlineColor);
    }
}
