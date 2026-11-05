package com.teslicek.txoptimizations.mixin;

import com.teslicek.txoptimizations.PrecipitationCache;
import com.teslicek.txoptimizations.SectionNeighbors;
import net.minecraft.client.multiplayer.ClientChunkCache;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientChunkCache.class)
public abstract class ClientChunkCacheMixin {

    @Inject(method = {"replaceWithPacketData", "drop", "replaceBiomes"}, at = @At("RETURN"))
    private void txoptimizations$invalidatePrecipitation(CallbackInfo ci) {
        PrecipitationCache.invalidate();
    }

    @Inject(method = "updateViewCenter", at = @At("HEAD"))
    private void txoptimizations$neighborsChanged(int x, int z, CallbackInfo ci) {
        SectionNeighbors.changed();
    }
}
