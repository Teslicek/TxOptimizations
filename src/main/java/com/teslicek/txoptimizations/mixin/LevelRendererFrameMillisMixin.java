package com.teslicek.txoptimizations.mixin;

import com.teslicek.txoptimizations.ClientClock;
import net.minecraft.client.renderer.LevelRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(LevelRenderer.class)
public abstract class LevelRendererFrameMillisMixin {

    @Redirect(method = "isSectionCompiledAndVisible", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/Util;getMillis()J"))
    private long txoptimizations$frameMillis() {
        return ClientClock.frameMillis();
    }
}
