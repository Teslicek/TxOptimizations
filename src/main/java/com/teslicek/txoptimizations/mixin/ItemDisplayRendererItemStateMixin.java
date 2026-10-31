package com.teslicek.txoptimizations.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.teslicek.txoptimizations.ItemDisplayItemState;
import com.teslicek.txoptimizations.ItemModelPurity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.DisplayRenderer;
import net.minecraft.client.renderer.entity.state.ItemDisplayEntityRenderState;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
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

    @WrapOperation(method = "extractRenderState(Lnet/minecraft/world/entity/Display$ItemDisplay;Lnet/minecraft/client/renderer/entity/state/ItemDisplayEntityRenderState;F)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/item/ItemModelResolver;updateForNonLiving(Lnet/minecraft/client/renderer/item/ItemStackRenderState;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemDisplayContext;Lnet/minecraft/world/entity/Entity;)V"), require = 1)
    private void txoptimizations$resolveOnce(ItemModelResolver resolver, ItemStackRenderState itemState, ItemStack stack, ItemDisplayContext context, Entity owner, Operation<Void> original) {
        Display.ItemDisplay  display = (Display.ItemDisplay) owner;
        ItemDisplayItemState cache   = (ItemDisplayItemState) display;
        Object               item    = display.itemRenderState();
        Object               level   = display.level();
        Object               models  = Minecraft.getInstance().getModelManager().bakedItemStackModels;

        if (itemState == cache.txoptimizations$reusedItemState() && cache.txoptimizations$resolvedFor(item, level, models))
            return;

        ItemModelPurity.begin();

        boolean pure = false;

        try {
            original.call(resolver, itemState, stack, context, owner);
        } finally {
            pure = ItemModelPurity.end();
        }

        cache.txoptimizations$setResolved(item, level, models, pure && itemState == cache.txoptimizations$reusedItemState());
    }
}
