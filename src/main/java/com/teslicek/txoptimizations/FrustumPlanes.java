package com.teslicek.txoptimizations;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import org.joml.FrustumIntersection;

public final class FrustumPlanes {

    private static final String[]    FIELDS   = {"nxX", "nxY", "nxZ", "nxW", "pxX", "pxY", "pxZ", "pxW", "nyX", "nyY", "nyZ", "nyW", "pyX", "pyY", "pyZ", "pyW", "nzX", "nzY", "nzZ", "nzW", "pzX", "pzY", "pzZ", "pzW"};
    private static final VarHandle[] PLANES   = planes();
    private static final int         VALUES   = 4;
    private static final double      MARGIN   = 1.0E-6;
    private static final int         CAMERA   = FIELDS.length;
    private static final int         CAPTURED = CAMERA + 3;

    private FrustumPlanes() {
    }

    public static double[] newPlanes() {
        return new double[CAPTURED];
    }

    public static void capture(FrustumIntersection intersection, double camX, double camY, double camZ, double[] planes) {
        for (int index = 0; index < PLANES.length; index ++)
            planes[index] = (float) PLANES[index].get(intersection);

        planes[CAMERA]     = camX;
        planes[CAMERA + 1] = camY;
        planes[CAMERA + 2] = camZ;
    }

    public static int classifyPoints(double[] planes, double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        double x0     = minX - planes[CAMERA];
        double y0     = minY - planes[CAMERA + 1];
        double z0     = minZ - planes[CAMERA + 2];
        double x1     = maxX - planes[CAMERA];
        double y1     = maxY - planes[CAMERA + 1];
        double z1     = maxZ - planes[CAMERA + 2];
        double reach  = Math.max(Math.max(Math.max(Math.abs(x0), Math.abs(x1)), Math.max(Math.abs(y0), Math.abs(y1))), Math.max(Math.abs(z0), Math.abs(z1)));
        int    result = FrustumBoxTest.INSIDE;

        for (int plane = 0; plane < PLANES.length; plane += VALUES) {
            double a      = planes[plane];
            double b      = planes[plane + 1];
            double c      = planes[plane + 2];
            double w      = planes[plane + 3];
            double lowest = w + Math.min(a * x0, a * x1) + Math.min(b * y0, b * y1) + Math.min(c * z0, c * z1);
            double most   = w + Math.max(a * x0, a * x1) + Math.max(b * y0, b * y1) + Math.max(c * z0, c * z1);
            double margin = MARGIN * ((Math.abs(a) + Math.abs(b) + Math.abs(c)) * reach + Math.abs(w));

            if (most < -margin)
                return FrustumBoxTest.OUTSIDE;

            if (!(lowest > margin))
                result = FrustumBoxTest.MIXED;
        }

        return result;
    }

    private static VarHandle[] planes() {
        try {
            MethodHandles.Lookup lookup = MethodHandles.privateLookupIn(FrustumIntersection.class, MethodHandles.lookup());
            VarHandle[]          planes = new VarHandle[FIELDS.length];

            for (int index = 0; index < FIELDS.length; index ++)
                planes[index] = lookup.findVarHandle(FrustumIntersection.class, FIELDS[index], float.class);

            return planes;
        } catch (ReflectiveOperationException missing) {
            throw new IllegalStateException("JOML's FrustumIntersection no longer has the plane fields the particle block test reads", missing);
        }
    }
}
