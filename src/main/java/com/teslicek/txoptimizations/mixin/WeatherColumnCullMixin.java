package com.teslicek.txoptimizations.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.WeatherEffectRenderer;
import net.minecraft.client.renderer.state.level.WeatherRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(WeatherEffectRenderer.class)
public abstract class WeatherColumnCullMixin {

    @WrapOperation(method = "extractRenderState", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/ClientLevel;getPrecipitationAt(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/biome/Biome$Precipitation;"))
    private Biome.Precipitation txoptimizations$skipColumnsOutsideView(ClientLevel level, BlockPos pos, Operation<Biome.Precipitation> original, @Local(argsOnly = true) Vec3 cameraPosition, @Local(argsOnly = true) WeatherRenderState state) {
        int  cameraY = Mth.floor(cameraPosition.y);
        int  surface = level.getHeight(Heightmap.Types.MOTION_BLOCKING, pos.getX(), pos.getZ());
        int  bottom  = Math.max(cameraY - state.radius, surface);
        int  top     = Math.max(cameraY + state.radius, surface);
        AABB column  = new AABB(pos.getX(), bottom, pos.getZ(), pos.getX() + 1, top + 1, pos.getZ() + 1);

        if (!Minecraft.getInstance().gameRenderer.mainCamera().getCullFrustum().isVisible(column))
            return Biome.Precipitation.NONE;

        return original.call(level, pos);
    }
}
