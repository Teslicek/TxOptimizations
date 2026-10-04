package com.teslicek.txoptimizations.mixin.bake;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import com.teslicek.txoptimizations.bake.Baking;
import com.teslicek.txoptimizations.bake.Cushions;
import net.caffeinemc.mods.sodium.client.render.chunk.compile.ChunkBuildOutput;
import net.caffeinemc.mods.sodium.client.render.chunk.compile.pipeline.BlockRenderer;
import net.caffeinemc.mods.sodium.client.render.chunk.compile.tasks.ChunkBuilderMeshingTask;
import net.caffeinemc.mods.sodium.client.world.LevelSlice;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ChunkBuilderMeshingTask.class)
public abstract class ChunkBuilderMeshingTaskBakeMixin {

    @Unique
    private static final int SECTION_MASK = 15;

    @Unique
    private final SectionPos txoptimizations$section = ((ChunkBuilderMeshingTask) (Object) this).getRenderSection().getPosition();

    @WrapOperation(method = "execute", at = @At(value = "INVOKE", target = "Lnet/caffeinemc/mods/sodium/client/world/LevelSlice;getBlockState(III)Lnet/minecraft/world/level/block/state/BlockState;"))
    private BlockState txoptimizations$captureBlockEntity(LevelSlice slice, int x, int y, int z, Operation<BlockState> original, @Share("blockEntity") LocalRef<BlockEntity> blockEntity) {
        BlockState state = original.call(slice, x, y, z);

        blockEntity.set(state.hasBlockEntity() ? slice.getBlockEntity(x, y, z) : null);

        return state;
    }

    @WrapOperation(method = "execute", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/state/BlockState;getRenderShape()Lnet/minecraft/world/level/block/RenderShape;"))
    private RenderShape txoptimizations$meshBakedBlockEntities(BlockState state, Operation<RenderShape> original, @Share("blockEntity") LocalRef<BlockEntity> blockEntity) {
        return Baking.meshedRenderShape(blockEntity.get(), this.txoptimizations$section, original.call(state));
    }

    @ModifyArg(method = "execute", at = @At(value = "INVOKE", target = "Lnet/caffeinemc/mods/sodium/client/render/chunk/compile/pipeline/BlockRenderer;renderModel(Lnet/minecraft/client/renderer/block/dispatch/BlockStateModel;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/BlockPos;)V"), index = 0)
    private BlockStateModel txoptimizations$chooseMeshedModel(BlockStateModel model, @Share("blockEntity") LocalRef<BlockEntity> blockEntity) {
        return Baking.meshedModel(blockEntity.get(), model);
    }

    @WrapOperation(method = "execute", at = @At(value = "INVOKE", target = "Lnet/caffeinemc/mods/sodium/client/render/chunk/ExtendedBlockEntityType;shouldRender(Lnet/minecraft/world/level/block/entity/BlockEntityType;Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/entity/BlockEntity;)Z"))
    private boolean txoptimizations$skipMeshedBlockEntities(BlockEntityType<?> type, BlockGetter slice, BlockPos pos, BlockEntity blockEntity, Operation<Boolean> original) {
        if (blockEntity != null && Baking.isMeshedOnly(blockEntity))
            return false;

        return original.call(type, slice, pos, blockEntity);
    }

    @Inject(method = "execute", at = @At(value = "INVOKE", target = "Lnet/caffeinemc/mods/sodium/client/render/chunk/compile/pipeline/BlockRenderer;release()V"))
    private void txoptimizations$meshCushions(CallbackInfoReturnable<ChunkBuildOutput> cir, @Local BlockRenderer renderer) {
        if (!Cushions.hasCushions(this.txoptimizations$section))
            return;

        BlockState               air    = Blocks.AIR.defaultBlockState();
        BlockPos.MutableBlockPos offset = new BlockPos.MutableBlockPos();

        Cushions.forEachMeshed(this.txoptimizations$section, (pos, model) -> {
            offset.set(pos.getX() & SECTION_MASK, pos.getY() & SECTION_MASK, pos.getZ() & SECTION_MASK);
            renderer.renderModel(model, air, pos, offset);
        });
    }
}
