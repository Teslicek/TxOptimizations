package com.teslicek.txoptimizations.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.teslicek.txoptimizations.GuiItemCache;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.item.TrackingItemStackRenderState;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(GuiGraphicsExtractor.class)
public abstract class GuiGraphicsExtractorItemCacheMixin {

    @WrapOperation(method = "item(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/level/Level;Lnet/minecraft/world/item/ItemStack;III)V", at = @At(value = "NEW", target = "()Lnet/minecraft/client/renderer/item/TrackingItemStackRenderState;"), require = 1)
    private TrackingItemStackRenderState txoptimizations$cachedState(Operation<TrackingItemStackRenderState> original, @Local(argsOnly = true) LivingEntity owner, @Local(argsOnly = true) Level level, @Local(argsOnly = true) ItemStack stack, @Local(argsOnly = true, ordinal = 2) int seed) {
        return GuiItemCache.state(owner, level, stack, seed);
    }

    @WrapOperation(method = "item(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/level/Level;Lnet/minecraft/world/item/ItemStack;III)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/item/ItemModelResolver;updateForTopItem(Lnet/minecraft/client/renderer/item/ItemStackRenderState;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemDisplayContext;Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/LivingEntity;I)V"), require = 1)
    private void txoptimizations$resolveOnce(ItemModelResolver resolver, ItemStackRenderState state, ItemStack stack, ItemDisplayContext context, Level level, LivingEntity owner, int seed, Operation<Void> original) {
        GuiItemCache.resolve(resolver, (TrackingItemStackRenderState) state, stack, context, level, owner, seed);
    }
}
