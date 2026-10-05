package com.teslicek.txoptimizations.gpu;

import java.util.Locale;

public final class ProfileCounters {

    private static long renderPasses;
    private static long pipelineSets;
    private static long draws;
    private static long multiDraws;
    private static long indirectDraws;
    private static long batchedDraws;

    private ProfileCounters() {
    }

    static void reset() {
        renderPasses  = 0L;
        pipelineSets  = 0L;
        draws         = 0L;
        multiDraws    = 0L;
        indirectDraws = 0L;
        batchedDraws  = 0L;
    }

    public static void countRenderPass() {
        if (GpuPassProfiler.isRecording())
            renderPasses ++;
    }

    public static void countPipelineSet() {
        if (GpuPassProfiler.isRecording())
            pipelineSets ++;
    }

    public static void countDraw() {
        if (GpuPassProfiler.isRecording())
            draws ++;
    }

    public static void countMultiDraw() {
        if (GpuPassProfiler.isRecording())
            multiDraws ++;
    }

    public static void countIndirectDraws(int count) {
        if (GpuPassProfiler.isRecording())
            indirectDraws += count;
    }

    public static void countBatchedDraws(int count) {
        if (GpuPassProfiler.isRecording())
            batchedDraws += count;
    }

    static String report(int frames) {
        return String.format(Locale.ROOT, "Per frame: %.1f render passes, %.1f pipeline sets, %.1f draws, %.1f multi-draw calls, %.1f indirect draws, %.1f batched draws%n", (double) renderPasses / frames, (double) pipelineSets / frames, (double) draws / frames, (double) multiDraws / frames, (double) indirectDraws / frames, (double) batchedDraws / frames);
    }
}
