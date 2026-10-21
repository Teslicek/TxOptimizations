package com.teslicek.txoptimizations;

import com.mojang.renderpearl.backend.vulkan.Destroyable;
import com.mojang.renderpearl.backend.vulkan.DestructionQueue;
import com.mojang.renderpearl.backend.vulkan.VulkanGpuBuffer;
import com.mojang.renderpearl.backend.vulkan.VulkanGpuTexture;
import com.mojang.renderpearl.backend.vulkan.VulkanGpuTextureView;
import com.teslicek.txoptimizations.gpu.GpuCallEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class BackgroundDestroyer implements DestructionQueue.Destroyer<Destroyable> {

    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor(task -> {
        Thread thread = new Thread(task, "TxOptimizations GPU destroyer");

        thread.setDaemon(true);

        return thread;
    });

    private static volatile CompletableFuture<Void> pending = CompletableFuture.completedFuture(null);

    private List<Destroyable> batch;

    @Override
    public void begin(int count) {
        throwFailure();
    }

    @Override
    public void destroy(Destroyable destroyable) {
        if (destroyable instanceof VulkanGpuBuffer.Direct || destroyable instanceof VulkanGpuTexture || destroyable instanceof VulkanGpuTextureView) {
            if (this.batch == null)
                this.batch = new ArrayList<>();

            this.batch.add(destroyable);

            return;
        }

        destroyable.destroy();
    }

    @Override
    public void end() {
        List<Destroyable> destroyables = this.batch;

        if (destroyables == null)
            return;

        this.batch = null;
        pending    = pending.thenRunAsync(() -> destroyAll(destroyables), EXECUTOR);
    }

    public static void drain() {
        pending.join();
    }

    private static void destroyAll(List<Destroyable> destroyables) {
        Runnable destroy = () -> destroyables.forEach(Destroyable::destroy);

        if (!GpuCallEvent.isRecording()) {
            destroy.run();

            return;
        }

        GpuCallEvent.time(GpuCallEvent.DESTROY, destroy);
    }

    private static void throwFailure() {
        if (pending.isCompletedExceptionally())
            pending.join();
    }
}
