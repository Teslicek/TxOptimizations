package com.teslicek.txoptimizations;

import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public interface ResolvedItemState {

    boolean txoptimizations$resolvedFor(ItemStack stack, int version, ItemDisplayContext context, Level level, ItemOwner owner, int seed, Object models);

    void txoptimizations$setResolved(ItemStack stack, int version, ItemDisplayContext context, Level level, ItemOwner owner, int seed, Object models);
}
