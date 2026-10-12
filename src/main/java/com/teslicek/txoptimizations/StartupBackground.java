package com.teslicek.txoptimizations;

import com.mojang.datafixers.DSL;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import net.minecraft.CrashReport;
import net.minecraft.ReportType;
import net.minecraft.util.MemoryReserve;
import net.minecraft.util.datafix.DataFixers;

public final class StartupBackground {

    private StartupBackground() {
    }

    public static CompletableFuture<?> optimizeDataFixers(Set<DSL.TypeReference> types) {
        CompletableFuture<CompletableFuture<?>> started = new CompletableFuture<>();

        Thread.ofPlatform().daemon().name("TxOptimizations DataFixers").start(() -> {
            try {
                started.complete(DataFixers.optimize(types));
            } catch (Throwable throwable) {
                started.completeExceptionally(throwable);
                throw throwable;
            }
        });
        return started.thenCompose(optimized -> optimized.thenApply(result -> result));
    }

    public static void preloadCrashReport() {
        MemoryReserve.allocate();
        Thread.ofPlatform().daemon().name("TxOptimizations Crash Report Preload").start(() -> new CrashReport("Don't panic!", new Throwable()).getFriendlyReport(ReportType.CRASH));
    }
}
