package com.teslicek.txoptimizations.mixin;

import com.teslicek.txoptimizations.ParallelFeatures;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(TextureManager.class)
public abstract class TextureManagerParallelMixin {

    @Inject(method = "getTexture(Lnet/minecraft/resources/Identifier;)Lnet/minecraft/client/renderer/texture/AbstractTexture;", at = @At("HEAD"))
    private void txoptimizations$lock(Identifier location, CallbackInfoReturnable<AbstractTexture> cir) {
        ParallelFeatures.lock();
    }

    @Inject(method = "getTexture(Lnet/minecraft/resources/Identifier;)Lnet/minecraft/client/renderer/texture/AbstractTexture;", at = @At("RETURN"))
    private void txoptimizations$unlock(Identifier location, CallbackInfoReturnable<AbstractTexture> cir) {
        ParallelFeatures.unlock();
    }
}
