package com.teslicek.txoptimizations.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.mojang.blaze3d.systems.RenderSystem;
import net.caffeinemc.mods.sodium.client.gpu.device.backend.DrawBackend;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(DrawBackend.class)
public abstract class DrawBackendIndirectMixin {

    @ModifyReturnValue(method = "chooseBackend", at = @At("RETURN"))
    private static DrawBackend txoptimizations$preferIndirect(DrawBackend backend) {
        if (backend == DrawBackend.VK_MULTIDRAW && RenderSystem.getDevice().getDeviceInfo().features().multiDrawIndirect())
            return DrawBackend.VK_INDIRECT;

        return backend;
    }
}
