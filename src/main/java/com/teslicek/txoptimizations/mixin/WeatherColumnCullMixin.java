package com.teslicek.txoptimizations.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.teslicek.txoptimizations.FrustumBoxTest;
import com.teslicek.txoptimizations.PrecipitationCache;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.WeatherEffectRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.biome.Biome;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(WeatherEffectRenderer.class)
public abstract class WeatherColumnCullMixin {

    @WrapOperation(method = "extractRenderState", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/ClientLevel;getPrecipitationAt(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/biome/Biome$Precipitation;"))
    private Biome.Precipitation txoptimizations$skipColumnsOutsideView(ClientLevel level, BlockPos pos, Operation<Biome.Precipitation> original, @Local(ordinal = 6) int bottom, @Local(ordinal = 7) int top) {
        if (!((FrustumBoxTest) Minecraft.getInstance().gameRenderer.mainCamera().getCullFrustum()).txoptimizations$isBoxVisible(pos.getX(), bottom, pos.getZ(), pos.getX() + 1, top + 1, pos.getZ() + 1))
            return Biome.Precipitation.NONE;

        return PrecipitationCache.get(level, pos, original);
    }
}
