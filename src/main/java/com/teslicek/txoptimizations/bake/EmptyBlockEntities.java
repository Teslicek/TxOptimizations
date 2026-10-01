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

    private static long    textFilteringFrame = -1L;
    private static boolean textFiltering;

    private EmptyBlockEntities() {
    }

    public static boolean isEmpty(BlockEntity blockEntity) {
        return switch (blockEntity) {
            case SignBlockEntity sign -> !hasMessage(sign, SignTextSlot.FRONT) && !hasMessage(sign, SignTextSlot.BACK);
            case BeaconBlockEntity beacon -> beacon.getBeamSections().isEmpty();
            case CampfireBlockEntity campfire -> isEmpty(campfire.getItems());
            case ShelfBlockEntity shelf -> isEmpty(shelf.getItems());
            default -> false;
        };
    }

    private static boolean hasMessage(SignBlockEntity sign, SignTextSlot slot) {
        return ((SignMessageCache) sign.getText(slot)).txoptimizations$hasMessage(isTextFilteringEnabled());
    }

    private static boolean isEmpty(List<ItemStack> items) {
        for (ItemStack item : items)
            if (!item.isEmpty())
                return false;

        return true;
    }

    private static boolean isTextFilteringEnabled() {
        long frame = ClientClock.frame();

        if (frame != textFilteringFrame) {
            textFiltering      = Minecraft.getInstance().isTextFilteringEnabled();
            textFilteringFrame = frame;
        }

        return textFiltering;
    }
}
