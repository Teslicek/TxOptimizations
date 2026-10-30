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
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net.minecraft.client.renderer.StagedVertexBuffer$GpuBufferPool")
public abstract class GpuBufferPoolRecycleMixin {

    @Unique
    private static final long SPARE_NANOS = 1_000_000_000L;

    @Shadow
    @Final
    private List<GpuBuffer> available;

    @Unique
    private Map<GpuBuffer, Long> txoptimizations$idleSince = new IdentityHashMap<>();

    @Unique
    private Map<GpuBuffer, Long> txoptimizations$nextIdleSince = new IdentityHashMap<>();

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
        if (this.available.isEmpty() && !this.txoptimizations$idleSince.isEmpty())
            this.txoptimizations$idleSince.clear();
    }

    @WrapOperation(method = "endFrame", at = @At(value = "INVOKE", target = "Ljava/util/List;forEach(Ljava/util/function/Consumer;)V"))
    private void txoptimizations$closeExpiredSpares(List<GpuBuffer> spares, Consumer<? super GpuBuffer> close, Operation<Void> original) {
        Map<GpuBuffer, Long> idleSince = this.txoptimizations$nextIdleSince;
        List<GpuBuffer>      expired   = null;
        Iterator<GpuBuffer>  iterator  = spares.iterator();
        long                 now       = System.nanoTime();

        while (iterator.hasNext()) {
            GpuBuffer spare = iterator.next();
            Long      since = this.txoptimizations$idleSince.get(spare);
            long      start = since == null ? now : since;

            if (now - start <= SPARE_NANOS) {
                idleSince.put(spare, start);
                continue;
            }

            if (expired == null)
                expired = new ArrayList<>();

            expired.add(spare);
            iterator.remove();
        }

        this.txoptimizations$nextIdleSince = this.txoptimizations$idleSince;
        this.txoptimizations$idleSince     = idleSince;

        if (!this.txoptimizations$nextIdleSince.isEmpty())
            this.txoptimizations$nextIdleSince.clear();

        if (expired != null)
            original.call(expired, close);
    }

    @ModifyArg(method = "acquire", at = @At(value = "INVOKE", target = "Lcom/mojang/renderpearl/api/device/GpuDevice;createBuffer(Ljava/util/function/Supplier;IJ)Lcom/mojang/renderpearl/api/buffers/GpuBuffer;"), index = 2, require = 1)
    private long txoptimizations$growGeometrically(long size) {
        return Long.highestOneBit(size - 1) << 1;
    }

    @Redirect(method = "endFrame", at = @At(value = "INVOKE", target = "Ljava/util/List;clear()V"))
    private void txoptimizations$keepSpares(List<GpuBuffer> buffers) {
        if (buffers == this.available)
            return;

        buffers.clear();
    }
}
