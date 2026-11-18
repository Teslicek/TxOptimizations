package com.teslicek.txoptimizations.mixin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import net.minecraft.client.renderer.feature.phase.SimpleFeatureRenderPhase;
import net.minecraft.client.renderer.rendertype.RenderType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(SimpleFeatureRenderPhase.class)
public abstract class SimpleFeaturePhasePipelineOrderMixin {

    @Unique
    private static final List<Object> ORDERED = new ArrayList<>();

    @Unique
    private static final List<Object> UNORDERED = new ArrayList<>();

    @Unique
    private static long[] txoptimizations$order = new long[256];

    @Redirect(method = "sortFeatureInto", at = @At(value = "INVOKE", target = "Ljava/util/Map;values()Ljava/util/Collection;"), require = 1)
    private static Collection<?> txoptimizations$groupByPipeline(Map<Object, List<?>> batches) {
        if (batches.size() < 2)
            return batches.values();

        int size = batches.size();

        if (txoptimizations$order.length < size)
            txoptimizations$order = new long[size * 2];

        long[] order = txoptimizations$order;
        int    index = 0;

        UNORDERED.clear();

        for (Map.Entry<Object, List<?>> entry : batches.entrySet()) {
            int pipeline = entry.getKey() instanceof RenderType renderType ? renderType.pipeline().getSortKey() : 0;

            UNORDERED.add(entry.getValue());
            order[index] = (long) pipeline << Integer.SIZE | index;
            index ++;
        }

        Arrays.sort(order, 0, size);
        ORDERED.clear();

        for (int position = 0; position < size; position ++)
            ORDERED.add(UNORDERED.get((int) order[position]));

        return ORDERED;
    }
}
