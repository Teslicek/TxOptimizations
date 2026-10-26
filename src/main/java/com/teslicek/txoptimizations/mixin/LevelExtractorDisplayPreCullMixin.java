package com.teslicek.txoptimizations.mixin;

import com.teslicek.txoptimizations.DisplayPreCull;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.extract.LevelExtractor;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelExtractor.class)
public abstract class LevelExtractorDisplayPreCullMixin {

    @Inject(method = "extractVisibleEntities", at = @At("HEAD"))
    private void txoptimizations$prepareDisplayPreCull(Camera camera, Frustum frustum, DeltaTracker deltaTracker, LevelRenderState output, CallbackInfo ci) {
        DisplayPreCull.prepare(Minecraft.getInstance().gameRenderer);
    }
}
