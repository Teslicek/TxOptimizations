package com.teslicek.txoptimizations.mixin;

import com.teslicek.txoptimizations.SectionNeighbors;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net.minecraft.client.multiplayer.ClientChunkCache$Storage")
public abstract class ClientChunkStorageNeighborsMixin {

    @Inject(method = {"<init>", "replace", "drop"}, at = @At("RETURN"))
    private void txoptimizations$neighborsChanged(CallbackInfo ci) {
        SectionNeighbors.changed();
    }
}
