package com.teslicek.txoptimizations.mixin;

import net.minecraft.client.renderer.rendertype.LayeringTransform;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.TextureTransform;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(RenderSetup.class)
public interface RenderSetupTransformAccessor {

    @Accessor("textureTransform")
    TextureTransform txoptimizations$textureTransform();

    @Accessor("layeringTransform")
    LayeringTransform txoptimizations$layeringTransform();
}
