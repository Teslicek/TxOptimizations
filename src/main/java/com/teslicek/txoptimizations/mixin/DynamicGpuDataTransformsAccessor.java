package com.teslicek.txoptimizations.mixin;

import net.minecraft.client.renderer.DynamicGpuData;
import net.minecraft.client.renderer.DynamicGpuDataStorageMapped;
import org.joml.Vector3fc;
import org.joml.Vector4fc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(DynamicGpuData.class)
public interface DynamicGpuDataTransformsAccessor {

    @Accessor("transforms")
    DynamicGpuDataStorageMapped<DynamicGpuData.Transform> txoptimizations$transforms();

    @Accessor("WHITE")
    static Vector4fc txoptimizations$white() {
        throw new AssertionError();
    }

    @Accessor("NO_OFFSET")
    static Vector3fc txoptimizations$noOffset() {
        throw new AssertionError();
    }
}
