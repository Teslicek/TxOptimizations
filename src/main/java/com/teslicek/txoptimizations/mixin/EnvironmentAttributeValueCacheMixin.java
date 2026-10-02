package com.teslicek.txoptimizations.mixin;

import net.minecraft.world.attribute.EnvironmentAttribute;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

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

    @Shadow
    private Object getValueFromLevel(EnvironmentAttribute<Object> attribute) {
        throw new AssertionError();
    }

    @Overwrite
    public Object get(EnvironmentAttribute<Object> attribute, float partialTicks) {
        if (this.newValue == null)
            this.newValue = this.getValueFromLevel(attribute);
        else if (this.newValue == this.txoptimizations$cachedNewValue && this.lastValue == this.txoptimizations$cachedLastValue && partialTicks == this.txoptimizations$cachedPartialTick)
            return this.txoptimizations$cachedResult;

        this.txoptimizations$cachedLastValue   = this.lastValue;
        this.txoptimizations$cachedNewValue    = this.newValue;
        this.txoptimizations$cachedPartialTick = partialTicks;
        this.txoptimizations$cachedResult      = attribute.type().partialTickLerp().apply(partialTicks, this.lastValue, this.newValue);

        return this.txoptimizations$cachedResult;
    }
}
