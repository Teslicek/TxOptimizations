package com.teslicek.txoptimizations.bake;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class Baking {

    private static final int REBUILD_FLAGS = 8;

    private Baking() {
    }

    public static boolean shouldRenderEntity(BlockEntity blockEntity) {
        BakeableBlockEntity bakeable = (BakeableBlockEntity) blockEntity;

        return !blockEntity.hasLevel()
            || bakeable.txoptimizations$isForcedEntity()
            || !bakeable.txoptimizations$isBakeSupported()
            || bakeable.txoptimizations$getRenderMode() == RenderMode.ENTITY
            || bakeable.txoptimizations$getPendingMode() == RenderMode.ENTITY
            || bakeable.txoptimizations$isRenderBoth();
    }

    public static boolean isBaked(BlockEntity blockEntity) {
        return blockEntity.hasLevel() && ((BakeableBlockEntity) blockEntity).txoptimizations$isBakeSupported();
    }

    public static boolean isMeshedOnly(BlockEntity blockEntity) {
        BakeableBlockEntity bakeable = (BakeableBlockEntity) blockEntity;

        if (bakeable.txoptimizations$isHidden())
            return true;

        return bakeable.txoptimizations$isBakeSupported()
            && bakeable.txoptimizations$getPendingMode() == RenderMode.TERRAIN
            && !bakeable.txoptimizations$isForcedEntity()
            && !bakeable.txoptimizations$isRenderBoth();
    }

    public static boolean isEntityMeshed(Entity entity) {
        Bakeable bakeable = (Bakeable) entity;

        return bakeable.txoptimizations$isBakeSupported() && bakeable.txoptimizations$getRenderMode() == RenderMode.TERRAIN && !entity.shouldShowName();
    }

    public static void requestMode(BlockEntity blockEntity, RenderMode mode) {
        BakeableBlockEntity bakeable = (BakeableBlockEntity) blockEntity;

        if (bakeable.txoptimizations$getPendingMode() == mode)
            return;

        if (mode == RenderMode.TERRAIN && !canBeTerrain(blockEntity))
            return;

        bakeable.txoptimizations$setPendingMode(mode);
        rebuild(blockEntity.getBlockPos());
    }

    public static boolean canBeTerrain(BlockEntity blockEntity) {
        if (Minecraft.getInstance().level == null)
            return false;

        BlockState state = blockEntity.getBlockState();
        BakedKind  kind  = BakedKind.of(state);

        return kind != null && kind.canBake(state);
    }

    public static void rebuild(BlockPos pos) {
        Minecraft minecraft = Minecraft.getInstance();

        if (minecraft.level == null)
            return;

        if (!minecraft.isSameThread()) {
            minecraft.execute(() -> rebuild(pos));

            return;
        }

        minecraft.levelExtractor.blockChanged(pos, REBUILD_FLAGS);
    }

    public static RenderShape meshedRenderShape(BlockState state, BlockEntity blockEntity, SectionPos section, RenderShape vanilla) {
        if (blockEntity == null || !state.hasBlockEntity())
            return vanilla;

        BakeableBlockEntity bakeable = (BakeableBlockEntity) blockEntity;

        if (!bakeable.txoptimizations$isBakeSupported())
            return vanilla;

        if (bakeable.txoptimizations$isTimerFinished())
            ChunkTasks.add(section, () -> bakeable.txoptimizations$setRenderMode(RenderMode.TERRAIN));

        if (bakeable.txoptimizations$getRenderMode() != bakeable.txoptimizations$getPendingMode())
            ChunkTasks.add(section, () -> bakeable.txoptimizations$setRenderMode(bakeable.txoptimizations$getPendingMode()));

        if (bakeable.txoptimizations$getPendingMode() == RenderMode.TERRAIN && !bakeable.txoptimizations$isForcedEntity())
            return RenderShape.MODEL;

        return vanilla;
    }

    public static BlockStateModel meshedModel(BlockState state, BlockEntity blockEntity, BlockStateModel model) {
        if (blockEntity == null || !state.hasBlockEntity())
            return model;

        BakeableBlockEntity bakeable = (BakeableBlockEntity) blockEntity;

        if (!bakeable.txoptimizations$isBakeSupported())
            return model;

        if (bakeable.txoptimizations$getPendingMode() != RenderMode.TERRAIN || bakeable.txoptimizations$isForcedEntity())
            return BakedModels.vanilla(blockEntity.getBlockState());

        return model;
    }
}
