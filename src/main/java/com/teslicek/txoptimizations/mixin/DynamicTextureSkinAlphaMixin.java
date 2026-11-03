package com.teslicek.txoptimizations.mixin;

import com.teslicek.txoptimizations.SkinAlpha;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.DynamicTexture;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(DynamicTexture.class)
public abstract class DynamicTextureSkinAlphaMixin extends AbstractTexture {

    @Inject(method = "close", at = @At("HEAD"))
    private void txoptimizations$forgetSkinAlpha(CallbackInfo ci) {
        SkinAlpha.forget(this.textureView);
    }
}
