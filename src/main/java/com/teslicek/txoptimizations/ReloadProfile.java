package com.teslicek.txoptimizations;

public final class ReloadProfile {

    private static boolean armed;

    private ReloadProfile() {
    }

    public static void arm() {
        armed = true;
    }

    public static boolean consume() {
        boolean profile = armed;

        armed = false;

        return profile;
    }
}
