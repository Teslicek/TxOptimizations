package com.teslicek.txoptimizations.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import net.caffeinemc.mods.sodium.client.gpu.device.backend.DrawBackend;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(DrawBackend.class)
public abstract class DrawBackendIndirectMixin {

    @Inject(method = "chooseBackend", at = @At("RETURN"), cancellable = true)
    private static void txoptimizations$preferIndirect(CallbackInfoReturnable<DrawBackend> cir) {
        if (cir.getReturnValue() == DrawBackend.VK_MULTIDRAW && RenderSystem.getDevice().getDeviceInfo().features().multiDrawIndirect())
            cir.setReturnValue(DrawBackend.VK_INDIRECT);
    }
}
