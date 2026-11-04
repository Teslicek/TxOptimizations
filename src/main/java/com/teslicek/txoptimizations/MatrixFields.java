package com.teslicek.txoptimizations;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import org.joml.Matrix4f;

public final class MatrixFields {

    private static final VarHandle M00 = find("m00");
    private static final VarHandle M01 = find("m01");
    private static final VarHandle M02 = find("m02");
    private static final VarHandle M03 = find("m03");
    private static final VarHandle M10 = find("m10");
    private static final VarHandle M11 = find("m11");
    private static final VarHandle M12 = find("m12");
    private static final VarHandle M13 = find("m13");
    private static final VarHandle M20 = find("m20");
    private static final VarHandle M21 = find("m21");
    private static final VarHandle M22 = find("m22");
    private static final VarHandle M23 = find("m23");
    private static final VarHandle M30 = find("m30");
    private static final VarHandle M31 = find("m31");
    private static final VarHandle M32 = find("m32");
    private static final VarHandle M33 = find("m33");

    private MatrixFields() {
    }

    public static void set(Matrix4f target, float m00, float m01, float m02, float m03, float m10, float m11, float m12, float m13, float m20, float m21, float m22, float m23, float m30, float m31, float m32, float m33, int properties) {
        M00.set(target, m00);
        M01.set(target, m01);
        M02.set(target, m02);
        M03.set(target, m03);
        M10.set(target, m10);
        M11.set(target, m11);
        M12.set(target, m12);
        M13.set(target, m13);
        M20.set(target, m20);
        M21.set(target, m21);
        M22.set(target, m22);
        M23.set(target, m23);
        M30.set(target, m30);
        M31.set(target, m31);
        M32.set(target, m32);
        M33.set(target, m33);
        target.assume(properties);
    }

    private static VarHandle find(String name) {
        try {
            return MethodHandles.privateLookupIn(Matrix4f.class, MethodHandles.lookup()).findVarHandle(Matrix4f.class, name, float.class);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Matrix4f field " + name + " not found", exception);
        }
    }
}
