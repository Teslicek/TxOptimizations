package com.teslicek.txoptimizations;

import java.util.List;
import net.minecraft.client.renderer.feature.phase.FeatureRenderPhase;
import org.jspecify.annotations.Nullable;

public interface PhaseGroups {

    @Nullable List<?> txoptimizations$groups();

    void txoptimizations$setGroups(List<?> groups);

    static boolean hasGroups(FeatureRenderPhase<?> phase) {
        List<?> groups = require(phase).txoptimizations$groups();

        return groups != null && !groups.isEmpty();
    }

    static PhaseGroups require(FeatureRenderPhase<?> phase) {
        if (!(phase instanceof PhaseGroups groups))
            throw new IllegalStateException("Feature render phase " + phase.getClass().getName() + " does not track its prepared groups");

        return groups;
    }
}
