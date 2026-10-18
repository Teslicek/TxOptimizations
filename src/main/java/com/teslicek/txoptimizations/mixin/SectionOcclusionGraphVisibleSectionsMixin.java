package com.teslicek.txoptimizations.mixin;

import com.teslicek.txoptimizations.SectionSubset;
import java.util.List;
import net.minecraft.client.renderer.SectionOcclusionGraph;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import net.minecraft.client.renderer.culling.Frustum;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SectionOcclusionGraph.class)
public abstract class SectionOcclusionGraphVisibleSectionsMixin {

    @Inject(method = "addSectionsInFrustum", at = @At("HEAD"))
    private void txoptimizations$beginAdd(Frustum frustum, List<SectionRenderDispatcher.RenderSection> visibleSections, List<SectionRenderDispatcher.RenderSection> nearbyVisibleSections, CallbackInfo ci) {
        SectionSubset.beginVisibleChange();
    }

    @Inject(method = "addSectionsInFrustum", at = @At("RETURN"))
    private void txoptimizations$endAdd(Frustum frustum, List<SectionRenderDispatcher.RenderSection> visibleSections, List<SectionRenderDispatcher.RenderSection> nearbyVisibleSections, CallbackInfo ci) {
        SectionSubset.endVisibleChange();
    }
}
