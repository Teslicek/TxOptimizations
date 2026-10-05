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

    private final PreparableReloadListener.PreparationBarrier barrier;
    private final String                                      name;
    private final long                                        startNanos;
    private final Map<String, Long>                           prepared;

    public ReloadTimeline(PreparableReloadListener.PreparationBarrier barrier, String name, long startNanos, Map<String, Long> prepared) {
        this.barrier    = barrier;
        this.name       = name;
        this.startNanos = startNanos;
        this.prepared   = prepared;
    }

    @Override
    public <T> CompletableFuture<T> wait(T value) {
        this.prepared.put(this.name, System.nanoTime() - this.startNanos);

        return this.barrier.wait(value);
    }

    public static Map<String, Long> newTimes() {
        return new ConcurrentHashMap<>();
    }

    public static void log(Map<String, Long> prepared) {
        LOGGER.info("Reload preparation finished, in order:");
        prepared.entrySet().stream()
            .sorted(Map.Entry.comparingByValue())
            .forEach(entry -> LOGGER.info("  {} ms  {}", TimeUnit.NANOSECONDS.toMillis(entry.getValue()), entry.getKey()));
    }
}
