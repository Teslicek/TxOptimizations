package com.teslicek.txoptimizations.bake;

import com.teslicek.txoptimizations.ClientClock;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BeaconBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.CampfireBlockEntity;
import net.minecraft.world.level.block.entity.ShelfBlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignTextSlot;

public final class EmptyBlockEntities {

    private static long    textFilteringTick = -1L;
    private static boolean textFiltering;

    private EmptyBlockEntities() {
    }

    public static boolean isEmpty(BlockEntity blockEntity) {
        if (blockEntity instanceof SignBlockEntity sign)
            return !hasMessage(sign, SignTextSlot.FRONT) && !hasMessage(sign, SignTextSlot.BACK);

        if (blockEntity instanceof CampfireBlockEntity campfire)
            return isEmpty(campfire.getItems());

        if (blockEntity instanceof ShelfBlockEntity shelf)
            return isEmpty(shelf.getItems());

        if (blockEntity instanceof BeaconBlockEntity beacon)
            return beacon.getBeamSections().isEmpty();

        return false;
    }

    private static boolean hasMessage(SignBlockEntity sign, SignTextSlot slot) {
        return ((SignMessageCache) sign.getText(slot)).txoptimizations$hasMessage(isTextFilteringEnabled());
    }

    private static boolean isEmpty(List<ItemStack> items) {
        for (int i = 0; i < items.size(); i ++)
            if (!items.get(i).isEmpty())
                return false;

        return true;
    }

    private static boolean isTextFilteringEnabled() {
        long tick = ClientClock.tick();

        if (tick != textFilteringTick) {
            textFiltering     = Minecraft.getInstance().isTextFilteringEnabled();
            textFilteringTick = tick;
        }

        return textFiltering;
    }
}
