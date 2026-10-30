package com.teslicek.txoptimizations.mixin;

import com.teslicek.txoptimizations.DrawRecords;
import com.teslicek.txoptimizations.SectionDrawList;
import com.teslicek.txoptimizations.SectionDrawRecord;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.chunk.SectionMesh;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import net.minecraft.client.renderer.chunk.TranslucencyPointOfView;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = LevelRenderer.class, priority = 999)
public abstract class LevelRendererTranslucentResortMixin {

    @Unique
    private static final int MIN_RESORTS = 15;

    @Unique
    private static final int RESORT_SHARE = 8;

    @Shadow
    @Final
    private GameRenderer gameRenderer;

    @Shadow
    @Final
    private ObjectArrayList<SectionRenderDispatcher.RenderSection> visibleSections;

    @Unique
    private double txoptimizations$sweptX = Double.NaN;

    @Unique
    private double txoptimizations$sweptY = Double.NaN;

    @Unique
    private double txoptimizations$sweptZ = Double.NaN;

    @Unique
    private int txoptimizations$sweptChanges;

    @Unique
    private int txoptimizations$sweptRefills;

    @Unique
    private int txoptimizations$sweptFrames;

    @Unique
    private boolean txoptimizations$sweptImprovedTransparency;

    @Inject(method = "scheduleTranslucentSectionResort", at = @At("HEAD"), cancellable = true)
    private void txoptimizations$skipSettledResorts(Vec3 cameraPos, CallbackInfo ci) {
        int     changes  = DrawRecords.recordChanges();
        int     refills  = SectionDrawList.visibleRefills();
        boolean improved = this.gameRenderer.useImprovedTransparency();

        if (cameraPos.x != this.txoptimizations$sweptX || cameraPos.y != this.txoptimizations$sweptY || cameraPos.z != this.txoptimizations$sweptZ || changes != this.txoptimizations$sweptChanges || refills != this.txoptimizations$sweptRefills || improved != this.txoptimizations$sweptImprovedTransparency) {
            this.txoptimizations$sweptX                    = cameraPos.x;
            this.txoptimizations$sweptY                    = cameraPos.y;
            this.txoptimizations$sweptZ                    = cameraPos.z;
            this.txoptimizations$sweptChanges              = changes;
            this.txoptimizations$sweptRefills              = refills;
            this.txoptimizations$sweptFrames               = 0;
            this.txoptimizations$sweptImprovedTransparency = improved;

            return;
        }

        int size  = this.visibleSections.size();
        int sweep = size == 0 ? 0 : Math.ceilDiv(size, Math.max(size / RESORT_SHARE, MIN_RESORTS)) + 1;

        if (this.txoptimizations$sweptFrames < sweep) {
            this.txoptimizations$sweptFrames ++;

            return;
        }

        ci.cancel();
    }

    @Overwrite
    private void scheduleResort(SectionRenderDispatcher.RenderSection section, TranslucencyPointOfView pointOfView, Vec3 cameraPos, boolean blockPosChanged, boolean isNearby) {
        if (this.gameRenderer.useImprovedTransparency() || !((SectionDrawRecord) section).txoptimizations$hasTranslucentGeometry())
            return;

        SectionMesh sectionMesh = section.getSectionMesh();

        if (!sectionMesh.hasTranslucentGeometry())
            return;

        pointOfView.set(cameraPos, section.getSectionNode());

        boolean resortBecauseBlockPosChanged = blockPosChanged && (pointOfView.isAxisAligned() || isNearby);

        if (!resortBecauseBlockPosChanged && !sectionMesh.isDifferentPointOfView(pointOfView))
            return;

        if (section.transparencyResortingScheduled())
            return;

        section.resortTransparency();
    }
}
