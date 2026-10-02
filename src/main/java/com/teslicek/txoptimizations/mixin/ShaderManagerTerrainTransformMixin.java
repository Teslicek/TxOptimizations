package com.teslicek.txoptimizations.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.renderpearl.api.pipeline.ShaderType;
import net.minecraft.client.renderer.ShaderManager;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ShaderManager.class)
public abstract class ShaderManagerTerrainTransformMixin {

    @Unique
    private static final Identifier TERRAIN_VERTEX_SHADER = Identifier.fromNamespaceAndPath("sodium", "shaders/blocks/block_layer_opaque.vsh");

    @Unique
    private static final String MATRIX_PRODUCT = "gl_Position = u_ProjectionMatrix * u_ModelViewMatrix * vec4(position, 1.0);";

    @Unique
    private static final String VECTOR_PRODUCT = "gl_Position = u_ProjectionMatrix * (u_ModelViewMatrix * vec4(position, 1.0));";

    @WrapOperation(method = "loadShader", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/packs/resources/Resource;readAllAsString()Ljava/lang/String;"))
    private static String txoptimizations$transformVerticesDirectly(Resource resource, Operation<String> original, @Local(argsOnly = true) Identifier location, @Local(argsOnly = true) ShaderType type) {
        String contents = original.call(resource);

        if (type != ShaderType.VERTEX || !location.equals(TERRAIN_VERTEX_SHADER))
            return contents;

        if (!contents.contains(MATRIX_PRODUCT))
            throw new IllegalStateException("Sodium terrain vertex shader " + location + " no longer contains the expected transform: " + MATRIX_PRODUCT);

        return contents.replace(MATRIX_PRODUCT, VECTOR_PRODUCT);
    }
}
