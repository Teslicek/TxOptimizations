package com.teslicek.txoptimizations.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.renderpearl.api.commands.CommandEncoder;
import com.mojang.renderpearl.api.device.GpuSurface;
import com.teslicek.txoptimizations.EarlyPresent;
import com.teslicek.txoptimizations.gpu.GpuPassProfiler;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Minecraft.class)
public abstract class MinecraftEarlyPresentMixin {

    @Shadow
    @Final
    private GpuSurface windowSurface;

    @WrapOperation(method = "renderFrame", at = @At(value = "INVOKE", target = "Lcom/mojang/renderpearl/api/commands/CommandEncoder;submit()V"))
    private void txoptimizations$presentBeforeFrameWait(CommandEncoder encoder, Operation<Void> original) {
        if (this.windowSurface.isAcquired())
            EarlyPresent.arm(this.windowSurface);

        try {
            original.call(encoder);
        } finally {
            EarlyPresent.disarm();
        }

        GpuPassProfiler.frameSubmitted();
    }
}
