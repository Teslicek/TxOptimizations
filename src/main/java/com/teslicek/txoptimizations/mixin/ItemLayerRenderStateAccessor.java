package com.teslicek.txoptimizations.mixin;

import java.util.function.Supplier;
import net.minecraft.client.resources.model.cuboid.ItemTransform;
import org.joml.Matrix4f;
import org.joml.Vector3fc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(targets = "net.minecraft.client.renderer.item.ItemStackRenderState$LayerRenderState")
public interface ItemLayerRenderStateAccessor {

    @Accessor("extents")
    Supplier<Vector3fc[]> txoptimizations$extents();

    @Accessor("itemTransform")
    ItemTransform txoptimizations$itemTransform();

    @Accessor("localTransform")
    Matrix4f txoptimizations$localTransform();
}
