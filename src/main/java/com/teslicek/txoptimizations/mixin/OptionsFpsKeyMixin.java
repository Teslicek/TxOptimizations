package com.teslicek.txoptimizations.mixin;

import com.teslicek.txoptimizations.FpsCounter;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Options;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Options.class)
public abstract class OptionsFpsKeyMixin {

    @Shadow
    @Final
    @Mutable
    public KeyMapping[] keyMappings;

    @Inject(method = "load", at = @At("HEAD"))
    private void txoptimizations$addFpsKey(CallbackInfo ci) {
        this.keyMappings = FpsCounter.withToggleKey(this.keyMappings);
    }
}
