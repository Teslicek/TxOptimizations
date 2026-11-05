package com.teslicek.txoptimizations.mixin;

import com.teslicek.txoptimizations.FpsCounter;
import com.teslicek.txoptimizations.cull.EntityCulling;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public abstract class MinecraftClientTickMixin {

    @Inject(method = "tick", at = @At("HEAD"))
    private void txoptimizations$startTick(CallbackInfo ci) {
        EntityCulling.tick((Minecraft) (Object) this);
    }

    @Inject(method = "tick", at = @At("RETURN"))
    private void txoptimizations$endTick(CallbackInfo ci) {
        FpsCounter.onEndClientTick((Minecraft) (Object) this);
    }
}
