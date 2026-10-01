package com.teslicek.txoptimizations.mixin.bake;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.AbstractSignRenderer;
import net.minecraft.client.renderer.blockentity.state.SignRenderState;
import net.minecraft.world.level.block.entity.SignText;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractSignRenderer.class)
public abstract class SignTextCullingMixin {

    @Inject(method = "submitSignText", at = @At("HEAD"), cancellable = true)
    private void txoptimizations$skipTextFacingAway(SignRenderState state, PoseStack poseStack, SubmitNodeCollector collector, SignText text, CallbackInfo ci) {
        PoseStack.Pose pose     = poseStack.last();
        Vector3f       forward  = pose.normal().getColumn(2, new Vector3f());
        Vector3f       position = pose.pose().transformPosition(0.0F, 0.0F, 0.0F, new Vector3f());

        if (forward.dot(position) < 0.0F)
            return;

        ci.cancel();
    }
}
