package com.teslicek.txoptimizations.bake;

import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import net.minecraft.core.SectionPos;
import org.jspecify.annotations.Nullable;

public final class ChunkTasks {

    private static final Map<SectionPos, Queue<Runnable>> PENDING = new ConcurrentHashMap<>();

    private static volatile long generation;

    private ChunkTasks() {
    }

    public static void add(SectionPos section, Runnable task) {
        PENDING.computeIfAbsent(section, _ -> new ConcurrentLinkedQueue<>()).add(task);
    }

    public static @Nullable Claim claim(SectionPos section) {
        Queue<Runnable> tasks = PENDING.remove(section);

        return tasks == null ? null : new Claim(section, tasks, generation);
    }

    public static void run(SectionTasks tasks) {
        Claim claim = tasks.txoptimizations$getClaim();

        if (claim != null)
            drain(claim.tasks());

        drain(tasks.txoptimizations$getSectionTasks());
    }

    public static void restore(SectionTasks tasks) {
        Claim claim = tasks.txoptimizations$getClaim();

        if (claim == null || claim.tasks().isEmpty() || claim.generation() != generation)
            return;

        PENDING.compute(claim.section(), (_, newer) -> {
            Queue<Runnable> restored = new ConcurrentLinkedQueue<>(claim.tasks());

            claim.tasks().clear();

            if (newer != null)
                restored.addAll(newer);

            return restored;
        });
        Baking.rebuild(claim.section().origin(), false);
    }

    public static void clear() {
        generation ++;
        PENDING.clear();
    }

    private static void drain(Queue<Runnable> tasks) {
        for (Runnable task = tasks.poll(); task != null; task = tasks.poll())
            task.run();
    }

    public record Claim(SectionPos section, Queue<Runnable> tasks, long generation) {
    }
}
