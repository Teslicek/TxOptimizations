package com.teslicek.txoptimizations.mixin;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.teslicek.txoptimizations.GpuCubeBuilder;
import com.teslicek.txoptimizations.GpuCubeDraw;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(BufferBuilder.class)
public abstract class BufferBuilderGpuCubesMixin implements GpuCubeBuilder {

    @Unique
    private @Nullable GpuCubeDraw txoptimizations$cubeDraw;

    @Override
    public @Nullable GpuCubeDraw txoptimizations$cubeDraw() {
        return this.txoptimizations$cubeDraw;
    }

    @Override
    public void txoptimizations$setCubeDraw(@Nullable GpuCubeDraw draw) {
        this.txoptimizations$cubeDraw = draw;
    }
}
