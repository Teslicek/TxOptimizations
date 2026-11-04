package com.teslicek.txoptimizations.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.ScreenEffectRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ScreenEffectRenderer.class)
public abstract class ScreenEffectRendererPoseStackMixin {

    @Unique
    private final PoseStack txoptimizations$poseStack = new PoseStack();

    @Redirect(method = "submit", at = @At(value = "NEW", target = "()Lcom/mojang/blaze3d/vertex/PoseStack;"), require = 1)
    private PoseStack txoptimizations$reusePoseStack() {
        PoseStack poseStack = this.txoptimizations$poseStack;

        if (!poseStack.isEmpty())
            throw new IllegalStateException("The screen effect pose stack was left with pushed poses");

        poseStack.setIdentity();

        return poseStack;
    }
}
