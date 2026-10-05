package com.teslicek.txoptimizations;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class BackgroundLog {

    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor(task -> {
        Thread thread = new Thread(task, "TxOptimizations log writer");

        thread.setDaemon(true);

        return thread;
    });

    private BackgroundLog() {
    }

    public static void write(Runnable log) {
        EXECUTOR.execute(log);
    }
}
