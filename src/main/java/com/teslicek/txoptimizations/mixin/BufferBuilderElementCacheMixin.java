package com.teslicek.txoptimizations.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.renderpearl.api.vertex.VertexFormat;
import com.mojang.renderpearl.api.vertex.VertexFormatElement;
import com.teslicek.txoptimizations.VertexFormatElementCache;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(BufferBuilder.class)
public abstract class BufferBuilderElementCacheMixin {

    @Unique
    private static final int POSITION = 0;

    @Shadow
    @Final
    private static String[] elementNames;

    @Redirect(method = "<init>", at = @At(value = "INVOKE", target = "Lcom/mojang/renderpearl/api/vertex/VertexFormat;contains(Ljava/lang/String;)Z"))
    private boolean txoptimizations$cachedContains(VertexFormat format, String name) {
        return txoptimizations$element(format, name, POSITION) != null;
    }

    @Redirect(method = "<init>", at = @At(value = "INVOKE", target = "Lcom/mojang/renderpearl/api/vertex/VertexFormat;getElement(Ljava/lang/String;)Lcom/mojang/renderpearl/api/vertex/VertexFormatElement;"))
    private VertexFormatElement txoptimizations$cachedElement(VertexFormat format, String name, @Local(ordinal = 1) int index) {
        return txoptimizations$element(format, name, index);
    }

    @Unique
    private static VertexFormatElement txoptimizations$element(VertexFormat format, String name, int index) {
        if (elementNames[index] != name)
            throw new IllegalStateException("BufferBuilder asked for vertex element " + name + " at index " + index + ", where its name table has " + elementNames[index]);

        VertexFormatElementCache cache    = (VertexFormatElementCache) format;
        VertexFormatElement[]    elements = cache.txoptimizations$getBufferElements();

        if (elements == null) {
            elements = new VertexFormatElement[elementNames.length];

            for (int element = 0; element < elementNames.length; element ++)
                elements[element] = format.getElement(elementNames[element]);

            cache.txoptimizations$setBufferElements(elements);
        }

        return elements[index];
    }
}
