#version 150
// 1.2.0: written for Theoretical Sciences (MIT). Granulated plasma: cells of hot rising gas with darker lanes between,
// slowly churning; brighter toward the limb on the outer (additive) layers, darker toward the limb on the core.

uniform vec4 ColorModulator;
uniform float FxTime;
uniform float Layer;

in vec3 dir;
in vec4 vertexColor;
in vec3 viewPos;
in vec3 viewNormal;
out vec4 fragColor;

float hash(vec3 p) { return fract(sin(dot(p, vec3(127.1, 311.7, 74.7))) * 43758.5453); }
float noise(vec3 p) {
    vec3 i = floor(p), f = fract(p);
    vec3 u = f * f * (3.0 - 2.0 * f);
    float a = mix(mix(hash(i), hash(i + vec3(1, 0, 0)), u.x), mix(hash(i + vec3(0, 1, 0)), hash(i + vec3(1, 1, 0)), u.x), u.y);
    float b = mix(mix(hash(i + vec3(0, 0, 1)), hash(i + vec3(1, 0, 1)), u.x), mix(hash(i + vec3(0, 1, 1)), hash(i + vec3(1, 1, 1)), u.x), u.y);
    return mix(a, b, u.z);
}
float fbm(vec3 p) {
    float s = 0.0, a = 0.5;
    for (int k = 0; k < 4; k++) { s += a * noise(p); p = p * 2.03 + vec3(1.7, 9.2, 4.1); a *= 0.5; }
    return s;
}

void main() {
    vec3 d = normalize(dir);
    float t = FxTime;
    float n = fbm(d * (4.0 + Layer * 2.0) + vec3(0.0, t * 0.05, t * 0.03));
    float g = fbm(d * 14.0 - vec3(t * 0.08, 0.0, t * 0.06));
    float cells = smoothstep(0.25, 0.75, n) * 0.7 + g * 0.5;
    float mu = clamp(abs(dot(normalize(viewNormal), normalize(-viewPos))), 0.0, 1.0);
    vec3 base = vertexColor.rgb;
    vec3 col;
    float a = vertexColor.a;
    if (Layer < 0.5) {
        // the photosphere: limb darkening, bright granules, a white-hot centre
        float limb = 0.35 + 0.65 * pow(mu, 0.6);
        col = mix(base * 0.55, mix(base, vec3(1.0, 0.97, 0.85), 0.45), cells) * limb * 1.25;
    } else {
        // the corona layers: faint, brightest at the rim, streaked
        float rim = pow(1.0 - mu, 1.6);
        col = base * (0.35 + 0.9 * cells) * (0.4 + 1.6 * rim);
        a *= 0.5 + 0.8 * rim;
    }
    fragColor = vec4(col, a) * ColorModulator;
}
