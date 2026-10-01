package com.teslicek.txoptimizations.mixin;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.renderer.feature.FeatureRenderer;
import net.minecraft.client.renderer.feature.FeatureRendererMap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FeatureRendererMap.class)
public abstract class FeatureRendererMapValuesMixin {

    @Shadow
    private FeatureRenderer<?>[] renderers;

    @Unique
    private List<FeatureRenderer<?>> txoptimizations$values = List.of();

    @Inject(method = "put", at = @At("RETURN"))
    private void txoptimizations$rebuildValues(CallbackInfo ci) {
        List<FeatureRenderer<?>> values = new ArrayList<>(this.renderers.length);

        for (FeatureRenderer<?> renderer : this.renderers)
            if (renderer != null)
                values.add(renderer);

        this.txoptimizations$values = List.copyOf(values);
    }

    @Overwrite
    public Iterable<FeatureRenderer<?>> values() {
        return this.txoptimizations$values;
    }
}
