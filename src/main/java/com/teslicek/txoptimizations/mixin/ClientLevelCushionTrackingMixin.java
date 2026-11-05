package com.teslicek.txoptimizations.mixin;

import com.teslicek.txoptimizations.bake.Cushions;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net.minecraft.client.multiplayer.ClientLevel$EntityCallbacks")
public abstract class ClientLevelCushionTrackingMixin {

    @Inject(method = "onTrackingStart(Lnet/minecraft/world/entity/Entity;)V", at = @At("TAIL"))
    private void txoptimizations$trackCushion(Entity entity, CallbackInfo ci) {
        Cushions.track(entity);
    }

    @Inject(method = "onTrackingEnd(Lnet/minecraft/world/entity/Entity;)V", at = @At("HEAD"))
    private void txoptimizations$untrackCushion(Entity entity, CallbackInfo ci) {
        Cushions.untrack(entity);
    }
}
