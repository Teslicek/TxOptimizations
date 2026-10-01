package com.teslicek.txoptimizations.mixin;

import com.teslicek.txoptimizations.ClientClock;
import com.teslicek.txoptimizations.EntityVisibilityMemo;
import com.teslicek.txoptimizations.bake.Baking;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.extract.LevelExtractor;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LevelExtractor.class)
public abstract class LevelExtractorEntityVisibilityMixin {

    @Unique
    private long txoptimizations$keyFrame = -1L;

    @Unique
    private Frustum txoptimizations$keyFrustum;

    @Unique
    private double txoptimizations$keyX;

    @Unique
    private double txoptimizations$keyY;

    @Unique
    private double txoptimizations$keyZ;

    @Unique
    private long txoptimizations$keyFadeIn;

    @Inject(method = "isEntityVisible", at = @At("HEAD"), cancellable = true)
    private void txoptimizations$reuseFrameVisibility(Entity entity, Frustum frustum, double x, double y, double z, float partialTick, long fadeIn, CallbackInfoReturnable<Boolean> cir) {
        if (!this.txoptimizations$matchesFrameKey(frustum, x, y, z, fadeIn))
            return;

        EntityVisibilityMemo memo = (EntityVisibilityMemo) entity;

        if (!memo.txoptimizations$hasVisibility(this.txoptimizations$keyFrame, partialTick))
            return;

        cir.setReturnValue(memo.txoptimizations$isVisible());
    }

    @Inject(method = "isEntityVisible", at = @At("RETURN"), cancellable = true)
    private void txoptimizations$storeFrameVisibility(Entity entity, Frustum frustum, double x, double y, double z, float partialTick, long fadeIn, CallbackInfoReturnable<Boolean> cir) {
        boolean visible = cir.getReturnValueZ() && !Baking.isEntityMeshed(entity);

        if (this.txoptimizations$matchesFrameKey(frustum, x, y, z, fadeIn))
            ((EntityVisibilityMemo) entity).txoptimizations$setVisibility(this.txoptimizations$keyFrame, partialTick, visible);

        cir.setReturnValue(visible);
    }

    @Unique
    private boolean txoptimizations$matchesFrameKey(Frustum frustum, double x, double y, double z, long fadeIn) {
        long frame = ClientClock.frame();

        if (frame != this.txoptimizations$keyFrame) {
            this.txoptimizations$keyFrame   = frame;
            this.txoptimizations$keyFrustum = frustum;
            this.txoptimizations$keyX       = x;
            this.txoptimizations$keyY       = y;
            this.txoptimizations$keyZ       = z;
            this.txoptimizations$keyFadeIn  = fadeIn;

            return true;
        }

        return frustum == this.txoptimizations$keyFrustum && x == this.txoptimizations$keyX && y == this.txoptimizations$keyY && z == this.txoptimizations$keyZ && fadeIn == this.txoptimizations$keyFadeIn;
    }
}
