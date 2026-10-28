package com.teslicek.txoptimizations.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import com.teslicek.txoptimizations.ShelfItemStates;
import net.minecraft.client.renderer.blockentity.ShelfRenderer;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.ShelfBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ShelfRenderer.class)
public abstract class ShelfRendererItemStateMixin {

    @Redirect(method = "extractRenderState(Lnet/minecraft/world/level/block/entity/ShelfBlockEntity;Lnet/minecraft/client/renderer/blockentity/state/ShelfRenderState;FLnet/minecraft/world/phys/Vec3;Lnet/minecraft/client/renderer/feature/ModelFeatureRenderer$CrumblingOverlay;)V", at = @At(value = "NEW", target = "()Lnet/minecraft/client/renderer/item/ItemStackRenderState;"))
    private ItemStackRenderState txoptimizations$reuseItemState(@Local(argsOnly = true) ShelfBlockEntity shelf, @Local NonNullList<ItemStack> items, @Local(ordinal = 1) int slot) {
        return ((ShelfItemStates) shelf).txoptimizations$itemState(slot, items.size());
    }
}
