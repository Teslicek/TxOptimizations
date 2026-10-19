package com.teslicek.txoptimizations;

import net.minecraft.resources.Identifier;

public final class RgssShortcut {

    private static final String FILE     = "shaders/include/texture_sampling.glsl";
    private static final String BLEND    = "    float blendFactor = smoothstep(transitionStart, transitionEnd, maxTexelSize);\n";
    private static final String NEAREST  = "    vec4 nearestColor = sampleNearest(source, uv, pixelSize, du, dv, texelScreenSize);\n\n    return mix(nearestColor, rgssColor, blendFactor);\n}";
    private static final String SHORTCUT = BLEND + "    if (blendFactor == 0.0) {\n        return sampleNearest(source, uv, pixelSize, du, dv, texelScreenSize);\n    }\n";

    private RgssShortcut() {
    }

    public static String patch(Identifier location, String source) {
        if (!location.getNamespace().equals(Identifier.DEFAULT_NAMESPACE) || !location.getPath().equals(FILE))
            return source;

        if (source.indexOf(BLEND) != source.lastIndexOf(BLEND) || !source.contains(BLEND) || !source.contains(NEAREST))
            return source;

        return source.replace(BLEND, SHORTCUT);
    }
}
