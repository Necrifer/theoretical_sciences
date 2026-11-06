#version 150
// 1.2.0: written for Theoretical Sciences (MIT). A dark starfield: stars from a hashed 3D cell grid on the direction
// (three sizes), twinkling, over a faint nebula of the tint colour.

uniform vec4 ColorModulator;
uniform float FxTime;

in vec3 dir;
in vec4 vertexColor;
out vec4 fragColor;

float hash(vec3 p) { return fract(sin(dot(p, vec3(127.1, 311.7, 74.7))) * 43758.5453); }
vec3 hash3(vec3 p) { return vec3(hash(p), hash(p + 19.19), hash(p + 47.53)); }
float noise(vec3 p) {
    vec3 i = floor(p), f = fract(p);
    vec3 u = f * f * (3.0 - 2.0 * f);
    float a = mix(mix(hash(i), hash(i + vec3(1, 0, 0)), u.x), mix(hash(i + vec3(0, 1, 0)), hash(i + vec3(1, 1, 0)), u.x), u.y);
    float b = mix(mix(hash(i + vec3(0, 0, 1)), hash(i + vec3(1, 0, 1)), u.x), mix(hash(i + vec3(0, 1, 1)), hash(i + vec3(1, 1, 1)), u.x), u.y);
    return mix(a, b, u.z);
}

float stars(vec3 d, float scale, float size) {
    vec3 p = d * scale;
    vec3 c = floor(p);
    vec3 h = hash3(c);
    if (h.x > 0.35) return 0.0;
    vec3 s = c + 0.2 + 0.6 * h;
    float r = length(p - s);
    float tw = 0.65 + 0.35 * sin(FxTime * (1.0 + 3.0 * h.y) + h.z * 40.0);
    return smoothstep(size, 0.0, r) * tw * (0.4 + h.y);
}

void main() {
    vec3 d = normalize(dir);
    float s = stars(d, 70.0, 0.09) + stars(d, 150.0, 0.07) * 0.7 + stars(d, 300.0, 0.06) * 0.5;
    float neb = noise(d * 3.0) * 0.6 + noise(d * 7.0 + 3.1) * 0.4;
    // ColorModulator: rgb is the nebula's tint, alpha how far the shell has opened (0 to 1)
    vec3 col = ColorModulator.rgb * pow(neb, 3.0) * 0.18 + vec3(0.9, 0.95, 1.0) * s;
    fragColor = vec4((col + vec3(0.004, 0.004, 0.01)) * ColorModulator.a, 1.0);
}
