package com.teslicek.txoptimizations.mixin;

import com.teslicek.txoptimizations.RenderStateBuffers;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public abstract class MinecraftRenderStateBuffersMixin {

    @Inject(method = "renderFrame", at = @At("HEAD"))
    private void txoptimizations$swapRenderStates(boolean advanceGameTime, CallbackInfo ci) {
        RenderStateBuffers.swap((Minecraft) (Object) this);
    }
}
