package com.teslicek.txoptimizations.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.renderpearl.api.commands.CommandEncoder;
import com.mojang.renderpearl.api.device.GpuSurface;
import com.mojang.renderpearl.api.device.SurfaceException;
import com.mojang.renderpearl.api.textures.GpuTextureView;
import com.teslicek.txoptimizations.AcquiredImageDiscard;
import com.teslicek.txoptimizations.ClientClock;
import com.teslicek.txoptimizations.FramePath;
import com.teslicek.txoptimizations.PresentThread;
import com.teslicek.txoptimizations.RenderStateBuffers;
import com.teslicek.txoptimizations.RenderWorker;
import com.teslicek.txoptimizations.VisibleSectionBinding;
import com.teslicek.txoptimizations.VisibleSectionBuffers;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.screens.LevelLoadingScreen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.DynamicGpuData;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.gizmos.Gizmos;
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

    @Shadow
    @Final
    public LevelRenderer levelRenderer;

    @Shadow
    @Final
    public GameRenderer gameRenderer;

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
        return false;
    }

    @ModifyExpressionValue(method = "renderFrame", at = @At(value = "FIELD", target = "Lnet/minecraft/client/Minecraft;windowSurfaceNeedsReconfiguring:Z", opcode = Opcodes.GETFIELD))
    private boolean txoptimizations$deferAcquire(boolean needsReconfiguring) {
        if (needsReconfiguring)
            RenderWorker.awaitFrame();

        this.txoptimizations$acquireDeferred    = !needsReconfiguring;
        this.txoptimizations$holdFrame          = !this.txoptimizations$worldShown && this.level != null && this.gui.screen() instanceof LevelLoadingScreen;
        this.txoptimizations$frameAcquiredEarly = false;

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
        return false;
    }

    @Redirect(method = "renderFrame", at = @At(value = "INVOKE", target = "Lcom/mojang/renderpearl/api/commands/CommandEncoder;submit()V"))
    private void txoptimizations$presentOnPresentThread(CommandEncoder encoder) {
    }

    @Redirect(method = "renderFrame", at = @At(value = "INVOKE", target = "Lcom/mojang/renderpearl/api/device/GpuSurface;isAcquired()Z", ordinal = 2))
    private boolean txoptimizations$presentInRenderJob(GpuSurface surface) {
        return false;
    }

    @WrapOperation(method = "renderFrame", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/RenderSystem;executePendingTasks()V"), require = 1)
    private void txoptimizations$pendingTasksInRenderJob(Operation<Void> original) {
    }

    @WrapOperation(method = "renderFrame", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/DynamicGpuData;reset()V"), require = 1)
    private void txoptimizations$uniformResetInRenderJob(DynamicGpuData uniforms, Operation<Void> original) {
    }

    @WrapOperation(method = "renderFrame", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/LevelRenderer;endFrame()V"), require = 1)
    private void txoptimizations$endFrameInRenderJob(LevelRenderer renderer, Operation<Void> original) {
    }

    @WrapOperation(method = "renderFrame", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/GameRenderer;render()V"), require = 1)
    private void txoptimizations$renderInRenderJob(GameRenderer renderer, Operation<Void> original) {
        boolean acquire = this.txoptimizations$acquireDeferred && !this.txoptimizations$holdFrame;
        int     width   = this.window.getWidth();
        int     height  = this.window.getHeight();
        boolean vsync   = this.options.enableVsync().get();

        this.txoptimizations$acquireDeferred = false;

        VisibleSectionBuffers.handOff();
        RenderStateBuffers.handOff();
        ClientClock.handOff();
        RenderWorker.start(() -> this.txoptimizations$renderJob(renderer, original, acquire, width, height, vsync));
    }

    @Unique
    private void txoptimizations$renderJob(GameRenderer renderer, Operation<Void> original, boolean acquire, int width, int height, boolean vsync) {
        FramePath.beginFrame(PresentThread.canDeferAcquire(this.windowSurface));
        ((VisibleSectionBinding) this.levelRenderer).txoptimizations$bindRenderSections();
        RenderStateBuffers.bindRender((Minecraft) (Object) this);
        RenderSystem.executePendingTasks();

        try (Gizmos.TemporaryCollection ignored = this.levelRenderer.collectPerFrameRenderThreadGizmos()) {
            original.call(renderer);
        }

        if (acquire)
            this.txoptimizations$configureAndAcquire(width, height, vsync);

        if (this.windowSurface.isAcquired()) {
            GpuTextureView colorTexture = this.gameRenderer.mainRenderTarget().getColorTextureView();

            if (colorTexture == null)
                throw new IllegalStateException("Can't blit to screen, color texture doesn't exist yet");

            this.windowSurface.blitFromTexture(RenderSystem.getDevice().createCommandEncoder(), colorTexture);
            PresentThread.arm(this.windowSurface);
        }

        try {
            RenderSystem.getDevice().createCommandEncoder().submit();
        } finally {
            PresentThread.disarm();
        }

        FramePath.frameSubmitted();
        RenderSystem.getDynamicUniforms().reset();
        this.levelRenderer.endFrame();
    }

    @Inject(method = "renderFrame", at = @At("TAIL"))
    private void txoptimizations$acquireNextEarly(boolean advanceGameTime, CallbackInfo ci) {
        if (this.txoptimizations$frameAcquiredEarly)
            throw new IllegalStateException("The screen image acquired early was not used by its frame");
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
    private void txoptimizations$configureAndAcquire(int width, int height, boolean vsync) {
        if ((this.windowSurfaceNeedsReconfiguring || this.windowSurface.isSuboptimal() && !this.surfaceIsInvalid) && !this.window.isIconified()) {
            GpuSurface.PresentMode   presentMode = GpuSurface.PresentMode.getSupportedVsyncMode(this.windowSurface.supportedPresentModes(), vsync);
            GpuSurface.Configuration config      = new GpuSurface.Configuration(width, height, presentMode);

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
