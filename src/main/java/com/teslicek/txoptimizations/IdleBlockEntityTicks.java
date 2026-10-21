package com.teslicek.txoptimizations;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BaseSpawner;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.ChestLidController;
import net.minecraft.world.level.block.entity.EnderChestBlockEntity;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.level.block.entity.TrappedChestBlockEntity;

public final class IdleBlockEntityTicks {

    private IdleBlockEntityTicks() {
    }

    public static boolean isIdle(Level level, BlockEntity entity) {
        if (!level.isClientSide())
            return false;

        Class<?> type = entity.getClass();

        if (type == ChestBlockEntity.class || type == TrappedChestBlockEntity.class)
            return isResting(((ChestBlockEntity) entity).chestLidController);

        if (type == EnderChestBlockEntity.class)
            return isResting(((EnderChestBlockEntity) entity).chestLidController);

        if (type == SpawnerBlockEntity.class)
            return isStillAndAlone(level, entity.getBlockPos(), ((SpawnerBlockEntity) entity).getSpawner());

        return false;
    }

    private static boolean isResting(ChestLidController lid) {
        boolean moving = lid.shouldBeOpen ? lid.openness < 1.0F : lid.openness > 0.0F;

        return !moving && Float.floatToRawIntBits(lid.oOpenness) == Float.floatToRawIntBits(lid.openness);
    }

    private static boolean isStillAndAlone(Level level, BlockPos pos, BaseSpawner spawner) {
        return Double.doubleToRawLongBits(spawner.oSpin) == Double.doubleToRawLongBits(spawner.spin) && !spawner.isNearPlayer(level, pos);
    }
}
