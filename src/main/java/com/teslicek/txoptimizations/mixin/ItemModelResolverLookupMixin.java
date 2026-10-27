package com.teslicek.txoptimizations.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import com.teslicek.txoptimizations.ItemModelLookup;
import net.minecraft.client.renderer.item.ClientItem;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ItemModelResolver.class)
public abstract class ItemModelResolverLookupMixin {

    @Shadow
    @Final
    private ModelManager modelManager;

    @Redirect(method = "appendItemLayers", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/item/ItemModelResolver;getItemProperties(Lnet/minecraft/resources/Identifier;)Lnet/minecraft/client/renderer/item/ClientItem$Properties;"))
    private ClientItem.Properties txoptimizations$cachedProperties(ItemModelResolver resolver, Identifier modelId, @Local(argsOnly = true) ItemStack item) {
        return ItemModelLookup.properties(this.modelManager, item.getItem(), modelId);
    }

    @Redirect(method = "appendItemLayers", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/item/ItemModelResolver;getItemModel(Lnet/minecraft/resources/Identifier;)Lnet/minecraft/client/renderer/item/ItemModel;"))
    private ItemModel txoptimizations$cachedModel(ItemModelResolver resolver, Identifier modelId, @Local(argsOnly = true) ItemStack item) {
        return ItemModelLookup.model(this.modelManager, item.getItem(), modelId);
    }
}
