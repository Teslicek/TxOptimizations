package com.teslicek.txoptimizations.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import com.teslicek.txoptimizations.NeighborMark;
import com.teslicek.txoptimizations.SectionDirtyStates;
import com.teslicek.txoptimizations.SectionNeighbors;
import com.teslicek.txoptimizations.SectionSubset;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectListIterator;
import net.minecraft.client.SectionUpdateTracker;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import net.minecraft.client.renderer.extract.LevelExtractor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(LevelExtractor.class)
public abstract class LevelExtractorDirtySectionsMixin {

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
}
