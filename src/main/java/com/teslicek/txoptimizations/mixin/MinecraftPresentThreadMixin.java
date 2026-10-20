package com.teslicek.txoptimizations.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.platform.Window;
import com.mojang.renderpearl.api.commands.CommandEncoder;
import com.mojang.renderpearl.api.device.GpuSurface;
import com.mojang.renderpearl.api.device.SurfaceException;
import com.teslicek.txoptimizations.AcquiredImageDiscard;
import com.teslicek.txoptimizations.FramePath;
import com.teslicek.txoptimizations.PresentThread;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.screens.LevelLoadingScreen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.Options;
import org.objectweb.asm.Opcodes;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

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

    @Shadow
    public ClientLevel level;

    @Shadow
    @Final
    public Gui gui;

    @Unique
    private boolean txoptimizations$acquireDeferred;

    @Unique
    private boolean txoptimizations$holdFrame;

    @Unique
    private boolean txoptimizations$worldShown;

    @Unique
    private boolean txoptimizations$imageAcquiredEarly;

    @Unique
    private SurfaceException txoptimizations$earlyAcquireFailure;

    @Unique
    private boolean txoptimizations$frameAcquiredEarly;

    @Redirect(method = "renderFrame", at = @At(value = "INVOKE", target = "Lcom/mojang/renderpearl/api/device/GpuSurface;isAcquired()Z", ordinal = 0))
    private boolean txoptimizations$startWithEarlyImage(GpuSurface surface) {
        if (this.txoptimizations$imageAcquiredEarly)
            return false;

        return surface.isAcquired();
    }

    @ModifyExpressionValue(method = "renderFrame", at = @At(value = "FIELD", target = "Lnet/minecraft/client/Minecraft;windowSurfaceNeedsReconfiguring:Z", opcode = Opcodes.GETFIELD))
    private boolean txoptimizations$deferAcquire(boolean needsReconfiguring) {
        boolean worker = FramePath.beginFrame(PresentThread.canDeferAcquire(this.windowSurface));

        this.txoptimizations$acquireDeferred    = !needsReconfiguring && worker;
        this.txoptimizations$holdFrame          = !this.txoptimizations$worldShown && this.level != null && this.gui.screen() instanceof LevelLoadingScreen;
        this.txoptimizations$frameAcquiredEarly = this.txoptimizations$imageAcquiredEarly || this.txoptimizations$earlyAcquireFailure != null;

        if (this.txoptimizations$frameAcquiredEarly && (worker || this.txoptimizations$holdFrame))
            throw new IllegalStateException("A frame that acquired its screen image early cannot defer or hold it");

        if (this.level != null && !this.txoptimizations$holdFrame)
            this.txoptimizations$worldShown = true;

        return needsReconfiguring;
    }

    @Redirect(method = "renderFrame", at = @At(value = "INVOKE", target = "Lcom/mojang/renderpearl/api/device/GpuSurface;isSuboptimal()Z"))
    private boolean txoptimizations$skipDeferredSuboptimal(GpuSurface surface) {
        if (this.txoptimizations$acquireDeferred || this.txoptimizations$frameAcquiredEarly)
            return false;

        return surface.isSuboptimal();
    }

    @Redirect(method = "renderFrame", at = @At(value = "INVOKE", target = "Lcom/mojang/renderpearl/api/device/GpuSurface;configure(Lcom/mojang/renderpearl/api/device/GpuSurface$Configuration;)V"))
    private void txoptimizations$dropEarlyBeforeConfigure(GpuSurface surface, GpuSurface.Configuration config) throws SurfaceException {
        this.txoptimizations$dropEarlyAcquire(surface);
        surface.configure(config);
    }

    @Redirect(method = "renderFrame", at = @At(value = "INVOKE", target = "Lcom/mojang/renderpearl/api/device/GpuSurface;acquireNextTexture()V"))
    private void txoptimizations$useEarlyAcquire(GpuSurface surface) throws SurfaceException {
        if (!this.txoptimizations$frameAcquiredEarly) {
            surface.acquireNextTexture();

            return;
        }

        SurfaceException failure = this.txoptimizations$earlyAcquireFailure;

        this.txoptimizations$frameAcquiredEarly  = false;
        this.txoptimizations$imageAcquiredEarly  = false;
        this.txoptimizations$earlyAcquireFailure = null;

        if (failure != null)
            throw failure;
    }

    @ModifyExpressionValue(method = "renderFrame", at = @At(value = "FIELD", target = "Lnet/minecraft/client/Minecraft;surfaceIsInvalid:Z", opcode = Opcodes.GETFIELD))
    private boolean txoptimizations$skipDeferredAcquire(boolean invalid) {
        return invalid || this.txoptimizations$acquireDeferred || this.txoptimizations$holdFrame;
    }

    @Redirect(method = "renderFrame", at = @At(value = "INVOKE", target = "Lcom/mojang/renderpearl/api/device/GpuSurface;isAcquired()Z", ordinal = 1))
    private boolean txoptimizations$acquireBeforeBlit(GpuSurface surface) {
        if (this.txoptimizations$acquireDeferred) {
            this.txoptimizations$acquireDeferred = false;

            if (!this.txoptimizations$holdFrame)
                this.txoptimizations$configureAndAcquire();
        }

        return surface.isAcquired();
    }

    @Redirect(method = "renderFrame", at = @At(value = "INVOKE", target = "Lcom/mojang/renderpearl/api/commands/CommandEncoder;submit()V"))
    private void txoptimizations$presentOnPresentThread(CommandEncoder encoder) {
        if (this.windowSurface.isAcquired())
            PresentThread.arm(this.windowSurface);

        try {
            encoder.submit();
        } finally {
            PresentThread.disarm();
        }

        FramePath.frameSubmitted();
    }

    @Inject(method = "renderFrame", at = @At("TAIL"))
    private void txoptimizations$acquireNextEarly(boolean advanceGameTime, CallbackInfo ci) {
        if (this.txoptimizations$frameAcquiredEarly)
            throw new IllegalStateException("The screen image acquired early was not used by its frame");

        if (!this.txoptimizations$worldShown || !FramePath.nextFrameAcquiresEarly(PresentThread.canDeferAcquire(this.windowSurface)))
            return;

        if (this.windowSurface.isAcquired() || this.surfaceIsInvalid || this.windowSurfaceNeedsReconfiguring || this.windowSurface.isSuboptimal())
            return;

        try {
            this.windowSurface.acquireNextTexture();
            this.txoptimizations$imageAcquiredEarly = true;
        } catch (SurfaceException exception) {
            this.txoptimizations$earlyAcquireFailure = exception;
        }
    }

    @WrapOperation(method = "close", at = @At(value = "INVOKE", target = "Lcom/mojang/renderpearl/api/device/GpuSurface;close()V"))
    private void txoptimizations$dropEarlyBeforeClose(GpuSurface surface, Operation<Void> original) {
        this.txoptimizations$dropEarlyAcquire(surface);
        original.call(surface);
    }

    @Unique
    private void txoptimizations$dropEarlyAcquire(GpuSurface surface) {
        if (this.txoptimizations$imageAcquiredEarly)
            ((AcquiredImageDiscard) surface).txoptimizations$discardAcquired();

        this.txoptimizations$imageAcquiredEarly  = false;
        this.txoptimizations$earlyAcquireFailure = null;
        this.txoptimizations$frameAcquiredEarly  = false;
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

        long acquireStart = System.nanoTime();

        try {
            this.windowSurface.acquireNextTexture();
        } catch (SurfaceException ex) {
            LOGGER.warn("Couldn't acquire next surface texture with config {}", this.windowSurface.currentConfiguration(), ex);
            this.surfaceIsInvalid                = true;
            this.windowSurfaceNeedsReconfiguring = true;
        }

        FramePath.recordAcquire(System.nanoTime() - acquireStart);
    }
}
