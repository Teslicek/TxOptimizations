package com.teslicek.txoptimizations.mixin;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import net.minecraft.client.renderer.feature.FeatureFrameContext;
import net.minecraft.client.renderer.feature.QuadParticleFeatureRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(QuadParticleFeatureRenderer.class)
public abstract class QuadParticleFeatureMapPoolMixin {

    @Unique
    private static final int UNRESIZED_ENTRIES = 21;

    @Unique
    private List<IdentityHashMap<?, ?>> txoptimizations$freeMaps;

    @Unique
    private List<IdentityHashMap<?, ?>> txoptimizations$lentMaps;

    @Redirect(method = "prepareGroup", at = @At(value = "NEW", target = "()Ljava/util/IdentityHashMap;"))
    private IdentityHashMap<?, ?> txoptimizations$lendMap() {
        if (this.txoptimizations$freeMaps == null) {
            this.txoptimizations$freeMaps = new ArrayList<>();
            this.txoptimizations$lentMaps = new ArrayList<>();
        }

        IdentityHashMap<?, ?> map = this.txoptimizations$freeMaps.isEmpty() ? new IdentityHashMap<>() : this.txoptimizations$freeMaps.removeLast();

        this.txoptimizations$lentMaps.add(map);

        return map;
    }

    @Inject(method = "finishExecute", at = @At("HEAD"))
    private void txoptimizations$returnMaps(FeatureFrameContext context, CallbackInfo ci) {
        if (this.txoptimizations$lentMaps == null)
            return;

        for (int index = 0; index < this.txoptimizations$lentMaps.size(); index ++) {
            IdentityHashMap<?, ?> map = this.txoptimizations$lentMaps.get(index);

            if (map.size() > UNRESIZED_ENTRIES)
                continue;

            if (!map.isEmpty())
                map.clear();

            this.txoptimizations$freeMaps.add(map);
        }

        this.txoptimizations$lentMaps.clear();
    }
}
