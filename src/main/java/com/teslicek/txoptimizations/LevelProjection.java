package com.teslicek.txoptimizations;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.renderpearl.api.buffers.GpuBufferSlice;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;

public final class LevelProjection {

    private static final Matrix4f MATRIX = new Matrix4f();

    private static GpuBufferSlice slice;

    private LevelProjection() {
    }

    public static void capture(Matrix4fc projection, GpuBufferSlice current) {
        MATRIX.set(projection);
        slice = current;
    }

    public static boolean isBound() {
        return RenderSystem.getProjectionMatrixBuffer() == slice;
    }

    public static Matrix4fc matrix() {
        return MATRIX;
    }
}
