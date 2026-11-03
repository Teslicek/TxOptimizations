package com.teslicek.txoptimizations.mixin;

import com.teslicek.txoptimizations.PhaseGroups;
import java.util.List;
import net.minecraft.client.renderer.feature.phase.SimpleFeatureRenderPhase;
import net.minecraft.client.renderer.feature.phase.TranslucentFeatureRenderPhase;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin({SimpleFeatureRenderPhase.class, TranslucentFeatureRenderPhase.class})
public abstract class FeatureRenderPhaseGroupsMixin implements PhaseGroups {

    @Unique
    private @Nullable List<?> txoptimizations$groups;

    @Override
    public @Nullable List<?> txoptimizations$groups() {
        return this.txoptimizations$groups;
    }

    @Override
    public void txoptimizations$setGroups(List<?> groups) {
        this.txoptimizations$groups = groups;
    }
}
