package com.teslicek.txoptimizations.mixin;

import com.teslicek.txoptimizations.SectionTreeWalk;
import java.util.List;
import net.minecraft.client.renderer.Octree;
import net.minecraft.client.renderer.SectionOcclusionGraph;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import net.minecraft.client.renderer.culling.Frustum;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(SectionOcclusionGraph.class)
public abstract class SectionOcclusionGraphTreeWalkMixin {

    @Shadow
    public abstract Octree getOctree();

    @Overwrite
    public void addSectionsInFrustum(Frustum frustum, List<SectionRenderDispatcher.RenderSection> visibleSections, List<SectionRenderDispatcher.RenderSection> nearbyVisibleSection) {
        SectionTreeWalk.collect(this.getOctree(), frustum, visibleSections, nearbyVisibleSection);
    }
}
