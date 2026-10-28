package com.teslicek.txoptimizations.mixin;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.renderpearl.api.pipeline.PrimitiveTopology;
import com.mojang.renderpearl.api.vertex.VertexFormat;
import com.teslicek.txoptimizations.RestartableBuilder;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(BufferBuilder.class)
public abstract class BufferBuilderRestartMixin implements RestartableBuilder {

    @Shadow
    private long vertexPointer;

    @Shadow
    private int vertices;

    @Shadow
    private int elementsToFill;

    @Shadow
    private boolean building;

    @Shadow
    @Final
    private VertexFormat format;

    @Shadow
    @Final
    private PrimitiveTopology primitiveTopology;

    @Override
    public boolean txoptimizations$builds(VertexFormat format, PrimitiveTopology topology) {
        return this.format == format && this.primitiveTopology == topology;
    }

    @Override
    public void txoptimizations$restart() {
        if (this.building)
            throw new IllegalStateException("A buffer builder that is still building cannot be restarted");

        this.vertexPointer  = -1L;
        this.vertices       = 0;
        this.elementsToFill = 0;
        this.building       = true;
    }
}
