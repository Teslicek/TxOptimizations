package com.teslicek.txoptimizations.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.teslicek.txoptimizations.ClientClock;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.fog.environment.AtmosphericFogEnvironment;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.lighting.LayerLightEventListener;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(AtmosphericFogEnvironment.class)
public abstract class AtmosphericFogTickCacheMixin {

    @Unique
    private static final int UNKNOWN_SKY_LIGHT = -1;

    @Unique
    private long txoptimizations$cachedTick = -1L;

    @Unique
    private long txoptimizations$cachedPos;

    @Unique
    private ClientLevel txoptimizations$cachedLevel;

    @Unique
    private Holder<Biome> txoptimizations$biome;

    @Unique
    private int txoptimizations$skyLight = UNKNOWN_SKY_LIGHT;

    @WrapOperation(method = "updateRainFogState", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/ClientLevel;getBiome(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/core/Holder;"))
    private Holder<Biome> txoptimizations$reuseTickBiome(ClientLevel level, BlockPos cameraPos, Operation<Holder<Biome>> original) {
        long tick = ClientClock.tick();
        long pos  = cameraPos.asLong();

        if (tick == this.txoptimizations$cachedTick && pos == this.txoptimizations$cachedPos && level == this.txoptimizations$cachedLevel)
            return this.txoptimizations$biome;

        this.txoptimizations$cachedTick  = tick;
        this.txoptimizations$cachedPos   = pos;
        this.txoptimizations$cachedLevel = level;
        this.txoptimizations$biome       = original.call(level, cameraPos);
        this.txoptimizations$skyLight    = UNKNOWN_SKY_LIGHT;

        return this.txoptimizations$biome;
    }

    @WrapOperation(method = "updateRainFogState", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/lighting/LayerLightEventListener;getLightValue(Lnet/minecraft/core/BlockPos;)I"))
    private int txoptimizations$reuseTickSkyLight(LayerLightEventListener skyLight, BlockPos cameraPos, Operation<Integer> original) {
        if (this.txoptimizations$skyLight == UNKNOWN_SKY_LIGHT)
            this.txoptimizations$skyLight = original.call(skyLight, cameraPos);

        return this.txoptimizations$skyLight;
    }
}
