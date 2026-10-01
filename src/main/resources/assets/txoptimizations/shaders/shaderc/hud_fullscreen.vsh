#version 330
#extension GL_ARB_separate_shader_objects : require

#include <minecraft:dynamictransforms.glsl>
#include <minecraft:projection.glsl>

layout(location = 0) in vec3 Position;
layout(location = 1) in vec2 UV0;

layout(location = 0) out vec2 texCoord0;

void main() {
    const vec2 vertices[3] = vec2[3] (
        vec2(-1,-1),
        vec2(3,-1),
        vec2(-1, 3)
    );
    gl_Position = vec4(vertices[gl_VertexIndex % 3], 0.0, 1.0);
    texCoord0 = 0.5 * gl_Position.xy + vec2(0.5);
}
