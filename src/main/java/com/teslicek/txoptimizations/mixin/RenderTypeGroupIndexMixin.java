package com.teslicek.txoptimizations.mixin;

import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.util.List;
import net.minecraft.client.renderer.rendertype.PreparedRenderType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(targets = "net.minecraft.client.renderer.feature.RenderTypeFeatureRenderer$Group")
public abstract class RenderTypeGroupIndexMixin {

    @Unique
    private static final int NOT_FOUND = -1;

    @Unique
    private static final int LINEAR_SEARCH_LIMIT = 8;

    @Unique
    private Object2IntOpenHashMap<PreparedRenderType> txoptimizations$index;

    @Redirect(method = "getOrAddDraw", at = @At(value = "INVOKE", target = "Ljava/util/List;indexOf(Ljava/lang/Object;)I"))
    private int txoptimizations$findIndexed(List<PreparedRenderType> list, Object type) {
        if (this.txoptimizations$index == null)
            return list.indexOf(type);

        return this.txoptimizations$index.getInt(type);
    }

    @Redirect(method = "getOrAddDraw", at = @At(value = "INVOKE", target = "Ljava/util/List;add(Ljava/lang/Object;)Z", ordinal = 1))
    private boolean txoptimizations$indexAdded(List<PreparedRenderType> list, Object type) {
        boolean added = list.add((PreparedRenderType) type);

        if (this.txoptimizations$index != null) {
            this.txoptimizations$index.putIfAbsent((PreparedRenderType) type, list.size() - 1);

            return added;
        }

        if (list.size() > LINEAR_SEARCH_LIMIT) {
            Object2IntOpenHashMap<PreparedRenderType> index = new Object2IntOpenHashMap<>(list.size() * 2);

            index.defaultReturnValue(NOT_FOUND);

            for (int position = 0; position < list.size(); position ++)
                index.putIfAbsent(list.get(position), position);

            this.txoptimizations$index = index;
        }

        return added;
    }
}
