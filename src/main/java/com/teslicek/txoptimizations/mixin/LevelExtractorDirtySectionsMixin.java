package com.teslicek.txoptimizations.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import com.teslicek.txoptimizations.SectionSubset;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectListIterator;
import net.minecraft.client.RotatingSectionStorage;
import net.minecraft.client.SectionUpdateTracker;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import net.minecraft.client.renderer.extract.LevelExtractor;
import net.minecraft.core.SectionPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
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
        SectionUpdateTracker.SectionDirtyState state = txoptimizations$dirtyState(tracker, section.index, sectionNode);

        if (state != null && state.isDirty())
            SectionSubset.DIRTY.record(section);

        return state;
    }

    @Unique
    private static SectionUpdateTracker.SectionDirtyState txoptimizations$dirtyState(SectionUpdateTracker tracker, int index, long sectionNode) {
        SectionUpdateTracker.SectionDirtyState state = txoptimizations$stateAtIndex(tracker.storage, index, sectionNode);

        if (state != null)
            return state;

        return tracker.getDirtyState(sectionNode);
    }

    @Unique
    private static SectionUpdateTracker.SectionDirtyState txoptimizations$stateAtIndex(RotatingSectionStorage<SectionUpdateTracker.SectionDirtyState> storage, int index, long sectionNode) {
        RotatingSectionStorage.Node<SectionUpdateTracker.SectionDirtyState>[] nodes = storage.nodes;

        if (index < 0 || index >= nodes.length)
            return null;

        SectionUpdateTracker.SectionDirtyState state = nodes[index].value();

        if (state.getSectionNode() != sectionNode || !txoptimizations$contains(storage, sectionNode))
            return null;

        return state;
    }

    @Unique
    private static boolean txoptimizations$contains(RotatingSectionStorage<?> storage, long sectionNode) {
        int y = SectionPos.y(sectionNode);

        if (y < storage.minY() || y > storage.maxY())
            return false;

        SectionPos center = storage.centerSectionPos();
        int        radius = storage.radius();
        int        x      = SectionPos.x(sectionNode);
        int        z      = SectionPos.z(sectionNode);

        return x >= center.x() - radius && x <= center.x() + radius && z >= center.z() - radius && z <= center.z() + radius;
    }
}
