package com.teslicek.txoptimizations.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.teslicek.txoptimizations.ClientClock;
import net.minecraft.client.Camera;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.FogType;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Camera.class)
public abstract class CameraFluidCacheMixin {

    @Shadow
    private boolean initialized;

    @Shadow
    private Level level;

    @Shadow
    private Vec3 position;

    @Shadow
    private float xRot;

    @Shadow
    private float yRot;

    @Unique
    private long txoptimizations$fluidFrame = -1L;

    @Unique
    private boolean txoptimizations$fluidInitialized;

    @Unique
    private Level txoptimizations$fluidLevel;

    @Unique
    private Vec3 txoptimizations$fluidPosition;

    @Unique
    private float txoptimizations$fluidXRot;

    @Unique
    private float txoptimizations$fluidYRot;

    @Unique
    private FogType txoptimizations$fluid;

    @Inject(method = "getFluidInCamera", at = @At("HEAD"), cancellable = true)
    private void txoptimizations$reuseFrameFluid(CallbackInfoReturnable<FogType> cir) {
        if (this.txoptimizations$fluidFrame != ClientClock.frame())
            return;

        if (this.txoptimizations$fluidInitialized != this.initialized || this.txoptimizations$fluidLevel != this.level || this.txoptimizations$fluidPosition != this.position)
            return;

        if (this.txoptimizations$fluidXRot != this.xRot || this.txoptimizations$fluidYRot != this.yRot)
            return;

        cir.setReturnValue(this.txoptimizations$fluid);
    }

    @ModifyReturnValue(method = "getFluidInCamera", at = @At("RETURN"))
    private FogType txoptimizations$storeFrameFluid(FogType fluid) {
        this.txoptimizations$fluidFrame       = ClientClock.frame();
        this.txoptimizations$fluidInitialized = this.initialized;
        this.txoptimizations$fluidLevel       = this.level;
        this.txoptimizations$fluidPosition    = this.position;
        this.txoptimizations$fluidXRot        = this.xRot;
        this.txoptimizations$fluidYRot        = this.yRot;
        this.txoptimizations$fluid            = fluid;

        return fluid;
    }
}
