package com.teslicek.txoptimizations.mixin;

import com.teslicek.txoptimizations.SectionNeighbors;
import net.minecraft.client.multiplayer.ClientChunkCache;
import net.minecraft.world.level.chunk.LightChunkGetter;
import net.minecraft.world.level.lighting.LayerLightSectionStorage;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LayerLightSectionStorage.class)
public abstract class LayerLightSectionStorageNeighborsMixin {

    @Shadow
    @Final
    protected LightChunkGetter chunkSource;

    @Inject(method = "setLightEnabled", at = @At("HEAD"))
    private void txoptimizations$neighborsChanged(long zeroNode, boolean enable, CallbackInfo ci) {
        if (this.chunkSource instanceof ClientChunkCache)
            SectionNeighbors.changed();
    }
}
