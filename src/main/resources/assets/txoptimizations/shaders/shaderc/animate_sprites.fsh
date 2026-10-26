#version 330
#extension GL_ARB_separate_shader_objects : require

uniform sampler2D Frames;

layout(location = 0) in float fAnimationProgress;
layout(location = 1) flat in ivec4 frames;
layout(location = 2) flat in ivec4 content;
layout(location = 3) flat in int mipLevel;

layout(location = 0) out vec4 fragColor;

void main() {
    ivec2 texel = clamp(ivec2(gl_FragCoord.xy) - content.xy, ivec2(0), content.zw - 1);
    vec4 currentColor = texelFetch(Frames, frames.xy + texel, mipLevel);
    vec4 nextColor = texelFetch(Frames, frames.zw + texel, mipLevel);
    fragColor = mix(currentColor, nextColor, fAnimationProgress);
}
