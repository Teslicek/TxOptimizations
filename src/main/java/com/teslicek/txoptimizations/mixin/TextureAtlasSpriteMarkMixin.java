package com.teslicek.txoptimizations.mixin;

import com.teslicek.txoptimizations.SpriteMarkVersion;
import net.minecraft.client.renderer.texture.TextureAtlas;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TextureAtlas.class)
public abstract class TextureAtlasSpriteMarkMixin {

    @Inject(method = "cycleAnimationFrames", at = @At("HEAD"))
    private void txoptimizations$invalidateSpriteMarks(CallbackInfo ci) {
        SpriteMarkVersion.bump();
    }
}
