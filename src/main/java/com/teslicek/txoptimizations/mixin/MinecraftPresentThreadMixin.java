package com.teslicek.txoptimizations.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.renderpearl.api.commands.CommandEncoder;
import com.mojang.renderpearl.api.device.GpuSurface;
import com.mojang.renderpearl.api.device.SurfaceException;
import com.mojang.renderpearl.frontend.FrontendCommandEncoder;
import com.teslicek.txoptimizations.PresentThread;
import com.teslicek.txoptimizations.SceneSubmit;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import org.objectweb.asm.Opcodes;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Minecraft.class)
public abstract class MinecraftPresentThreadMixin {

    @Shadow
    @Final
    private static Logger LOGGER;

    @Shadow
    @Final
    private Window window;

    @Shadow
    @Final
    private GpuSurface windowSurface;

    @Shadow
    @Final
    public Options options;

    @Shadow
    private boolean windowSurfaceNeedsReconfiguring;

    @Shadow
    private boolean surfaceIsInvalid;

    @Unique
    private boolean txoptimizations$acquireDeferred;

    @ModifyExpressionValue(method = "renderFrame", at = @At(value = "FIELD", target = "Lnet/minecraft/client/Minecraft;windowSurfaceNeedsReconfiguring:Z", opcode = Opcodes.GETFIELD))
    private boolean txoptimizations$deferAcquire(boolean needsReconfiguring) {
        this.txoptimizations$acquireDeferred = !needsReconfiguring && PresentThread.canDeferAcquire(this.windowSurface);

        return needsReconfiguring;
    }

    @WrapOperation(method = "renderFrame", at = @At(value = "INVOKE", target = "Lcom/mojang/renderpearl/api/device/GpuSurface;isSuboptimal()Z"))
    private boolean txoptimizations$skipDeferredSuboptimal(GpuSurface surface, Operation<Boolean> original) {
        if (this.txoptimizations$acquireDeferred)
            return false;

        return original.call(surface);
    }

    @ModifyExpressionValue(method = "renderFrame", at = @At(value = "FIELD", target = "Lnet/minecraft/client/Minecraft;surfaceIsInvalid:Z", opcode = Opcodes.GETFIELD))
    private boolean txoptimizations$skipDeferredAcquire(boolean invalid) {
        return invalid || this.txoptimizations$acquireDeferred;
    }

    @WrapOperation(method = "renderFrame", at = @At(value = "INVOKE", target = "Lcom/mojang/renderpearl/api/device/GpuSurface;isAcquired()Z", ordinal = 1))
    private boolean txoptimizations$acquireBeforeBlit(GpuSurface surface, Operation<Boolean> original) {
        if (this.txoptimizations$acquireDeferred) {
            this.txoptimizations$acquireDeferred = false;
            ((SceneSubmit) ((FrontendCommandEncoder) RenderSystem.getDevice().createCommandEncoder()).backend()).txoptimizations$submitScene();
            this.txoptimizations$configureAndAcquire();
        }

        return original.call(surface);
    }

    @WrapOperation(method = "renderFrame", at = @At(value = "INVOKE", target = "Lcom/mojang/renderpearl/api/commands/CommandEncoder;submit()V"))
    private void txoptimizations$presentOnPresentThread(CommandEncoder encoder, Operation<Void> original) {
        if (this.windowSurface.isAcquired())
            PresentThread.arm(this.windowSurface);

        try {
            original.call(encoder);
        } finally {
            PresentThread.disarm();
        }
    }

    @Unique
    private void txoptimizations$configureAndAcquire() {
        if ((this.windowSurfaceNeedsReconfiguring || this.windowSurface.isSuboptimal() && !this.surfaceIsInvalid) && !this.window.isIconified()) {
            Window.FramebufferSize   framebufferSize = this.window.queryFramebufferSize();
            GpuSurface.PresentMode   presentMode     = GpuSurface.PresentMode.getSupportedVsyncMode(this.windowSurface.supportedPresentModes(), this.options.enableVsync().get());
            GpuSurface.Configuration config          = new GpuSurface.Configuration(framebufferSize.width(), framebufferSize.height(), presentMode);

            try {
                this.windowSurface.configure(config);
                this.surfaceIsInvalid                = false;
                this.windowSurfaceNeedsReconfiguring = false;
            } catch (SurfaceException exception) {
                LOGGER.warn("Couldn't configure surface to {}", config, exception);
                this.surfaceIsInvalid = true;
            }
        }

        if (this.surfaceIsInvalid)
            return;

        try {
            this.windowSurface.acquireNextTexture();
        } catch (SurfaceException ex) {
            LOGGER.warn("Couldn't acquire next surface texture with config {}", this.windowSurface.currentConfiguration(), ex);
            this.surfaceIsInvalid                = true;
            this.windowSurfaceNeedsReconfiguring = true;
        }
    }
}
