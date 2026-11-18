#version 330
#extension GL_ARB_separate_shader_objects : require

uniform sampler2D Hud;

layout(location = 0) out vec4 fragColor;

const int TILE = 32;

void main() {
    ivec2 start = ivec2(gl_FragCoord.xy) * TILE;
    ivec2 end = min(start + TILE, textureSize(Hud, 0));

    for (int y = start.y; y < end.y; y++) {
        for (int x = start.x; x < end.x; x++) {
            if (texelFetch(Hud, ivec2(x, y), 0).a != 0.0) {
                fragColor = vec4(1.0);
                return;
            }
        }
    }

    fragColor = vec4(0.0);
}