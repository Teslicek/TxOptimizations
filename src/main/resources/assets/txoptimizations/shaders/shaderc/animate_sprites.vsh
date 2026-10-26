#version 330
#extension GL_ARB_separate_shader_objects : require

layout(std140) uniform AnimatedSprites {
    ivec4 AtlasSize;
    ivec4 Sprites[768];
};

layout(location = 0) out float fAnimationProgress;
layout(location = 1) flat out ivec4 frames;
layout(location = 2) flat out ivec4 content;
layout(location = 3) flat out int mipLevel;

const vec2[] positions = vec2[](
    vec2(0, 0),
    vec2(1, 0),
    vec2(0, 1),
    vec2(0, 1),
    vec2(1, 0),
    vec2(1, 1)
);

void main() {
    int index = gl_VertexIndex & 7;
    int level = gl_VertexIndex >> 3;
    ivec4 slot = Sprites[gl_InstanceIndex * 3];
    ivec4 sprite = Sprites[gl_InstanceIndex * 3 + 1];
    vec2 position = vec2(slot.xy) + positions[index] * vec2(slot.zw);
    gl_Position = vec4(position / vec2(AtlasSize.xy) * 2.0 - 1.0, 0.0, 1.0);
    fAnimationProgress = sprite.w / 1000.0;
    frames = Sprites[gl_InstanceIndex * 3 + 2] >> level;
    content = ivec4(slot.xy + sprite.x, sprite.yz) >> level;
    mipLevel = level;
}
