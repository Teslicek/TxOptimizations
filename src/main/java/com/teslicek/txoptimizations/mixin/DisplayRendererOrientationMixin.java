package com.teslicek.txoptimizations.mixin;

import com.teslicek.txoptimizations.RenderThreadStack;
import net.minecraft.client.renderer.entity.DisplayRenderer;
import org.joml.Quaternionf;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(DisplayRenderer.class)
public abstract class DisplayRendererOrientationMixin {

    @Unique
    private static final Quaternionf ORIENTATION = new Quaternionf();

    @Redirect(method = "submit(Lnet/minecraft/client/renderer/entity/state/DisplayEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V", at = @At(value = "NEW", target = "()Lorg/joml/Quaternionf;"))
    private Quaternionf txoptimizations$reuseOrientation() {
        return RenderThreadStack.isOwner() ? ORIENTATION.identity() : new Quaternionf();
    }
}
