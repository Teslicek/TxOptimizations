package com.teslicek.txoptimizations.mixin;

import com.teslicek.txoptimizations.ClientClock;
import com.teslicek.txoptimizations.EntityVisibilityMemo;
import com.teslicek.txoptimizations.bake.Baking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.extract.LevelExtractor;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(LevelExtractor.class)
public abstract class LevelExtractorEntityVisibilityMixin {

    @Shadow
    @Final
    private Minecraft minecraft;

    @Shadow
    @Final
    private LevelRenderer levelRenderer;

    @Shadow
    private ClientLevel level;

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

    @Overwrite
    public boolean isEntityVisible(Entity entity, Frustum frustum, double camX, double camY, double camZ, float partialTicks, long chunkFadeDuration) {
        boolean              keyed = this.txoptimizations$matchesFrameKey(frustum, camX, camY, camZ, chunkFadeDuration);
        EntityVisibilityMemo memo  = (EntityVisibilityMemo) entity;

        if (keyed && memo.txoptimizations$hasVisibility(this.txoptimizations$keyFrame, partialTicks))
            return memo.txoptimizations$isVisible();

        boolean visible = this.txoptimizations$computeVisibility(entity, frustum, camX, camY, camZ, partialTicks, chunkFadeDuration) && !Baking.isEntityMeshed(entity);

        if (keyed)
            memo.txoptimizations$setVisibility(this.txoptimizations$keyFrame, partialTicks, visible);

        return visible;
    }

    @Unique
    private boolean txoptimizations$computeVisibility(Entity entity, Frustum frustum, double camX, double camY, double camZ, float partialTicks, long chunkFadeDuration) {
        if (this.level == null)
            return false;

        if (!this.levelRenderer.entityRenderDispatcher().shouldRender(entity, frustum, camX, camY, camZ, partialTicks) && (this.minecraft.player == null || !entity.hasIndirectPassenger(this.minecraft.player)))
            return false;

        BlockPos blockPos = entity.blockPosition();

        return this.level.isOutsideBuildHeight(blockPos.getY()) || this.levelRenderer.isSectionCompiledAndVisible(blockPos, chunkFadeDuration);
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
