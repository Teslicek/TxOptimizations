package com.teslicek.txoptimizations.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.platform.NativeImage;
import com.teslicek.txoptimizations.SkinAlpha;
import net.minecraft.core.ClientAsset;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.SkinTextureDownloader;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SkinTextureDownloader.class)
public abstract class SkinTextureDownloaderAlphaMixin {

    @Inject(method = "lambda$registerTextureInManager$0", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/texture/TextureManager;register(Lnet/minecraft/resources/Identifier;Lnet/minecraft/client/renderer/texture/AbstractTexture;)V", shift = At.Shift.AFTER), require = 1)
    private void txoptimizations$rememberSkinAlpha(ClientAsset.Texture textureId, NativeImage contents, CallbackInfoReturnable<ClientAsset.Texture> cir, @Local DynamicTexture texture) {
        SkinAlpha.register(texture);
    }
}
