package com.teslicek.txoptimizations.mixin;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.ObjectSets;
import java.util.Set;
import net.minecraft.core.Holder;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(EnchantmentHelper.class)
public abstract class EnchantmentHelperEmptyIterationMixin {

    @Redirect(method = "runIterationOnItem(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/enchantment/EnchantmentHelper$EnchantmentVisitor;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/enchantment/ItemEnchantments;entrySet()Ljava/util/Set;"))
    private static Set<Object2IntMap.Entry<Holder<Enchantment>>> txoptimizations$skipEmptyEntries(ItemEnchantments enchantments) {
        if (enchantments.isEmpty())
            return ObjectSets.emptySet();

        return enchantments.entrySet();
    }
}
