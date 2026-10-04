package com.teslicek.txoptimizations.mixin;

import com.teslicek.txoptimizations.RenderThreadStack;
import net.minecraft.client.Minecraft;
import org.lwjgl.system.MemoryStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public abstract class MinecraftRenderThreadStackMixin {

    @Inject(method = "<init>", at = @At("RETURN"))
    private void txoptimizations$captureRenderThreadStack(CallbackInfo ci) {
        RenderThreadStack.capture(MemoryStack.stackGet());
    }
}
