package com.teslicek.txoptimizations.mixin.bake;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.VertexSorting;
import com.teslicek.txoptimizations.bake.Baking;
import com.teslicek.txoptimizations.bake.Cushions;
import java.util.Map;
import net.fabricmc.fabric.api.client.renderer.v1.Renderer;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.QuadEmitter;
import net.fabricmc.fabric.api.client.renderer.v1.render.AltModelBlockRenderer;
import net.minecraft.client.color.block.BlockColors;
import net.minecraft.client.renderer.SectionBufferBuilderPack;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.chunk.RenderSectionRegion;
import net.minecraft.client.renderer.chunk.SectionCompiler;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SectionCompiler.class)
public abstract class SectionCompilerBakeMixin {

    @Shadow
    @Final
    private boolean ambientOcclusion;

    @Shadow
    @Final
    private BlockColors blockColors;

    @Shadow
    protected abstract BufferBuilder getOrBeginLayer(Map<ChunkSectionLayer, BufferBuilder> startedLayers, SectionBufferBuilderPack builders, ChunkSectionLayer layer);

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

    @ModifyExpressionValue(method = "compile", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/block/BlockStateModelSet;get(Lnet/minecraft/world/level/block/state/BlockState;)Lnet/minecraft/client/renderer/block/dispatch/BlockStateModel;"))
    private BlockStateModel txoptimizations$chooseMeshedModel(BlockStateModel model, @Share("blockEntity") LocalRef<BlockEntity> blockEntity) {
        return Baking.meshedModel(blockEntity.get(), model);
    }

    @Inject(method = "compile", at = @At(value = "INVOKE", target = "Ljava/util/Map;entrySet()Ljava/util/Set;"))
    private void txoptimizations$meshCushions(SectionPos section, RenderSectionRegion region, VertexSorting vertexSorting, SectionBufferBuilderPack builders, CallbackInfoReturnable<SectionCompiler.Results> cir, @Local(name = "startedLayers") Map<ChunkSectionLayer, BufferBuilder> startedLayers) {
        if (!Cushions.hasCushions(section))
            return;

        AltModelBlockRenderer renderer = Renderer.get().altModelBlockRenderer(this.ambientOcclusion, true, this.blockColors);
        QuadEmitter           emitter  = Renderer.get().quadEmitter(quad -> quad.buffer(OverlayTexture.NO_OVERLAY, this.getOrBeginLayer(startedLayers, builders, quad.chunkLayer())));
        BlockState            air      = Blocks.AIR.defaultBlockState();

        Cushions.forEachMeshed(section, (pos, model) -> renderer.tesselateBlock(emitter, SectionPos.sectionRelative(pos.getX()), SectionPos.sectionRelative(pos.getY()), SectionPos.sectionRelative(pos.getZ()), region, pos, air, model, air.getSeed(pos)));
    }
}
