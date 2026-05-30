#version 150

in vec3 Position;

uniform mat4 InvProjMat;
uniform mat4 InvViewMat;

out vec3 vRayDir;

void main() {
    gl_Position = vec4(Position.xy, 1.0, 1.0);

    vec4 rayView = InvProjMat * vec4(Position.xy, -1.0, 1.0);
    rayView = vec4(rayView.xy, -1.0, 0.0);
    vRayDir = normalize((InvViewMat * rayView).xyz);
}
