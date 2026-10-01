package com.teslicek.txoptimizations.mixin;

import net.minecraft.world.attribute.EnvironmentAttribute;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "net.minecraft.world.attribute.EnvironmentAttributeProbe$ValueProbe")
public abstract class EnvironmentAttributeValueCacheMixin {

    @Shadow
    private Object lastValue;

    @Shadow
    private Object newValue;

    @Unique
    private Object txoptimizations$cachedLastValue;

    @Unique
    private Object txoptimizations$cachedNewValue;

    @Unique
    private float txoptimizations$cachedPartialTick;

    @Unique
    private Object txoptimizations$cachedResult;

    @Inject(method = "get", at = @At("HEAD"), cancellable = true)
    private void txoptimizations$reuseLerpedValue(EnvironmentAttribute<?> attribute, float partialTick, CallbackInfoReturnable<Object> cir) {
        if (this.newValue == null || this.newValue != this.txoptimizations$cachedNewValue || this.lastValue != this.txoptimizations$cachedLastValue)
            return;

        if (partialTick != this.txoptimizations$cachedPartialTick)
            return;

        cir.setReturnValue(this.txoptimizations$cachedResult);
    }

    @Inject(method = "get", at = @At("RETURN"))
    private void txoptimizations$storeLerpedValue(EnvironmentAttribute<?> attribute, float partialTick, CallbackInfoReturnable<Object> cir) {
        this.txoptimizations$cachedLastValue   = this.lastValue;
        this.txoptimizations$cachedNewValue    = this.newValue;
        this.txoptimizations$cachedPartialTick = partialTick;
        this.txoptimizations$cachedResult      = cir.getReturnValue();
    }
}
