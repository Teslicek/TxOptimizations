package com.teslicek.txoptimizations.mixin;

import com.teslicek.txoptimizations.OrientationMemo;
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

    @Redirect(method = "submit", at = @At(value = "NEW", target = "()Lorg/joml/Quaternionf;"), require = 1)
    private Quaternionf txoptimizations$reuseOrientation() {
        return RenderThreadStack.isOwner() ? ORIENTATION : new Quaternionf();
    }

    @Redirect(method = "calculateOrientation", at = @At(value = "INVOKE", target = "Lorg/joml/Quaternionf;rotationYXZ(FFF)Lorg/joml/Quaternionf;"), require = 4)
    private Quaternionf txoptimizations$memoizedOrientation(Quaternionf target, float angleY, float angleX, float angleZ) {
        return OrientationMemo.rotationYXZ(target, angleY, angleX, angleZ);
    }
}
