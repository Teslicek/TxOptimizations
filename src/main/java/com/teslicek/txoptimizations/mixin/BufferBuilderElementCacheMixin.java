package com.teslicek.txoptimizations.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.renderpearl.api.vertex.VertexFormat;
import com.mojang.renderpearl.api.vertex.VertexFormatElement;
import com.teslicek.txoptimizations.VertexFormatElementCache;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(BufferBuilder.class)
public abstract class BufferBuilderElementCacheMixin {

    @Shadow
    @Final
    private static String[] elementNames;

    @WrapOperation(method = "<init>", at = @At(value = "INVOKE", target = "Lcom/mojang/renderpearl/api/vertex/VertexFormat;getElement(Ljava/lang/String;)Lcom/mojang/renderpearl/api/vertex/VertexFormatElement;"))
    private VertexFormatElement txoptimizations$cachedElement(VertexFormat format, String name, Operation<VertexFormatElement> original) {
        VertexFormatElementCache cache    = (VertexFormatElementCache) format;
        VertexFormatElement[]    elements = cache.txoptimizations$getBufferElements();

        if (elements == null) {
            elements = new VertexFormatElement[elementNames.length];

            for (int index = 0; index < elementNames.length; index ++)
                elements[index] = original.call(format, elementNames[index]);

            cache.txoptimizations$setBufferElements(elements);
        }

        for (int index = 0; index < elementNames.length; index ++) {
            if (elementNames[index] == name)
                return elements[index];
        }

        throw new IllegalStateException("BufferBuilder asked for vertex element " + name + " that is not in its element name table");
    }
}
