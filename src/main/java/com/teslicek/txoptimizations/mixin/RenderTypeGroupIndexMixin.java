package com.teslicek.txoptimizations.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.renderer.rendertype.PreparedRenderType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(targets = "net.minecraft.client.renderer.feature.RenderTypeFeatureRenderer$Group")
public abstract class RenderTypeGroupIndexMixin {

    @Unique
    private final Map<PreparedRenderType, Integer> txoptimizations$index = new HashMap<>();

    @WrapOperation(method = "getOrAddDraw", at = @At(value = "INVOKE", target = "Ljava/util/List;indexOf(Ljava/lang/Object;)I"))
    private int txoptimizations$findIndexed(List<PreparedRenderType> list, Object type, Operation<Integer> original) {
        Integer index = this.txoptimizations$index.get(type);

        return index == null ? -1 : index;
    }

    @WrapOperation(method = "getOrAddDraw", at = @At(value = "INVOKE", target = "Ljava/util/List;add(Ljava/lang/Object;)Z", ordinal = 1))
    private boolean txoptimizations$indexAdded(List<PreparedRenderType> list, Object type, Operation<Boolean> original) {
        this.txoptimizations$index.putIfAbsent((PreparedRenderType) type, list.size());

        return original.call(list, type);
    }
}
