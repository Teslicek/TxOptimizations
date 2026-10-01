package com.teslicek.txoptimizations.mixin;

import com.teslicek.txoptimizations.ClientClock;
import net.minecraft.client.particle.Particle;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Particle.class)
public abstract class ParticleLightCacheMixin {

    @Shadow
    protected double x;

    @Shadow
    protected double y;

    @Shadow
    protected double z;

    @Unique
    private long txoptimizations$lightTick = -1L;

    @Unique
    private double txoptimizations$lightX;

    @Unique
    private double txoptimizations$lightY;

    @Unique
    private double txoptimizations$lightZ;

    @Unique
    private int txoptimizations$light;

    @Inject(method = "getLightCoords", at = @At("HEAD"), cancellable = true)
    private void txoptimizations$reuseTickLight(float partialTick, CallbackInfoReturnable<Integer> cir) {
        if (this.txoptimizations$lightTick != ClientClock.tick())
            return;

        if (this.txoptimizations$lightX != this.x || this.txoptimizations$lightY != this.y || this.txoptimizations$lightZ != this.z)
            return;

        cir.setReturnValue(this.txoptimizations$light);
    }

    @Inject(method = "getLightCoords", at = @At("RETURN"))
    private void txoptimizations$storeTickLight(float partialTick, CallbackInfoReturnable<Integer> cir) {
        this.txoptimizations$lightTick = ClientClock.tick();
        this.txoptimizations$lightX    = this.x;
        this.txoptimizations$lightY    = this.y;
        this.txoptimizations$lightZ    = this.z;
        this.txoptimizations$light     = cir.getReturnValueI();
    }
}
