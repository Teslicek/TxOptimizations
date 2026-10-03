package com.teslicek.txoptimizations.mixin.bake;

import com.teslicek.txoptimizations.bake.BakeableBlockEntity;
import com.teslicek.txoptimizations.bake.BakedKind;
import com.teslicek.txoptimizations.bake.Baking;
import com.teslicek.txoptimizations.bake.RenderMode;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.LecternBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.BlockEntityTypes;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BlockEntity.class)
public abstract class BlockEntityBakeStateMixin implements BakeableBlockEntity {

    @Unique
    private RenderMode txoptimizations$renderMode = RenderMode.ENTITY;

    @Unique
    private RenderMode txoptimizations$pendingMode = RenderMode.TERRAIN;

    @Unique
    private boolean txoptimizations$bakeSupported;

    @Unique
    private boolean txoptimizations$renderBoth;

    @Unique
    private boolean txoptimizations$hidden;

    @Unique
    private boolean txoptimizations$forcedEntity;

    @Unique
    private long txoptimizations$emptyTick = -1L;

    @Unique
    private boolean txoptimizations$empty;

    @Shadow
    public abstract BlockEntityType<?> getType();

    @Inject(method = "<init>", at = @At("TAIL"))
    private void txoptimizations$initBakeState(BlockEntityType<?> type, BlockPos pos, BlockState state, CallbackInfo ci) {
        if (type == BlockEntityTypes.LECTERN)
            this.txoptimizations$hideBooklessLectern(state);

        if (BakedKind.of(type) == null)
            return;

        this.txoptimizations$bakeSupported = true;
        this.txoptimizations$renderBoth    = type == BlockEntityTypes.BANNER;

        if (!Baking.canBeTerrain((BlockEntity) (Object) this))
            this.txoptimizations$pendingMode = RenderMode.ENTITY;
    }

    @Inject(method = "setBlockState", at = @At("TAIL"))
    private void txoptimizations$updateLectern(BlockState state, CallbackInfo ci) {
        if (this.getType() != BlockEntityTypes.LECTERN)
            return;

        this.txoptimizations$hideBooklessLectern(state);
    }

    @Override
    public boolean txoptimizations$isBakeSupported() {
        return this.txoptimizations$bakeSupported;
    }

    @Override
    public RenderMode txoptimizations$getRenderMode() {
        return this.txoptimizations$renderMode;
    }

    @Override
    public void txoptimizations$setRenderMode(RenderMode mode) {
        this.txoptimizations$renderMode  = mode;
        this.txoptimizations$pendingMode = mode;
    }

    @Override
    public RenderMode txoptimizations$getPendingMode() {
        return this.txoptimizations$pendingMode;
    }

    @Override
    public void txoptimizations$setPendingMode(RenderMode mode) {
        this.txoptimizations$pendingMode = mode;
    }

    @Override
    public boolean txoptimizations$isRenderBoth() {
        return this.txoptimizations$renderBoth;
    }

    @Override
    public boolean txoptimizations$isHidden() {
        return this.txoptimizations$hidden;
    }

    @Override
    public boolean txoptimizations$isForcedEntity() {
        return this.txoptimizations$forcedEntity;
    }

    @Override
    public void txoptimizations$setForcedEntity(boolean forcedEntity) {
        this.txoptimizations$forcedEntity = forcedEntity;
    }

    @Override
    public long txoptimizations$getEmptyTick() {
        return this.txoptimizations$emptyTick;
    }

    @Override
    public boolean txoptimizations$isEmpty() {
        return this.txoptimizations$empty;
    }

    @Override
    public void txoptimizations$setEmpty(long tick, boolean empty) {
        this.txoptimizations$emptyTick = tick;
        this.txoptimizations$empty     = empty;
    }

    @Unique
    private void txoptimizations$hideBooklessLectern(BlockState state) {
        if (!Minecraft.getInstance().isSameThread())
            return;

        this.txoptimizations$hidden = !state.getValueOrElse(LecternBlock.HAS_BOOK, true);
    }
}
