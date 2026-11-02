package com.teslicek.txoptimizations.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import java.util.concurrent.ConcurrentHashMap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(targets = "net.minecraft.util.Util$11")
public abstract class UtilPairMemoizeMixin {

    @Unique
    private final ConcurrentHashMap<Object, ConcurrentHashMap<Object, Object>> txoptimizations$byFirst = new ConcurrentHashMap<>();

    @WrapMethod(method = "apply")
    private Object txoptimizations$lookupWithoutPair(Object first, Object second, Operation<Object> original) {
        if (first == null || second == null)
            return original.call(first, second);

        ConcurrentHashMap<Object, Object> bySecond = this.txoptimizations$byFirst.get(first);

        if (bySecond != null) {
            Object cached = bySecond.get(second);

            if (cached != null)
                return cached;
        }

        Object result = original.call(first, second);

        if (result != null)
            this.txoptimizations$byFirst.computeIfAbsent(first, _ -> new ConcurrentHashMap<>()).putIfAbsent(second, result);

        return result;
    }
}
