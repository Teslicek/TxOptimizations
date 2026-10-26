package com.teslicek.txoptimizations.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import com.teslicek.txoptimizations.DisplayCulling;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.TextDisplayEntityRenderState;
import net.minecraft.world.entity.Display;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net.minecraft.client.renderer.entity.DisplayRenderer$TextDisplayRenderer")
public abstract class TextDisplayRendererCullingMixin {

    @Shadow
    @Final
    private Font font;

    @Inject(method = "submitInner", at = @At(value = "INVOKE", target = "Lorg/joml/Matrix4f;translate(FFF)Lorg/joml/Matrix4f;", shift = At.Shift.AFTER), cancellable = true)
    private void txoptimizations$skipHiddenText(TextDisplayEntityRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int lightCoords, float interpolationProgress, CallbackInfo ci, @Local(name = "pose") Matrix4f pose, @Local(name = "backgroundColor") int backgroundColor, @Local(name = "width") int width, @Local(name = "height") int height, @Local(name = "shadow") boolean shadow, @Local(name = "alignment") Display.TextDisplay.Align alignment, @Local(name = "textOpacity") byte textOpacity) {
        if (DisplayCulling.isTextHidden(this.font, pose, state.cachedInfo, backgroundColor, width, height, shadow, alignment, textOpacity << 24 | 0xFFFFFF))
            ci.cancel();
    }
}
