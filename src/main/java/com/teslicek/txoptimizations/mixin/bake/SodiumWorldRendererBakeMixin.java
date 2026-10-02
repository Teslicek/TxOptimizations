package com.teslicek.txoptimizations.mixin.bake;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.mojang.blaze3d.vertex.PoseStack;
import com.teslicek.txoptimizations.bake.EmptyBlockEntities;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import java.util.SortedSet;
import net.caffeinemc.mods.sodium.client.render.SodiumWorldRenderer;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import net.minecraft.server.level.BlockDestructionProgress;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(SodiumWorldRenderer.class)
public abstract class SodiumWorldRendererBakeMixin {

    @WrapWithCondition(method = "extractBlockEntities", at = @At(value = "INVOKE", target = "Lnet/caffeinemc/mods/sodium/client/render/SodiumWorldRenderer;extractBlockEntity(Lnet/minecraft/world/level/block/entity/BlockEntity;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/Camera;FLit/unimi/dsi/fastutil/longs/Long2ObjectMap;Lnet/minecraft/client/renderer/state/level/LevelRenderState;Z)V"))
    private boolean txoptimizations$skipEmptyBlockEntities(SodiumWorldRenderer renderer, BlockEntity blockEntity, PoseStack poseStack, Camera camera, float tickDelta, Long2ObjectMap<SortedSet<BlockDestructionProgress>> progression, LevelRenderState levelRenderState, boolean global) {
        return !EmptyBlockEntities.isEmpty(blockEntity);
    }
}
