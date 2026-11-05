package com.teslicek.txoptimizations.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.teslicek.txoptimizations.ParallelFeatures;
import com.teslicek.txoptimizations.TextureManagerVersion;
import com.teslicek.txoptimizations.WorkerTextureLoads;
import java.util.Map;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TextureManager.class)
public abstract class TextureManagerMixin implements TextureManagerVersion {

    @Shadow
    @Final
    private Map<Identifier, AbstractTexture> byPath;

    @WrapMethod(method = "getTexture(Lnet/minecraft/resources/Identifier;)Lnet/minecraft/client/renderer/texture/AbstractTexture;")
    private AbstractTexture txoptimizations$locked(Identifier location, Operation<AbstractTexture> original) {
        if (WorkerTextureLoads.isServing() || !ParallelFeatures.lock())
            return original.call(location);

        try {
            if (!ParallelFeatures.isWorkerThread() || this.byPath.containsKey(location))
                return original.call(location);

            return WorkerTextureLoads.await((TextureManager) (Object) this, location);
        } finally {
            ParallelFeatures.unlock();
        }
    }

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
