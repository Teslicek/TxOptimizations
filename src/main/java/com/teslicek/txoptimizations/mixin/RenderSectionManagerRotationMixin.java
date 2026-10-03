package com.teslicek.txoptimizations.mixin;

import com.teslicek.txoptimizations.PendingSections;
import java.util.Map;
import net.caffeinemc.mods.sodium.client.render.chunk.RenderSectionManager;
import net.caffeinemc.mods.sodium.client.render.chunk.async.CullTask;
import net.caffeinemc.mods.sodium.client.render.chunk.occlusion.CullType;
import net.caffeinemc.mods.sodium.client.render.chunk.occlusion.SectionTree;
import net.caffeinemc.mods.sodium.client.render.viewport.CameraTransform;
import net.caffeinemc.mods.sodium.client.render.viewport.Viewport;
import net.caffeinemc.mods.sodium.client.util.FogParameters;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = RenderSectionManager.class, remap = false)
public abstract class RenderSectionManagerRotationMixin {

    @Shadow
    private boolean needsGraphUpdate;

    @Shadow
    private boolean cameraChanged;

    @Shadow
    private CullTask pendingTask;

    @Shadow
    @Final
    private Map<CullType, SectionTree> cullResults;

    @Unique
    private boolean txoptimizations$hasTask;

    @Unique
    private double txoptimizations$taskX;

    @Unique
    private double txoptimizations$taskY;

    @Unique
    private double txoptimizations$taskZ;

    @Unique
    private float txoptimizations$taskRegularDistance;

    @Unique
    private float txoptimizations$taskLocalDistance;

    @Unique
    private boolean txoptimizations$taskOcclusion;

    @Unique
    private boolean txoptimizations$skippedTask;

    @Shadow
    private float getSearchDistanceForCullType(CullType type, FogParameters fogParameters) {
        throw new AssertionError();
    }

    @Shadow
    public abstract void markGraphDirty();

    @Inject(method = "prepareRenderTrees", at = @At("HEAD"))
    private void txoptimizations$cullFinalView(Viewport viewport, FogParameters fogParameters, boolean useOcclusionCulling, CallbackInfo ci) {
        if (this.cameraChanged || !this.txoptimizations$skippedTask)
            return;

        this.txoptimizations$skippedTask = false;
        this.markGraphDirty();
    }

    @Inject(method = "scheduleAsyncWork", at = @At("HEAD"), cancellable = true)
    private void txoptimizations$skipRotationOnlyTask(Viewport viewport, FogParameters fogParameters, boolean useOcclusionCulling, CallbackInfo ci) {
        if (this.pendingTask != null)
            return;

        CameraTransform transform        = viewport.getTransform();
        float           regularDistance  = this.getSearchDistanceForCullType(CullType.REGULAR, fogParameters);
        float           localDistance    = this.getSearchDistanceForCullType(CullType.LOCAL, fogParameters);
        boolean         samePosition     = transform.x == this.txoptimizations$taskX && transform.y == this.txoptimizations$taskY && transform.z == this.txoptimizations$taskZ;
        boolean         sameDistances    = regularDistance == this.txoptimizations$taskRegularDistance && localDistance == this.txoptimizations$taskLocalDistance;
        boolean         treesAvailable   = this.cullResults.containsKey(CullType.REGULAR) && this.cullResults.containsKey(CullType.WIDE);

        if (this.txoptimizations$hasTask && !this.needsGraphUpdate && samePosition && sameDistances && useOcclusionCulling == this.txoptimizations$taskOcclusion && treesAvailable && PendingSections.count() == 0) {
            this.txoptimizations$skippedTask = true;
            ci.cancel();

            return;
        }

        this.txoptimizations$hasTask             = true;
        this.txoptimizations$taskX               = transform.x;
        this.txoptimizations$taskY               = transform.y;
        this.txoptimizations$taskZ               = transform.z;
        this.txoptimizations$taskRegularDistance = regularDistance;
        this.txoptimizations$taskLocalDistance   = localDistance;
        this.txoptimizations$taskOcclusion       = useOcclusionCulling;
    }
}
