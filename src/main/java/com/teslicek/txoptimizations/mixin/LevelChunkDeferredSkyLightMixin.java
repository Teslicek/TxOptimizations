package com.teslicek.txoptimizations.mixin;

import com.teslicek.txoptimizations.DeferredSkyLightSources;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(LevelChunk.class)
public abstract class LevelChunkDeferredSkyLightMixin {

    @Redirect(method = "replaceWithPacketData", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/chunk/LevelChunk;initializeLightSources()V"))
    private void txoptimizations$fillSkyLightSourcesLater(LevelChunk chunk) {
        ((DeferredSkyLightSources) chunk.getSkyLightSources()).txoptimizations$fillLater(chunk);
    }
}
