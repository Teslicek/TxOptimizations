package com.teslicek.txoptimizations.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.caffeinemc.mods.sodium.client.render.chunk.ChunkRenderMatrices;
import net.caffeinemc.mods.sodium.client.render.chunk.UniformBufferManager;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = UniformBufferManager.class, remap = false)
public abstract class TerrainCombinedMatrixMixin {

    @Unique
    private static final Matrix4fc IDENTITY = new Matrix4f();

    @WrapOperation(method = "update", at = @At(value = "INVOKE", target = "Lnet/caffeinemc/mods/sodium/client/render/chunk/ChunkRenderMatrices;projection()Lorg/joml/Matrix4fc;"))
    private Matrix4fc txoptimizations$combineMatrices(ChunkRenderMatrices matrices, Operation<Matrix4fc> original) {
        return original.call(matrices).mul(matrices.modelView(), new Matrix4f());
    }

    @WrapOperation(method = "update", at = @At(value = "INVOKE", target = "Lnet/caffeinemc/mods/sodium/client/render/chunk/ChunkRenderMatrices;modelView()Lorg/joml/Matrix4fc;"))
    private Matrix4fc txoptimizations$identityModelView(ChunkRenderMatrices matrices, Operation<Matrix4fc> original) {
        return IDENTITY;
    }
}
