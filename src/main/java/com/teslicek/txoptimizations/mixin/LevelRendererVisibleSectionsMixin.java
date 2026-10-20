package com.teslicek.txoptimizations.mixin;

import com.teslicek.txoptimizations.SectionSubset;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = LevelRenderer.class, priority = 999)
public abstract class LevelRendererVisibleSectionsMixin {

    @Shadow
    @Final
    private ObjectArrayList<SectionRenderDispatcher.RenderSection> visibleSections;

    @Shadow
    @Final
    private ObjectArrayList<SectionRenderDispatcher.RenderSection> nearbyVisibleSections;

    @Overwrite
    public void clearVisibleSections() {
        SectionSubset.beginVisibleChange();
        this.visibleSections.clear();
        this.nearbyVisibleSections.clear();
        SectionSubset.endVisibleChange();
    }
}
