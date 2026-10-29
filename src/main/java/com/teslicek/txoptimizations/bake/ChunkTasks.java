package com.teslicek.txoptimizations.bake;

import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import net.minecraft.core.SectionPos;

public final class ChunkTasks {

    private static final Map<SectionPos, Queue<Runnable>> PENDING = new ConcurrentHashMap<>();

    private ChunkTasks() {
    }

    public static void add(SectionPos section, Runnable task) {
        PENDING.computeIfAbsent(section, _ -> new ConcurrentLinkedQueue<>()).add(task);
    }

    public static Queue<Runnable> claim(SectionPos section) {
        return PENDING.remove(section);
    }

    public static void run(Queue<Runnable> tasks) {
        for (Runnable task = tasks.poll(); task != null; task = tasks.poll())
            task.run();
    }

    public static void clear() {
        PENDING.clear();
    }
}
