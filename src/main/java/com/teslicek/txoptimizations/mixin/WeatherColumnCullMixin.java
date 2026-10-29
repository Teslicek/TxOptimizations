package com.teslicek.txoptimizations.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.teslicek.txoptimizations.FrustumBoxTest;
import com.teslicek.txoptimizations.PrecipitationCache;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.WeatherEffectRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.biome.Biome;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(WeatherEffectRenderer.class)
public abstract class WeatherColumnCullMixin {

    @Unique
    private static final int BOB_MARGIN = 1;

    @WrapOperation(method = "extractRenderState", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/ClientLevel;getPrecipitationAt(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/biome/Biome$Precipitation;"))
    private Biome.Precipitation txoptimizations$skipColumnsOutsideView(ClientLevel level, BlockPos pos, Operation<Biome.Precipitation> original, @Local(ordinal = 6) int bottom, @Local(ordinal = 7) int top) {
        Minecraft minecraft = Minecraft.getInstance();

        if (!txoptimizations$isViewWarped(minecraft) && !((FrustumBoxTest) minecraft.gameRenderer.mainCamera().getCullFrustum()).txoptimizations$isBoxVisible(pos.getX() - BOB_MARGIN, bottom - BOB_MARGIN, pos.getZ() - BOB_MARGIN, pos.getX() + 1 + BOB_MARGIN, top + 1 + BOB_MARGIN, pos.getZ() + 1 + BOB_MARGIN))
            return Biome.Precipitation.NONE;

        return PrecipitationCache.get(level, pos, original);
    }

    @Unique
    private static boolean txoptimizations$isViewWarped(Minecraft minecraft) {
        if (minecraft.getCameraEntity() instanceof LivingEntity living && (living.hurtTime > 0 || living.isDeadOrDying()))
            return true;

        LocalPlayer player = minecraft.player;

        return player != null && (player.portalEffectIntensity > 0.0F || player.oPortalEffectIntensity > 0.0F || player.hasEffect(MobEffects.NAUSEA));
    }
}
