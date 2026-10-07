package com.teslicek.txoptimizations.mixin;

import com.teslicek.txoptimizations.TextureManagerVersion;
import net.minecraft.client.renderer.texture.TextureManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TextureManager.class)
public abstract class TextureManagerVersionMixin implements TextureManagerVersion {

    @Unique
    private int txoptimizations$version;

    @Inject(method = {"register", "release", "close"}, at = @At("HEAD"))
    private void txoptimizations$changeTextures(CallbackInfo ci) {
        this.txoptimizations$version ++;
    }

    @Override
    public int txoptimizations$getVersion() {
        return this.txoptimizations$version;
    }
}
