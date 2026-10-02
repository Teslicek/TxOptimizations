#version 330
#extension GL_ARB_separate_shader_objects : require

uniform sampler2D DepthSampler;

layout(push_constant) uniform PC {
    ivec4 u_Sizes;
};

layout(location = 0) out vec4 FragColor;

void main() {
    ivec2 target = ivec2(gl_FragCoord.xy);
    ivec2 start = target * u_Sizes.xy / u_Sizes.zw;
    ivec2 end = (target + 1) * u_Sizes.xy / u_Sizes.zw;
    float farthest = 1.0;

    for (int y = start.y; y < end.y; y++) {
        for (int x = start.x; x < end.x; x++) {
            farthest = min(farthest, texelFetch(DepthSampler, ivec2(x, y), 0).r);
        }
    }

    FragColor = vec4(farthest, 0.0, 0.0, 0.0);
}
