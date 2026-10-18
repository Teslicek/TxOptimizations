package com.teslicek.txoptimizations.mixin;

import com.llamalad7.mixinextras.sugar.Local;
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

    @Redirect(method = "extract", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/SectionUpdateTracker;getDirtyState(J)Lnet/minecraft/client/SectionUpdateTracker$SectionDirtyState;"))
    private SectionUpdateTracker.SectionDirtyState txoptimizations$dirtyStateByIndex(SectionUpdateTracker tracker, long sectionNode, @Local SectionRenderDispatcher.RenderSection section) {
        SectionUpdateTracker.SectionDirtyState state = txoptimizations$stateAtIndex(tracker.storage, section.index, sectionNode);

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
