package com.teslicek.txoptimizations.mixin.bake;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import com.teslicek.txoptimizations.bake.Baking;
import com.teslicek.txoptimizations.bake.Cushions;
import net.minecraft.client.renderer.block.BlockQuadOutput;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.chunk.RenderSectionRegion;
import net.minecraft.client.renderer.chunk.SectionCompiler;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SectionCompiler.class)
public abstract class SectionCompilerBakeMixin {

    @ModifyExpressionValue(method = "compile", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/chunk/RenderSectionRegion;getBlockState(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/state/BlockState;"))
    private BlockState txoptimizations$forgetBlockEntity(BlockState state, @Share("blockEntity") LocalRef<BlockEntity> blockEntity) {
        blockEntity.set(null);

        return state;
    }

    @ModifyExpressionValue(method = "compile", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/chunk/RenderSectionRegion;getBlockEntity(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/entity/BlockEntity;"))
    private BlockEntity txoptimizations$captureBlockEntity(BlockEntity found, @Share("blockEntity") LocalRef<BlockEntity> blockEntity) {
        blockEntity.set(found);

        return found;
    }

    @WrapWithCondition(method = "compile", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/chunk/SectionCompiler;handleBlockEntity(Lnet/minecraft/client/renderer/chunk/SectionCompiler$Results;Lnet/minecraft/world/level/block/entity/BlockEntity;)V"))
    private boolean txoptimizations$skipMeshedBlockEntities(SectionCompiler compiler, SectionCompiler.Results results, BlockEntity blockEntity) {
        return !Baking.isMeshedOnly(blockEntity);
    }

    @ModifyExpressionValue(method = "compile", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/state/BlockState;getRenderShape()Lnet/minecraft/world/level/block/RenderShape;"))
    private RenderShape txoptimizations$meshBakedBlockEntities(RenderShape shape, @Local(argsOnly = true) SectionPos section, @Share("blockEntity") LocalRef<BlockEntity> blockEntity) {
        return Baking.meshedRenderShape(blockEntity.get(), section, shape);
    }

    @ModifyArg(method = "compile", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/block/ModelBlockRenderer;tesselateBlock(Lnet/minecraft/client/renderer/block/BlockQuadOutput;FFFLnet/minecraft/client/renderer/block/BlockAndTintGetter;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/client/renderer/block/dispatch/BlockStateModel;J)V"), index = 7)
    private BlockStateModel txoptimizations$chooseMeshedModel(BlockStateModel model, @Share("blockEntity") LocalRef<BlockEntity> blockEntity) {
        return Baking.meshedModel(blockEntity.get(), model);
    }

    @Inject(method = "compile", at = @At(value = "INVOKE", target = "Ljava/util/Map;entrySet()Ljava/util/Set;"))
    private void txoptimizations$meshCushions(CallbackInfoReturnable<SectionCompiler.Results> cir, @Local(argsOnly = true) SectionPos section, @Local(argsOnly = true) RenderSectionRegion region, @Local ModelBlockRenderer blockRenderer, @Local(ordinal = 0) BlockQuadOutput quadOutput) {
        if (!Cushions.hasCushions(section))
            return;

        BlockState air = Blocks.AIR.defaultBlockState();

        Cushions.forEachMeshed(section, (pos, model) -> blockRenderer.tesselateBlock(quadOutput, SectionPos.sectionRelative(pos.getX()), SectionPos.sectionRelative(pos.getY()), SectionPos.sectionRelative(pos.getZ()), region, pos, air, model, air.getSeed(pos)));
    }
}
