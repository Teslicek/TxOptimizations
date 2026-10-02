#version 330
#extension GL_ARB_separate_shader_objects : require

#include <sodium:globals.glsl>

layout(push_constant) uniform PC {
    vec3 u_Origin;
};

const int CORNERS[36] = int[36](
    0, 2, 1, 1, 2, 3,
    4, 5, 6, 5, 7, 6,
    0, 1, 4, 1, 5, 4,
    2, 6, 3, 3, 6, 7,
    0, 4, 2, 2, 4, 6,
    1, 3, 5, 3, 7, 5
);

void main() {
    int corner = CORNERS[gl_VertexIndex];
    vec3 offset = vec3(float(corner & 1), float((corner >> 1) & 1), float((corner >> 2) & 1)) * 16.0;
    gl_Position = u_ProjectionMatrix * u_ModelViewMatrix * vec4(u_Origin + offset, 1.0);
}
