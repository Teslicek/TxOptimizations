package com.teslicek.txoptimizations.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(GameRenderer.class)
public interface GameRendererBobAccessor {

    @Invoker("bobHurt")
    void txoptimizations$bobHurt(CameraRenderState cameraState, PoseStack poseStack);

    @Invoker("bobView")
    void txoptimizations$bobView(CameraRenderState cameraState, PoseStack poseStack);
}
