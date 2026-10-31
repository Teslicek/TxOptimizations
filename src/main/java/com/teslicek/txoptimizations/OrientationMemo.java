package com.teslicek.txoptimizations;

import org.joml.Quaternionf;

public final class OrientationMemo {

    private static boolean known;
    private static int     angleX;
    private static int     angleY;
    private static int     angleZ;
    private static float   x;
    private static float   y;
    private static float   z;
    private static float   w;

    private OrientationMemo() {
    }

    public static Quaternionf rotationYXZ(Quaternionf target, float angleYValue, float angleXValue, float angleZValue) {
        if (!RenderThreadStack.isOwner())
            return target.rotationYXZ(angleYValue, angleXValue, angleZValue);

        int bitsY = Float.floatToRawIntBits(angleYValue);
        int bitsX = Float.floatToRawIntBits(angleXValue);
        int bitsZ = Float.floatToRawIntBits(angleZValue);

        if (known && bitsY == angleY && bitsX == angleX && bitsZ == angleZ)
            return target.set(x, y, z, w);

        target.rotationYXZ(angleYValue, angleXValue, angleZValue);
        known  = true;
        angleY = bitsY;
        angleX = bitsX;
        angleZ = bitsZ;
        x      = target.x;
        y      = target.y;
        z      = target.z;
        w      = target.w;

        return target;
    }
}
