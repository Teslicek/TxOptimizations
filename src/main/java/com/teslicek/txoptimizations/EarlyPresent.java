package com.teslicek.txoptimizations;

import com.mojang.renderpearl.api.device.GpuSurface;

public final class EarlyPresent {

    private static GpuSurface surface;

    private EarlyPresent() {
    }

    public static void arm(GpuSurface frameSurface) {
        if (surface != null)
            throw new IllegalStateException("Early present is already armed");

        surface = frameSurface;
    }

    public static void disarm() {
        surface = null;
    }

    public static void presentArmed() {
        GpuSurface armed = surface;

        if (armed == null)
            return;

        surface = null;
        armed.present();
    }
}
