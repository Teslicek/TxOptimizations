package com.teslicek.txoptimizations.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.mojang.renderpearl.api.pipeline.ShaderType;
import net.minecraft.client.renderer.ShaderManager;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ShaderManager.Configs.class)
public abstract class SodiumTerrainShaderMixin {

    @Unique
    private static final Identifier TERRAIN_SHADER = Identifier.fromNamespaceAndPath("sodium", "blocks/block_layer_opaque");

    @Unique
    private static final String BLEND_FACTOR = "    float blendFactor = smoothstep(transitionStart, transitionEnd, maxTexelSize);\n";

    @Unique
    private static final String NEAREST_ONLY = "    if (blendFactor == 0.0) {\n        return sampleNearest(source, uv, pixelSize, du, dv, texelScreenSize);\n    }\n";

    @ModifyReturnValue(method = "getShader", at = @At("RETURN"))
    private String txoptimizations$skipUnusedSupersampling(String source, Identifier id, ShaderType type) {
        if (source == null || type != ShaderType.FRAGMENT || !id.equals(TERRAIN_SHADER))
            return source;

        int at = source.indexOf(BLEND_FACTOR);

        if (at < 0 || source.indexOf(BLEND_FACTOR, at + 1) >= 0)
            throw new IllegalStateException("Sodium terrain fragment shader no longer has exactly one RGSS blend factor line");

        int end = at + BLEND_FACTOR.length();

        return source.substring(0, end) + NEAREST_ONLY + source.substring(end);
    }
}
