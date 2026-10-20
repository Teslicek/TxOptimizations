package com.teslicek.txoptimizations.bake;

import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import net.minecraft.core.SectionPos;

public final class ChunkTasks {

    private static final Map<SectionPos, Queue<Runnable>> TASKS = new ConcurrentHashMap<>();

    private ChunkTasks() {
    }

    public static void add(SectionPos section, Runnable task) {
        TASKS.computeIfAbsent(section, _ -> new ConcurrentLinkedQueue<>()).add(task);
    }

    public static void run(long sectionNode) {
        if (TASKS.isEmpty())
            return;

        Queue<Runnable> tasks = TASKS.remove(SectionPos.of(sectionNode));

        if (tasks == null)
            return;

        for (Runnable task = tasks.poll(); task != null; task = tasks.poll())
            task.run();
    }
}
