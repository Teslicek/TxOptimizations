package com.teslicek.txoptimizations.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.teslicek.txoptimizations.ComponentVersion;
import com.teslicek.txoptimizations.ItemModelPurity;
import com.teslicek.txoptimizations.ResolvedItemState;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ItemModelResolver.class)
public abstract class ItemModelResolverLivingCacheMixin {

    @Shadow
    @Final
    private ModelManager modelManager;

    @WrapOperation(method = "updateForLiving(Lnet/minecraft/client/renderer/item/ItemStackRenderState;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemDisplayContext;Lnet/minecraft/world/entity/LivingEntity;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/item/ItemModelResolver;updateForTopItem(Lnet/minecraft/client/renderer/item/ItemStackRenderState;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemDisplayContext;Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/ItemOwner;I)V"), require = 1)
    private void txoptimizations$resolveOnce(ItemModelResolver resolver, ItemStackRenderState state, ItemStack stack, ItemDisplayContext context, Level level, ItemOwner owner, int seed, Operation<Void> original) {
        if (stack.isEmpty()) {
            original.call(resolver, state, stack, context, level, owner, seed);
            return;
        }

        ResolvedItemState resolved = (ResolvedItemState) state;
        Object            models   = this.modelManager.bakedItemStackModels;
        int               version  = ((ComponentVersion) stack.getComponents()).txoptimizations$version();

        if (resolved.txoptimizations$resolvedFor(stack, version, context, level, owner, seed, models))
            return;

        ItemModelPurity.begin();

        boolean pure = false;

        try {
            original.call(resolver, state, stack, context, level, owner, seed);
        } finally {
            pure = ItemModelPurity.end();
        }

        if (pure)
            resolved.txoptimizations$setResolved(stack, version, context, level, owner, seed, models);
    }
}
