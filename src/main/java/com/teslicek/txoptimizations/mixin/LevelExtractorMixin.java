package com.teslicek.txoptimizations.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import com.teslicek.txoptimizations.ClientClock;
import com.teslicek.txoptimizations.DisplayPreCull;
import com.teslicek.txoptimizations.NeighborMark;
import com.teslicek.txoptimizations.ParticleExtractAhead;
import com.teslicek.txoptimizations.SectionDirtyStates;
import com.teslicek.txoptimizations.SectionNeighbors;
import com.teslicek.txoptimizations.SectionSubset;
import com.teslicek.txoptimizations.cull.EntityCulling;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectListIterator;
import java.util.List;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.SectionUpdateTracker;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleGroup;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.ViewArea;
import net.minecraft.client.renderer.chunk.RenderRegionCache;
import net.minecraft.client.renderer.chunk.RenderSectionRegion;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.extract.LevelExtractor;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelExtractor.class)
public abstract class LevelExtractorMixin {

    @Redirect(method = "extractVisibleBlockEntities", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/Util;getMillis()J"))
    private long txoptimizations$frameMillis() {
        return ClientClock.frameMillis();
    }

    @Redirect(method = "extractVisibleBlockEntities", at = @At(value = "INVOKE", target = "Lit/unimi/dsi/fastutil/objects/ObjectArrayList;iterator()Lit/unimi/dsi/fastutil/objects/ObjectListIterator;"))
    private ObjectListIterator<SectionRenderDispatcher.RenderSection> txoptimizations$blockEntitySections(ObjectArrayList<SectionRenderDispatcher.RenderSection> visibleSections) {
        return SectionSubset.BLOCK_ENTITIES.iterator(visibleSections);
    }

    @ModifyExpressionValue(method = "extractVisibleBlockEntities", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/chunk/SectionMesh;getRenderableBlockEntities()Ljava/util/List;"))
    private List<BlockEntity> txoptimizations$recordBlockEntitySection(List<BlockEntity> blockEntities, @Local SectionRenderDispatcher.RenderSection section) {
        SectionSubset.BLOCK_ENTITIES.note(section, !blockEntities.isEmpty());

        if (!blockEntities.isEmpty())
            EntityCulling.enterBlockEntitySection(section);

        return blockEntities;
    }

    @Redirect(method = "extractVisibleBlockEntities", at = @At(value = "INVOKE", target = "Lit/unimi/dsi/fastutil/longs/Long2ObjectMap;get(J)Ljava/lang/Object;"))
    private Object txoptimizations$skipEmptyDestruction(Long2ObjectMap<?> destruction, long pos) {
        if (destruction.isEmpty())
            return null;

        return destruction.get(pos);
    }

    @Inject(method = "extractVisibleBlockEntities", at = @At("RETURN"))
    private void txoptimizations$leaveBlockEntitySections(CallbackInfo ci) {
        EntityCulling.leaveBlockEntitySections();
    }

    @Redirect(method = "extract", at = @At(value = "INVOKE", target = "Lit/unimi/dsi/fastutil/objects/ObjectArrayList;iterator()Lit/unimi/dsi/fastutil/objects/ObjectListIterator;"))
    private ObjectListIterator<SectionRenderDispatcher.RenderSection> txoptimizations$dirtySections(ObjectArrayList<SectionRenderDispatcher.RenderSection> visibleSections) {
        return SectionSubset.DIRTY.iterator(visibleSections);
    }

    @Redirect(method = "extract", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/SectionUpdateTracker;getDirtyState(J)Lnet/minecraft/client/SectionUpdateTracker$SectionDirtyState;"))
    private SectionUpdateTracker.SectionDirtyState txoptimizations$recordDirtyState(SectionUpdateTracker tracker, long sectionNode, @Local SectionRenderDispatcher.RenderSection section) {
        SectionUpdateTracker.SectionDirtyState state = SectionDirtyStates.of(tracker, section.index, sectionNode);

        SectionSubset.DIRTY.note(section, state != null && state.isDirty());

        return state;
    }

    @Redirect(method = "extract", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/SectionUpdateTracker;hasAllNeighbors(Lnet/minecraft/client/multiplayer/ClientLevel;J)Z"))
    private boolean txoptimizations$rememberMissingNeighbors(SectionUpdateTracker tracker, ClientLevel level, long sectionNode, @Local SectionRenderDispatcher.RenderSection section) {
        return SectionNeighbors.hasAllNeighbors(tracker, level, sectionNode, (NeighborMark) section);
    }

    @Shadow
    @Final
    private LevelRenderer levelRenderer;

    @Shadow
    private @Nullable SectionUpdateTracker sectionUpdateTracker;

    @Inject(method = "applyFrustum", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/LevelRenderer;clearVisibleSections()V"))
    private void txoptimizations$beginWalk(Frustum frustum, CallbackInfo ci) {
        SectionSubset.beginWalk();
    }

    @Inject(method = "applyFrustum", at = @At("RETURN"))
    private void txoptimizations$carrySubsets(Frustum frustum, CallbackInfo ci) {
        ViewArea viewArea = this.levelRenderer.viewArea();

        if (viewArea == null)
            throw new IllegalStateException("Visible sections were walked without a view area");

        SectionSubset.endWalk(this.levelRenderer.visibleSections(), viewArea, this.sectionUpdateTracker);
    }

    @Unique
    private static final RenderRegionCache UNUSED_CACHE = new RenderRegionCache();

    @Redirect(method = "extract", at = @At(value = "NEW", target = "()Lnet/minecraft/client/renderer/chunk/RenderRegionCache;"))
    private RenderRegionCache txoptimizations$deferCache() {
        return UNUSED_CACHE;
    }

    @Redirect(method = "extract", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/chunk/RenderRegionCache;createRegion(Lnet/minecraft/client/multiplayer/ClientLevel;J)Lnet/minecraft/client/renderer/chunk/RenderSectionRegion;"))
    private RenderSectionRegion txoptimizations$createCacheOnFirstUse(RenderRegionCache deferred, ClientLevel level, long sectionNode, @Share("regionCache") LocalRef<RenderRegionCache> cache) {
        if (deferred != UNUSED_CACHE)
            throw new IllegalStateException("Section regions were requested from a region cache that was not deferred: " + deferred);

        if (cache.get() == null)
            cache.set(new RenderRegionCache());

        return cache.get().createRegion(level, sectionNode);
    }

    @Inject(method = "extractVisibleEntities", at = @At("HEAD"))
    private void txoptimizations$prepareDisplayPreCull(Camera camera, Frustum frustum, DeltaTracker deltaTracker, LevelRenderState output, CallbackInfo ci) {
        DisplayPreCull.prepare(Minecraft.getInstance().gameRenderer);
    }

    @Shadow
    @Final
    private Minecraft minecraft;

    @Inject(method = "extract", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/extract/LevelExtractor;extractPlayerState(Lnet/minecraft/client/Camera;Lnet/minecraft/client/DeltaTracker;FLnet/minecraft/client/renderer/state/level/PlayerRenderState;)V"), require = 1)
    private void txoptimizations$extractParticlesAhead(DeltaTracker deltaTracker, Camera camera, float worldPartialTicks, CallbackInfo ci) {
        ParticleGroup<?> group = ((ParticleEngineAccessor) this.minecraft.particleEngine).txoptimizations$particles().get(ParticleRenderType.SINGLE_QUADS);

        if (group instanceof ParticleExtractAhead ahead && !group.isEmpty())
            ahead.txoptimizations$extractAhead(new Frustum(camera.getCullFrustum()).offset(-3.0F), camera, worldPartialTicks);
    }
}
