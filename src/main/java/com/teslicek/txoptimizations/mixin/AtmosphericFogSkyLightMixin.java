package com.teslicek.txoptimizations.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.teslicek.txoptimizations.ClientClock;
import net.minecraft.client.renderer.fog.environment.AtmosphericFogEnvironment;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.lighting.LayerLightEventListener;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(AtmosphericFogEnvironment.class)
public abstract class AtmosphericFogSkyLightMixin {

    @Unique
    private long txoptimizations$skyLightTick = -1L;

    @Unique
    private long txoptimizations$skyLightPos;

    @Unique
    private int txoptimizations$skyLight;

    @WrapOperation(method = "updateRainFogState", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/lighting/LayerLightEventListener;getLightValue(Lnet/minecraft/core/BlockPos;)I"))
    private int txoptimizations$reuseTickSkyLight(LayerLightEventListener skyLight, BlockPos cameraPos, Operation<Integer> original) {
        long tick = ClientClock.tick();
        long pos  = cameraPos.asLong();

        if (tick == this.txoptimizations$skyLightTick && pos == this.txoptimizations$skyLightPos)
            return this.txoptimizations$skyLight;

        this.txoptimizations$skyLightTick = tick;
        this.txoptimizations$skyLightPos  = pos;
        this.txoptimizations$skyLight     = original.call(skyLight, cameraPos);

        return this.txoptimizations$skyLight;
    }
}
