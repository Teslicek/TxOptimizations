package com.teslicek.txoptimizations.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.teslicek.txoptimizations.ParallelFeatures;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(TextureManager.class)
public abstract class TextureManagerParallelMixin {

    @WrapMethod(method = "getTexture(Lnet/minecraft/resources/Identifier;)Lnet/minecraft/client/renderer/texture/AbstractTexture;")
    private AbstractTexture txoptimizations$locked(Identifier location, Operation<AbstractTexture> original) {
        if (!ParallelFeatures.lock())
            return original.call(location);

        try {
            return original.call(location);
        } finally {
            ParallelFeatures.unlock();
        }
    }
}
