package com.teslicek.txoptimizations.mixin;

import com.teslicek.txoptimizations.SectionSubset;
import com.teslicek.txoptimizations.VisibleSectionBuffers;
import com.teslicek.txoptimizations.VisibleSectionBinding;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = LevelRenderer.class, priority = 999)
public abstract class LevelRendererVisibleSectionsMixin implements VisibleSectionBinding {

    @Shadow
    @Final
    @Mutable
    private ObjectArrayList<SectionRenderDispatcher.RenderSection> visibleSections;

    @Shadow
    @Final
    @Mutable
    private ObjectArrayList<SectionRenderDispatcher.RenderSection> nearbyVisibleSections;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void txoptimizations$adoptLists(CallbackInfo ci) {
        VisibleSectionBuffers.adopt(this.visibleSections, this.nearbyVisibleSections);
    }

    @Overwrite
    public void clearVisibleSections() {
        SectionSubset.beginVisibleChange();
        VisibleSectionBuffers.beginRefill();
        SectionSubset.endVisibleChange();
    }

    @Overwrite
    public ObjectArrayList<SectionRenderDispatcher.RenderSection> visibleSections() {
        return VisibleSectionBuffers.visible();
    }

    @Overwrite
    public ObjectArrayList<SectionRenderDispatcher.RenderSection> nearbyVisibleSections() {
        return VisibleSectionBuffers.nearby();
    }

    @Override
    public void txoptimizations$bindRenderSections() {
        int index = VisibleSectionBuffers.renderIndex();

        this.visibleSections       = VisibleSectionBuffers.visibleAt(index);
        this.nearbyVisibleSections = VisibleSectionBuffers.nearbyAt(index);
    }
}
