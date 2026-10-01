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
    private Object txoptimizations$lastLayer;

    @Unique
    private Object txoptimizations$lastStorage;

    @WrapOperation(method = "add", at = @At(value = "INVOKE", target = "Ljava/util/Map;computeIfAbsent(Ljava/lang/Object;Ljava/util/function/Function;)Ljava/lang/Object;"))
    private Object txoptimizations$reuseLayerStorage(Map<Object, Object> particles, Object layer, Function<Object, Object> createStorage, Operation<Object> original) {
        if (layer == this.txoptimizations$lastLayer)
            return this.txoptimizations$lastStorage;

        Object storage = original.call(particles, layer, createStorage);
        this.txoptimizations$lastLayer   = layer;
        this.txoptimizations$lastStorage = storage;

        return storage;
    }
}
