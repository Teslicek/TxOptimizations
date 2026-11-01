package com.teslicek.txoptimizations.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.teslicek.txoptimizations.ParallelFeatures;
import com.teslicek.txoptimizations.WorkerTextureLoads;
import java.util.Map;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(TextureManager.class)
public abstract class TextureManagerParallelMixin {

    @Shadow
    @Final
    private Map<Identifier, AbstractTexture> byPath;

    @WrapMethod(method = "getTexture(Lnet/minecraft/resources/Identifier;)Lnet/minecraft/client/renderer/texture/AbstractTexture;")
    private AbstractTexture txoptimizations$locked(Identifier location, Operation<AbstractTexture> original) {
        if (!ParallelFeatures.lock())
            return original.call(location);

        try {
            if (!ParallelFeatures.isWorkerThread() || this.byPath.containsKey(location))
                return original.call(location);
        } finally {
            ParallelFeatures.unlock();
        }

        return WorkerTextureLoads.await((TextureManager) (Object) this, location);
    }
}
