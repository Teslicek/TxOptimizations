package com.teslicek.txoptimizations;

import com.mojang.renderpearl.backend.vulkan.Destroyable;
import com.mojang.renderpearl.backend.vulkan.DestructionQueue;
import com.mojang.renderpearl.backend.vulkan.VulkanGpuBuffer;
import com.mojang.renderpearl.backend.vulkan.VulkanGpuTexture;
import com.mojang.renderpearl.backend.vulkan.VulkanGpuTextureView;
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

    private List<Destroyable> batch = new ArrayList<>();

    @Override
    public void begin(int count) {
        throwFailure();
        this.batch = new ArrayList<>(count);
    }

    @Override
    public void destroy(Destroyable destroyable) {
        if (destroyable instanceof VulkanGpuBuffer.Direct || destroyable instanceof VulkanGpuTexture || destroyable instanceof VulkanGpuTextureView) {
            this.batch.add(destroyable);

            return;
        }

        destroyable.destroy();
    }

    @Override
    public void end() {
        if (this.batch.isEmpty())
            return;

        List<Destroyable> destroyables = this.batch;

        this.batch = new ArrayList<>();
        pending    = pending.thenRunAsync(() -> destroyables.forEach(Destroyable::destroy), EXECUTOR);
    }

    public static void drain() {
        pending.join();
    }

    private static void throwFailure() {
        if (pending.isCompletedExceptionally())
            pending.join();
    }
}
