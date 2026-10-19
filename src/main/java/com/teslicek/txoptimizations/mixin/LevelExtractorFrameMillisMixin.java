package com.teslicek.txoptimizations.mixin;

import com.teslicek.txoptimizations.ClientClock;
import net.minecraft.client.renderer.extract.LevelExtractor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(LevelExtractor.class)
public abstract class LevelExtractorFrameMillisMixin {

    @Redirect(method = "extractVisibleBlockEntities", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/Util;getMillis()J"))
    private long txoptimizations$frameMillis() {
        return ClientClock.frameMillis();
    }
}
