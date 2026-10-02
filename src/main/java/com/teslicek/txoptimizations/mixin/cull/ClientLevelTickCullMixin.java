package com.teslicek.txoptimizations.mixin.cull;

import com.teslicek.txoptimizations.cull.EntityCulling;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.warden.Warden;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientLevel.class)
public abstract class ClientLevelTickCullMixin {

    @Unique
    private static final float HEARTBEAT_VOLUME = 5.0F;

    @Inject(method = "tickNonPassenger", at = @At("HEAD"), cancellable = true)
    private void txoptimizations$tickHiddenEntityBasics(Entity entity, CallbackInfo ci) {
        if (!EntityCulling.skipsTick(entity))
            return;

        ci.cancel();
        entity.setOldPosAndRot();
        entity.tickCount ++;

        if (entity instanceof LivingEntity living) {
            living.aiStep();

            if (living.hurtTime > 0)
                living.hurtTime --;
        }

        entity.getInterpolation().interpolate();

        if (entity instanceof Warden warden && !warden.isSilent() && warden.tickCount % ((WardenHeartbeatAccessor) warden).txoptimizations$getHeartBeatDelay() == 0)
            warden.level().playLocalSound(warden.getX(), warden.getY(), warden.getZ(), SoundEvents.WARDEN_HEARTBEAT, warden.getSoundSource(), HEARTBEAT_VOLUME, warden.getVoicePitch(), false);
    }
}
