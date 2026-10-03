package com.teslicek.txoptimizations.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.renderpearl.api.pipeline.ShaderType;
import net.minecraft.client.renderer.ShaderManager;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ShaderManager.class)
public abstract class ShaderManagerTerrainMatrixMixin {

    @Unique
    private static final String TERRAIN_SHADER = "shaders/blocks/block_layer_opaque.vsh";

    @Unique
    private static final String SEPARATE_MATRICES = "u_ProjectionMatrix * u_ModelViewMatrix * vec4(position, 1.0)";

    @Unique
    private static final String COMBINED_MATRIX = "u_ProjectionMatrix * vec4(position, 1.0)";

    @ModifyExpressionValue(method = "loadShader", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/packs/resources/Resource;readAllAsString()Ljava/lang/String;"))
    private static String txoptimizations$useCombinedTerrainMatrix(String contents, @Local(argsOnly = true) Identifier location, @Local(argsOnly = true) ShaderType type) {
        if (type != ShaderType.VERTEX || !location.getNamespace().equals("sodium") || !location.getPath().equals(TERRAIN_SHADER))
            return contents;

        return contents.replace(SEPARATE_MATRICES, COMBINED_MATRIX);
    }
}
