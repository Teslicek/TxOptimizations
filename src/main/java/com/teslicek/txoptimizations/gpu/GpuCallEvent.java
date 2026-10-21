package com.teslicek.txoptimizations.gpu;

import jdk.jfr.Category;
import jdk.jfr.Enabled;
import jdk.jfr.Event;
import jdk.jfr.Label;
import jdk.jfr.Name;
import jdk.jfr.StackTrace;

@Name(GpuCallEvent.NAME)
@Label("GPU driver call")
@Category("TxOptimizations")
@Enabled(false)
@StackTrace(false)
public final class GpuCallEvent extends Event {

    public static final String START   = "present thread start";
    public static final String SUBMIT  = "queue submit";
    public static final String PRESENT = "queue present";
    public static final String DESTROY = "destroy";

    static final String NAME = "txoptimizations.GpuCall";

    private static volatile boolean recording;

    @Label("Call")
    private String call;

    public GpuCallEvent(String call) {
        this.call = call;
    }

    public static boolean isRecording() {
        return recording;
    }

    public static void time(String call, Runnable action) {
        GpuCallEvent event = new GpuCallEvent(call);

        event.begin();
        action.run();
        event.commit();
    }

    static void setRecording(boolean value) {
        recording = value;
    }
}
