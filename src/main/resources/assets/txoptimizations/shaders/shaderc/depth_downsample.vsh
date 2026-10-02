#version 330
#extension GL_ARB_separate_shader_objects : require

void main() {
    const vec2 vertices[3] = vec2[3](
        vec2(-1.0, -1.0),
        vec2(3.0, -1.0),
        vec2(-1.0, 3.0)
    );
    gl_Position = vec4(vertices[gl_VertexIndex], 0.0, 1.0);
}
