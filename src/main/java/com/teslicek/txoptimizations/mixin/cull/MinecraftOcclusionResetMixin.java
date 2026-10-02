package com.teslicek.txoptimizations.mixin.cull;

import com.teslicek.txoptimizations.cull.OcclusionCuller;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public abstract class MinecraftOcclusionResetMixin {

    @Inject(method = "setLevel", at = @At("TAIL"))
    private void txoptimizations$resetOcclusion(CallbackInfo ci) {
        OcclusionCuller.reset();
    }
}
