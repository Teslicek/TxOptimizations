package com.teslicek.txoptimizations.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.teslicek.txoptimizations.RecyclePolling;
import java.util.Iterator;
import java.util.List;
import java.util.function.Predicate;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(targets = "net.minecraft.client.renderer.StagedVertexBuffer$GpuBufferPool")
public abstract class GpuBufferPoolRecycleMixin {

    @WrapOperation(method = "tryRecycleBuffers", at = @At(value = "INVOKE", target = "Ljava/util/List;removeIf(Ljava/util/function/Predicate;)Z"))
    private boolean txoptimizations$recycleUntilFirstPending(List<Object> pendingRecycle, Predicate<Object> tryRecycle, Operation<Boolean> original) {
        RecyclePolling.setActive(true);

        try {
            boolean recycled = false;
            Iterator<Object> iterator = pendingRecycle.iterator();

            while (iterator.hasNext()) {
                if (!tryRecycle.test(iterator.next()))
                    return recycled;

                iterator.remove();
                recycled = true;
            }

            return recycled;
        } finally {
            RecyclePolling.setActive(false);
        }
    }
}
