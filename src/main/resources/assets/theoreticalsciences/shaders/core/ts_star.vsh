#version 150
// 1.2.0: a star's surface. The mesh is a unit sphere, so Position is also the direction from the star's centre:
// the noise is read in 3D from it, with no seam and no texture.
in vec3 Position;
in vec4 Color;

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;

out vec3 dir;
out vec4 vertexColor;
out vec3 viewPos;
out vec3 viewNormal;

void main() {
    vec4 vp = ModelViewMat * vec4(Position, 1.0);
    gl_Position = ProjMat * vp;
    dir = Position;
    vertexColor = Color;
    viewPos = vp.xyz;
    viewNormal = mat3(ModelViewMat) * Position;
}
