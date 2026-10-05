package com.teslicek.txoptimizations.mixin;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.renderpearl.api.buffers.GpuBufferSlice;
import com.mojang.renderpearl.api.commands.CommandEncoder;
import java.nio.ByteBuffer;
import net.minecraft.client.renderer.ProjectionMatrixBuffer;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ProjectionMatrixBuffer.class)
public abstract class ProjectionMatrixBufferSkipMixin {

    @Unique
    private Matrix4f txoptimizations$uploaded;

    @WrapWithCondition(method = "writeBuffer", at = @At(value = "INVOKE", target = "Lcom/mojang/renderpearl/api/commands/CommandEncoder;writeToBuffer(Lcom/mojang/renderpearl/api/buffers/GpuBufferSlice;Ljava/nio/ByteBuffer;)V"))
    private boolean txoptimizations$skipUnchanged(CommandEncoder encoder, GpuBufferSlice destination, ByteBuffer data, @Local(argsOnly = true) Matrix4f projectionMatrix) {
        if (this.txoptimizations$uploaded == null)
            this.txoptimizations$uploaded = new Matrix4f(projectionMatrix);
        else if (txoptimizations$sameBits(this.txoptimizations$uploaded, projectionMatrix))
            return false;
        else
            this.txoptimizations$uploaded.set(projectionMatrix);

        return true;
    }

    @Unique
    private static boolean txoptimizations$sameBits(Matrix4f a, Matrix4f b) {
        return txoptimizations$same(a.m00(), b.m00()) && txoptimizations$same(a.m01(), b.m01()) && txoptimizations$same(a.m02(), b.m02()) && txoptimizations$same(a.m03(), b.m03())
            && txoptimizations$same(a.m10(), b.m10()) && txoptimizations$same(a.m11(), b.m11()) && txoptimizations$same(a.m12(), b.m12()) && txoptimizations$same(a.m13(), b.m13())
            && txoptimizations$same(a.m20(), b.m20()) && txoptimizations$same(a.m21(), b.m21()) && txoptimizations$same(a.m22(), b.m22()) && txoptimizations$same(a.m23(), b.m23())
            && txoptimizations$same(a.m30(), b.m30()) && txoptimizations$same(a.m31(), b.m31()) && txoptimizations$same(a.m32(), b.m32()) && txoptimizations$same(a.m33(), b.m33());
    }

    @Unique
    private static boolean txoptimizations$same(float a, float b) {
        return Float.floatToRawIntBits(a) == Float.floatToRawIntBits(b);
    }
}
