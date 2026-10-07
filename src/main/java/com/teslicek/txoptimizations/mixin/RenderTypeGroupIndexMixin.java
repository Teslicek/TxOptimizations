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
    private Object2IntOpenHashMap<PreparedRenderType> txoptimizations$index;

    @Redirect(method = "getOrAddDraw", at = @At(value = "INVOKE", target = "Ljava/util/List;indexOf(Ljava/lang/Object;)I"))
    private int txoptimizations$findIndexed(List<PreparedRenderType> list, Object type) {
        if (this.txoptimizations$index == null)
            return NOT_FOUND;

        return this.txoptimizations$index.getInt(type);
    }

    @Redirect(method = "getOrAddDraw", at = @At(value = "INVOKE", target = "Ljava/util/List;add(Ljava/lang/Object;)Z", ordinal = 1))
    private boolean txoptimizations$indexAdded(List<PreparedRenderType> list, Object type) {
        if (this.txoptimizations$index == null) {
            this.txoptimizations$index = new Object2IntOpenHashMap<>();
            this.txoptimizations$index.defaultReturnValue(NOT_FOUND);
        }

        this.txoptimizations$index.putIfAbsent((PreparedRenderType) type, list.size());

        return list.add((PreparedRenderType) type);
    }
}
