#version 150
// 1.2.0: the inside of a sphere of space; Position (unit sphere) is the direction looked along.
in vec3 Position;
in vec4 Color;

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;

out vec3 dir;
out vec4 vertexColor;

void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
    dir = Position;
    vertexColor = Color;
}
