package com.teslicek.txoptimizations.mixin;

import net.minecraft.client.animation.KeyframeAnimation;
import net.minecraft.world.entity.AnimationState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(KeyframeAnimation.class)
public abstract class KeyframeAnimationApplyMixin {

    @Shadow
    public abstract void apply(long millis, float scale);

    @Overwrite
    public void apply(AnimationState state, float ageInTicks, float speedFactor) {
        if (state.isStarted())
            this.apply((long) ((float) state.getTimeInMillis(ageInTicks) * speedFactor), 1.0F);
    }
}
