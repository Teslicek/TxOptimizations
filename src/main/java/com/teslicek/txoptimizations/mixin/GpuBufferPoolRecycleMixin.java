package com.teslicek.txoptimizations.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.renderpearl.api.buffers.GpuBuffer;
import com.teslicek.txoptimizations.RecyclePolling;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Predicate;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net.minecraft.client.renderer.StagedVertexBuffer$GpuBufferPool")
public abstract class GpuBufferPoolRecycleMixin {

    @Unique
    private static final int SPARE_FRAMES = 3;

    @Shadow
    @Final
    private List<GpuBuffer> available;

    @Unique
    private Map<GpuBuffer, Integer> txoptimizations$idleFrames = new IdentityHashMap<>();

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

    @Inject(method = "endFrame", at = @At("HEAD"))
    private void txoptimizations$forgetReusedSpares(CallbackInfo ci) {
        if (this.available.isEmpty())
            this.txoptimizations$idleFrames.clear();
    }

    @WrapOperation(method = "endFrame", at = @At(value = "INVOKE", target = "Ljava/util/List;forEach(Ljava/util/function/Consumer;)V"))
    private void txoptimizations$closeExpiredSpares(List<GpuBuffer> spares, Consumer<? super GpuBuffer> close, Operation<Void> original) {
        Map<GpuBuffer, Integer> idleFrames = new IdentityHashMap<>();
        List<GpuBuffer>         expired    = new ArrayList<>();
        Iterator<GpuBuffer>     iterator   = spares.iterator();

        while (iterator.hasNext()) {
            GpuBuffer spare  = iterator.next();
            int       frames = this.txoptimizations$idleFrames.getOrDefault(spare, 0) + 1;

            if (frames > SPARE_FRAMES) {
                expired.add(spare);
                iterator.remove();
                continue;
            }

            idleFrames.put(spare, frames);
        }

        this.txoptimizations$idleFrames = idleFrames;
        original.call(expired, close);
    }

    @WrapOperation(method = "endFrame", at = @At(value = "INVOKE", target = "Ljava/util/List;clear()V"))
    private void txoptimizations$keepSpares(List<GpuBuffer> buffers, Operation<Void> original) {
        if (buffers == this.available)
            return;

        original.call(buffers);
    }
}
