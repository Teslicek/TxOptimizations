package com.teslicek.txoptimizations.mixin.bake;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.AbstractSignRenderer;
import net.minecraft.client.renderer.blockentity.state.SignRenderState;
import net.minecraft.world.level.block.entity.SignText;
import org.joml.Math;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(AbstractSignRenderer.class)
public abstract class SignTextCullingMixin {

    @WrapWithCondition(method = "submit(Lnet/minecraft/client/renderer/blockentity/state/SignRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/blockentity/AbstractSignRenderer;submitSignText(Lnet/minecraft/client/renderer/blockentity/state/SignRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/world/level/block/entity/SignText;)V"))
    private boolean txoptimizations$skipTextFacingAway(AbstractSignRenderer<?> renderer, SignRenderState state, PoseStack poseStack, SubmitNodeCollector collector, SignText text) {
        PoseStack.Pose pose   = poseStack.last();
        Matrix3f       normal = pose.normal();
        Matrix4f       matrix = pose.pose();

        return Math.fma(normal.m20(), matrix.m30(), Math.fma(normal.m21(), matrix.m31(), normal.m22() * matrix.m32())) < 0.0F;
    }
}
