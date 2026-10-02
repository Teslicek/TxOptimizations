package com.teslicek.txoptimizations.mixin.bake;

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
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractSignRenderer.class)
public abstract class SignTextCullingMixin {

    @Inject(method = "submitSignText", at = @At("HEAD"), cancellable = true)
    private void txoptimizations$skipTextFacingAway(SignRenderState state, PoseStack poseStack, SubmitNodeCollector collector, SignText text, CallbackInfo ci) {
        PoseStack.Pose pose   = poseStack.last();
        Matrix3f       normal = pose.normal();
        Matrix4f       matrix = pose.pose();

        if (Math.fma(normal.m20(), matrix.m30(), Math.fma(normal.m21(), matrix.m31(), normal.m22() * matrix.m32())) < 0.0F)
            return;

        ci.cancel();
    }
}
