package com.teslicek.txoptimizations.mixin;

import com.teslicek.txoptimizations.ClientClock;
import net.minecraft.client.renderer.rendertype.TextureTransform;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(TextureTransform.class)
public abstract class TextureTransformFrameMillisMixin {

    @Redirect(method = "setupGlintTexturing", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/Util;getMillis()J"))
    private static long txoptimizations$frameMillis() {
        return ClientClock.frameMillis();
    }
}
