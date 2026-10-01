package com.teslicek.txoptimizations.mixin;

import com.mojang.renderpearl.api.vertex.VertexFormat;
import com.mojang.renderpearl.api.vertex.VertexFormatElement;
import com.teslicek.txoptimizations.VertexFormatElementCache;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(VertexFormat.class)
public abstract class VertexFormatElementCacheMixin implements VertexFormatElementCache {

    @Unique
    private volatile VertexFormatElement[] txoptimizations$bufferElements;

    @Unique
    private VertexFormatElement txoptimizations$positionElement;

    @Shadow
    public abstract VertexFormatElement getElement(String name);

    @Inject(method = "<init>", at = @At("RETURN"))
    private void txoptimizations$resolvePositionElement(CallbackInfo ci) {
        this.txoptimizations$positionElement = this.getElement("Position");
    }

    @Override
    public VertexFormatElement[] txoptimizations$getBufferElements() {
        return this.txoptimizations$bufferElements;
    }

    @Override
    public void txoptimizations$setBufferElements(VertexFormatElement[] elements) {
        this.txoptimizations$bufferElements = elements;
    }

    @Override
    public VertexFormatElement txoptimizations$getPositionElement() {
        return this.txoptimizations$positionElement;
    }
}
