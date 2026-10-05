package com.teslicek.txoptimizations.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.teslicek.txoptimizations.ReloadProfile;
import net.minecraft.server.packs.resources.ReloadableResourceManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ReloadableResourceManager.class)
public abstract class ReloadableResourceManagerProfileMixin {

    @ModifyExpressionValue(method = "createReload", at = @At(value = "INVOKE", target = "Lorg/slf4j/Logger;isDebugEnabled()Z"))
    private boolean txoptimizations$profileArmedReload(boolean debug) {
        return ReloadProfile.consume() || debug;
    }
}
