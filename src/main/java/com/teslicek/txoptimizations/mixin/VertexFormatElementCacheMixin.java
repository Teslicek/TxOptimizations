package com.teslicek.txoptimizations.mixin;

import com.mojang.renderpearl.api.vertex.VertexFormat;
import com.mojang.renderpearl.api.vertex.VertexFormatElement;
import com.teslicek.txoptimizations.VertexFormatElementCache;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(VertexFormat.class)
public abstract class VertexFormatElementCacheMixin implements VertexFormatElementCache {

    @Unique
    private volatile VertexFormatElement[] txoptimizations$bufferElements;

    @Override
    public VertexFormatElement[] txoptimizations$getBufferElements() {
        return this.txoptimizations$bufferElements;
    }

    @Override
    public void txoptimizations$setBufferElements(VertexFormatElement[] elements) {
        this.txoptimizations$bufferElements = elements;
    }
}
