package com.teslicek.txoptimizations.mixin;

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

@Mixin(value = LevelRenderer.class, priority = 999)
public abstract class LevelRendererTranslucentResortMixin {

    @Shadow
    @Final
    private GameRenderer gameRenderer;

    @Overwrite
    private void scheduleResort(SectionRenderDispatcher.RenderSection section, TranslucencyPointOfView pointOfView, Vec3 cameraPos, boolean blockPosChanged, boolean isNearby) {
        if (this.gameRenderer.useImprovedTransparency())
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
