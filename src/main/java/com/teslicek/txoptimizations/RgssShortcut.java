package com.teslicek.txoptimizations;

import net.minecraft.resources.Identifier;

public final class RgssShortcut {

    private static final String FILE     = "shaders/include/texture_sampling.glsl";
    private static final String BLEND    = "    float blendFactor = smoothstep(transitionStart, transitionEnd, maxTexelSize);\n";
    private static final String NEAREST  = "    vec4 nearestColor = sampleNearest(source, uv, pixelSize, du, dv, texelScreenSize);\n\n    return mix(nearestColor, rgssColor, blendFactor);\n}";
    private static final String SHORTCUT = BLEND + "    if (blendFactor == 0.0) {\n        return sampleNearest(source, uv, pixelSize, du, dv, texelScreenSize);\n    }\n";
    private static final String LEVELS   = """
            float mipLevelLow = floor(mipLevelExact);
            float mipLevelHigh = mipLevelLow + 1.0;
            float mipBlend = fract(mipLevelExact);

        """;
    private static final String TWO_TAPS = """
            vec4 rgssColorLow = vec4(0.0);
            vec4 rgssColorHigh = vec4(0.0);
            for (int i = 0; i < 4; ++i) {
                vec2 sampleUV = uv + offsets[i] * pixelSize;
                rgssColorLow += textureLod(source, sampleUV, mipLevelLow);
                rgssColorHigh += textureLod(source, sampleUV, mipLevelHigh);
            }
            rgssColorLow *= 0.25;
            rgssColorHigh *= 0.25;

            vec4 rgssColor = mix(rgssColorLow, rgssColorHigh, mipBlend);
        """;
    private static final String ONE_TAP  = """
            vec4 rgssColor = vec4(0.0);
            for (int i = 0; i < 4; ++i) {
                vec2 sampleUV = uv + offsets[i] * pixelSize;
                rgssColor += textureLod(source, sampleUV, mipLevelExact);
            }
            rgssColor *= 0.25;
        """;

    private RgssShortcut() {
    }

    public static String patch(Identifier location, String source) {
        if (!location.getNamespace().equals(Identifier.DEFAULT_NAMESPACE) || !location.getPath().equals(FILE))
            return source;

        if (!once(source, BLEND) || !once(source, NEAREST) || !once(source, LEVELS) || !once(source, TWO_TAPS))
            return source;

        return source.replace(BLEND, SHORTCUT).replace(LEVELS, "").replace(TWO_TAPS, ONE_TAP);
    }

    private static boolean once(String source, String part) {
        int first = source.indexOf(part);

        return first >= 0 && first == source.lastIndexOf(part);
    }
}
