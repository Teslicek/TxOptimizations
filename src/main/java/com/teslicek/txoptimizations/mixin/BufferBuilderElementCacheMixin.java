package com.teslicek.txoptimizations.mixin;

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

    @Shadow
    @Final
    private static String[] elementNames;

    @Redirect(method = "<init>", at = @At(value = "INVOKE", target = "Lcom/mojang/renderpearl/api/vertex/VertexFormat;contains(Ljava/lang/String;)Z"))
    private boolean txoptimizations$cachedContains(VertexFormat format, String name) {
        return txoptimizations$element(format, name) != null;
    }

    @Redirect(method = "<init>", at = @At(value = "INVOKE", target = "Lcom/mojang/renderpearl/api/vertex/VertexFormat;getElement(Ljava/lang/String;)Lcom/mojang/renderpearl/api/vertex/VertexFormatElement;"))
    private VertexFormatElement txoptimizations$cachedElement(VertexFormat format, String name) {
        return txoptimizations$element(format, name);
    }

    @Unique
    private static VertexFormatElement txoptimizations$element(VertexFormat format, String name) {
        VertexFormatElementCache cache    = (VertexFormatElementCache) format;
        VertexFormatElement[]    elements = cache.txoptimizations$getBufferElements();

        if (elements == null) {
            elements = new VertexFormatElement[elementNames.length];

            for (int index = 0; index < elementNames.length; index ++)
                elements[index] = format.getElement(elementNames[index]);

            cache.txoptimizations$setBufferElements(elements);
        }

        for (int index = 0; index < elementNames.length; index ++) {
            if (elementNames[index] == name)
                return elements[index];
        }

        throw new IllegalStateException("BufferBuilder asked for vertex element " + name + " that is not in its element name table");
    }
}
