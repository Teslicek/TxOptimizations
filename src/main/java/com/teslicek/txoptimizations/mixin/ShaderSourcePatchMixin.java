package com.teslicek.txoptimizations.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.mojang.renderpearl.api.pipeline.ShaderType;
import java.util.regex.Pattern;
import net.minecraft.client.renderer.ShaderManager;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ShaderManager.Configs.class)
public abstract class ShaderSourcePatchMixin {

    @Unique
    private static final Identifier TERRAIN_SHADER = Identifier.fromNamespaceAndPath("sodium", "blocks/block_layer_opaque");

    @Unique
    private static final String BLEND_FACTOR = "    float blendFactor = smoothstep(transitionStart, transitionEnd, maxTexelSize);\n";

    @Unique
    private static final String NEAREST_ONLY = "    if (blendFactor == 0.0) {\n        return sampleNearest(source, uv, pixelSize, du, dv, texelScreenSize);\n    }\n";

    @Unique
    private static final String RGSS_AVERAGE = "    rgssColor *= 0.25;\n";

    @Unique
    private static final String RGSS_ONLY = "    if (blendFactor == 1.0) {\n        return rgssColor;\n    }\n";

    @Unique
    private static final String FADE_START = "    int chunkId = int(_draw_id);\n";

    @Unique
    private static final String FADE_END = "    fadeFactor = (chunkFade < 0) ? 1.0 : fade;\n";

    @Unique
    private static final String TERRAIN_POSITION = "    gl_Position = u_ProjectionMatrix * u_ModelViewMatrix * vec4(position, 1.0);\n";

    @Unique
    private static final String TERRAIN_POSITION_SPLIT = "    gl_Position = u_ProjectionMatrix * (u_ModelViewMatrix * vec4(position, 1.0));\n";

    @Unique
    private static final Pattern VANILLA_POSITION = Pattern.compile("gl_Position = ProjMat \\* ModelViewMat \\* vec4\\((\\w+), 1\\.0\\);");

    @ModifyReturnValue(method = "getShader", at = @At("RETURN"))
    private String txoptimizations$patchShader(String source, Identifier id, ShaderType type) {
        if (source == null)
            return source;

        if (id.equals(TERRAIN_SHADER))
            return type == ShaderType.FRAGMENT ? txoptimizations$patchTerrainFragment(source) : txoptimizations$patchTerrainVertex(source);

        if (type == ShaderType.VERTEX && id.getNamespace().equals(Identifier.DEFAULT_NAMESPACE))
            return VANILLA_POSITION.matcher(source).replaceAll("gl_Position = ProjMat * (ModelViewMat * vec4($1, 1.0));");

        return source;
    }

    @Unique
    private static String txoptimizations$patchTerrainFragment(String source) {
        String nearest = txoptimizations$insertAfter(source, BLEND_FACTOR, NEAREST_ONLY);

        return txoptimizations$insertAfter(nearest, RGSS_AVERAGE, RGSS_ONLY);
    }

    @Unique
    private static String txoptimizations$patchTerrainVertex(String source) {
        String fadeStart = txoptimizations$insertBefore(source, FADE_START, "    if (isinf(u_FadePeriodInv)) {\n        fadeFactor = 1.0;\n    } else {\n");
        String fade      = txoptimizations$insertAfter(fadeStart, FADE_END, "    }\n");

        return txoptimizations$replace(fade, TERRAIN_POSITION, TERRAIN_POSITION_SPLIT);
    }

    @Unique
    private static String txoptimizations$insertBefore(String source, String anchor, String text) {
        return txoptimizations$replace(source, anchor, text + anchor);
    }

    @Unique
    private static String txoptimizations$insertAfter(String source, String anchor, String text) {
        return txoptimizations$replace(source, anchor, anchor + text);
    }

    @Unique
    private static String txoptimizations$replace(String source, String anchor, String replacement) {
        int at = source.indexOf(anchor);

        if (at < 0 || source.indexOf(anchor, at + 1) >= 0)
            throw new IllegalStateException("Sodium terrain shader no longer has exactly one line " + anchor.strip());

        return source.substring(0, at) + replacement + source.substring(at + anchor.length());
    }
}
