package com.teslicek.txoptimizations;

import com.mojang.logging.LogUtils;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import org.slf4j.Logger;

public final class ReloadTimeline implements PreparableReloadListener.PreparationBarrier {

    private static final Logger LOGGER = LogUtils.getLogger();

    private static volatile Map<String, Long> times;
    private static volatile long              startNanos;

    private final PreparableReloadListener.PreparationBarrier barrier;
    private final String                                      name;

    public ReloadTimeline(PreparableReloadListener.PreparationBarrier barrier, String name) {
        this.barrier = barrier;
        this.name    = name;
    }

    @Override
    public <T> CompletableFuture<T> wait(T value) {
        mark(this.name);

        return this.barrier.wait(value);
    }

    public static void begin() {
        startNanos = System.nanoTime();
        times      = new ConcurrentHashMap<>();
    }

    public static void mark(String name) {
        Map<String, Long> current = times;

        if (current == null)
            return;

        current.put(name, System.nanoTime() - startNanos);
    }

    public static void finish() {
        Map<String, Long> current = times;

        if (current == null)
            throw new IllegalStateException("A profiled reload finished without a timeline");

        times = null;
        LOGGER.info("Reload preparation finished, in order:");
        current.entrySet().stream()
            .sorted(Map.Entry.comparingByValue())
            .forEach(entry -> LOGGER.info("  {} ms  {}", TimeUnit.NANOSECONDS.toMillis(entry.getValue()), entry.getKey()));
    }
}
