package com.teslicek.txoptimizations.mixin;

import com.teslicek.txoptimizations.ParticleExtractAhead;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.ParticleGroup;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.extract.LevelExtractor;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelExtractor.class)
public abstract class LevelExtractorParticleAheadMixin {

    @Shadow
    @Final
    private Minecraft minecraft;

    @Inject(method = "extract", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/extract/LevelExtractor;extractVisibleEntities(Lnet/minecraft/client/Camera;Lnet/minecraft/client/renderer/culling/Frustum;Lnet/minecraft/client/DeltaTracker;Lnet/minecraft/client/renderer/state/level/LevelRenderState;)V"))
    private void txoptimizations$extractParticlesAhead(DeltaTracker deltaTracker, Camera camera, float worldPartialTicks, CallbackInfo ci) {
        ParticleGroup<?> group = ((ParticleEngineAccessor) this.minecraft.particleEngine).txoptimizations$particles().get(ParticleRenderType.SINGLE_QUADS);

        if (group instanceof ParticleExtractAhead ahead && !group.isEmpty())
            ahead.txoptimizations$extractAhead(new Frustum(camera.getCullFrustum()).offset(-3.0F), camera, worldPartialTicks);
    }
}
