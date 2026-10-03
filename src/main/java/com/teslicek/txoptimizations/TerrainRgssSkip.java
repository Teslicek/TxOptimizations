package com.teslicek.txoptimizations;

import net.minecraft.resources.Identifier;

public final class TerrainRgssSkip {

    private static final String SHADER = "shaders/blocks/block_layer_opaque.fsh";
    private static final String BLEND  = "    float blendFactor = smoothstep(transitionStart, transitionEnd, maxTexelSize);\n";
    private static final String SKIP   = "    if (blendFactor == 0.0) {\n        return sampleNearest(source, uv, pixelSize, du, dv, texelScreenSize);\n    }\n";

    private TerrainRgssSkip() {
    }

    public static String patch(Identifier location, String source) {
        if (!location.getNamespace().equals("sodium") || !location.getPath().equals(SHADER))
            return source;

        String normalized = source.replace("\r\n", "\n");

        if (!normalized.contains(BLEND))
            return source;

        return normalized.replace(BLEND, BLEND + SKIP);
    }
}
