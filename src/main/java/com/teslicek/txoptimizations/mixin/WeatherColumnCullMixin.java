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
import net.minecraft.client.renderer.state.level.WeatherRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WeatherEffectRenderer.class)
public abstract class WeatherColumnCullMixin {

    @Unique
    private static final int BOB_MARGIN = 1;

    @Unique
    private boolean txoptimizations$prepared;

    @Unique
    private @Nullable FrustumBoxTest txoptimizations$frustum;

    @Unique
    private int txoptimizations$firstX;

    @Unique
    private int txoptimizations$firstZ;

    @Unique
    private boolean[] txoptimizations$visibleXLines = new boolean[0];

    @Unique
    private boolean[] txoptimizations$visibleZLines = new boolean[0];

    @Inject(method = "extractRenderState", at = @At("HEAD"), require = 1)
    private void txoptimizations$forgetColumnLines(CallbackInfo ci) {
        this.txoptimizations$prepared = false;
    }

    @WrapOperation(method = "extractRenderState", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/ClientLevel;getPrecipitationAt(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/biome/Biome$Precipitation;"))
    private Biome.Precipitation txoptimizations$skipColumnsOutsideView(ClientLevel level, BlockPos pos, Operation<Biome.Precipitation> original, @Local(argsOnly = true) Vec3 cameraPos, @Local(argsOnly = true) WeatherRenderState renderState, @Local(ordinal = 6) int bottom, @Local(ordinal = 7) int top) {
        if (!this.txoptimizations$prepared)
            this.txoptimizations$prepareColumnLines(cameraPos, renderState.radius);

        FrustumBoxTest frustum = this.txoptimizations$frustum;

        if (frustum != null && !this.txoptimizations$isColumnVisible(frustum, pos.getX(), pos.getZ(), bottom, top))
            return Biome.Precipitation.NONE;

        return PrecipitationCache.get(level, pos, original);
    }

    @Unique
    private void txoptimizations$prepareColumnLines(Vec3 cameraPos, int radius) {
        Minecraft minecraft = Minecraft.getInstance();

        this.txoptimizations$prepared = true;

        if (txoptimizations$isViewWarped(minecraft)) {
            this.txoptimizations$frustum = null;

            return;
        }

        FrustumBoxTest frustum = (FrustumBoxTest) minecraft.gameRenderer.mainCamera().getCullFrustum();
        int            cameraX = Mth.floor(cameraPos.x);
        int            cameraY = Mth.floor(cameraPos.y);
        int            cameraZ = Mth.floor(cameraPos.z);
        int            lines   = 2 * radius + 1;
        int            minY    = cameraY - radius - BOB_MARGIN;
        int            maxY    = cameraY + radius + 1 + BOB_MARGIN;

        if (this.txoptimizations$visibleXLines.length != lines) {
            this.txoptimizations$visibleXLines = new boolean[lines];
            this.txoptimizations$visibleZLines = new boolean[lines];
        }

        this.txoptimizations$frustum = frustum;
        this.txoptimizations$firstX  = cameraX - radius;
        this.txoptimizations$firstZ  = cameraZ - radius;

        for (int line = 0; line < lines; line ++) {
            int x = this.txoptimizations$firstX + line;
            int z = this.txoptimizations$firstZ + line;

            this.txoptimizations$visibleXLines[line] = frustum.txoptimizations$isBoxVisible(x - BOB_MARGIN, minY, this.txoptimizations$firstZ - BOB_MARGIN, x + 1 + BOB_MARGIN, maxY, cameraZ + radius + 1 + BOB_MARGIN);
            this.txoptimizations$visibleZLines[line] = frustum.txoptimizations$isBoxVisible(this.txoptimizations$firstX - BOB_MARGIN, minY, z - BOB_MARGIN, cameraX + radius + 1 + BOB_MARGIN, maxY, z + 1 + BOB_MARGIN);
        }
    }

    @Unique
    private boolean txoptimizations$isColumnVisible(FrustumBoxTest frustum, int x, int z, int bottom, int top) {
        if (!this.txoptimizations$visibleXLines[x - this.txoptimizations$firstX] || !this.txoptimizations$visibleZLines[z - this.txoptimizations$firstZ])
            return false;

        return frustum.txoptimizations$isBoxVisible(x - BOB_MARGIN, bottom - BOB_MARGIN, z - BOB_MARGIN, x + 1 + BOB_MARGIN, top + 1 + BOB_MARGIN, z + 1 + BOB_MARGIN);
    }

    @Unique
    private static boolean txoptimizations$isViewWarped(Minecraft minecraft) {
        if (minecraft.getCameraEntity() instanceof LivingEntity living && (living.hurtTime > 0 || living.isDeadOrDying()))
            return true;

        LocalPlayer player = minecraft.player;

        return player != null && (player.portalEffectIntensity > 0.0F || player.oPortalEffectIntensity > 0.0F || player.hasEffect(MobEffects.NAUSEA));
    }
}
