package com.teslicek.txoptimizations.mixin;

import java.util.Objects;
import net.minecraft.client.renderer.DynamicGpuData;
import org.joml.Matrix4fc;
import org.joml.Vector3fc;
import org.joml.Vector4fc;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(DynamicGpuData.Transform.class)
public abstract class DynamicTransformEqualsMixin {

    @Shadow
    @Final
    private Matrix4fc modelView;

    @Shadow
    @Final
    private Vector4fc colorModulator;

    @Shadow
    @Final
    private Vector3fc modelOffset;

    @Shadow
    @Final
    private Matrix4fc textureMatrix;

    @Overwrite
    public final boolean equals(Object other) {
        if (this == other)
            return true;

        if (!(other instanceof DynamicGpuData.Transform that))
            return false;

        return Objects.equals(this.modelOffset, that.modelOffset()) && Objects.equals(this.colorModulator, that.colorModulator()) && Objects.equals(this.modelView, that.modelView()) && Objects.equals(this.textureMatrix, that.textureMatrix());
    }
}
