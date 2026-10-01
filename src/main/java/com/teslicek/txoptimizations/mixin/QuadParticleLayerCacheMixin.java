package com.teslicek.txoptimizations.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import java.util.Map;
import java.util.function.Function;
import net.minecraft.client.renderer.state.level.QuadParticleRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(QuadParticleRenderState.class)
public abstract class QuadParticleLayerCacheMixin {

    @Unique
    private static final int CACHED_LAYERS = 8;

    @Unique
    private final Object[] txoptimizations$layers = new Object[CACHED_LAYERS];

    @Unique
    private final Object[] txoptimizations$storages = new Object[CACHED_LAYERS];

    @Unique
    private int txoptimizations$cachedLayers;

    @WrapOperation(method = "add", at = @At(value = "INVOKE", target = "Ljava/util/Map;computeIfAbsent(Ljava/lang/Object;Ljava/util/function/Function;)Ljava/lang/Object;"))
    private Object txoptimizations$reuseLayerStorage(Map<Object, Object> particles, Object layer, Function<Object, Object> createStorage, Operation<Object> original) {
        for (int index = 0; index < this.txoptimizations$cachedLayers; index ++) {
            if (this.txoptimizations$layers[index] == layer)
                return this.txoptimizations$storages[index];
        }

        Object storage = original.call(particles, layer, createStorage);

        if (this.txoptimizations$cachedLayers < CACHED_LAYERS) {
            this.txoptimizations$layers[this.txoptimizations$cachedLayers]   = layer;
            this.txoptimizations$storages[this.txoptimizations$cachedLayers] = storage;
            this.txoptimizations$cachedLayers ++;
        }

        return storage;
    }
}
