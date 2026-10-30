#version 150

// Procedural accretion disk. UV0.x = angle (0..1), UV0.y = radius (0 inner .. 1 outer).
// Inner orbits move faster (Keplerian), hotter (whiter) and brighter. Vertex colour carries Doppler beaming.

uniform float GameTime;
uniform float Palette;

in vec2 texCoord0;
in vec4 vertexColor;
out vec4 fragColor;

float hash(vec2 p) { return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453); }
float noise(vec2 p) {
    vec2 i = floor(p), f = fract(p);
    vec2 u = f * f * (3.0 - 2.0 * f);
    return mix(mix(hash(i), hash(i + vec2(1, 0)), u.x), mix(hash(i + vec2(0, 1)), hash(i + vec2(1, 1)), u.x), u.y);
}

void main() {
    float t = GameTime * 1200.0;
    float rad = texCoord0.y;
    float ang = texCoord0.x;
    float omega = 0.35 / pow(0.25 + rad, 1.5);
    float a = ang * 24.0 + t * omega;
    float n = noise(vec2(a, rad * 14.0)) * 0.6 + noise(vec2(a * 2.3, rad * 31.0 - t * 0.2)) * 0.4;
    float streak = 0.55 + 0.75 * n;
    float bright = pow(1.0 - rad, 1.3) * streak;
    vec3 hot, warm, cool;
    if (Palette > 2.5) {
        // primordial law ocean: a churning void that bubbles bright 'laws' into existence and lets them pop
        float churn = noise(vec2(ang * 18.0 + t * 0.05, rad * 9.0 - t * 0.12)) * 0.6 + noise(vec2(ang * 41.0 - t * 0.09, rad * 23.0)) * 0.4;
        vec2 bc = vec2(ang * 60.0, rad * 12.0 - t * 0.35);
        vec2 bi = floor(bc);
        float life = fract(hash(bi) + t * 0.2);
        float bub = step(0.93, hash(bi + 7.0)) * smoothstep(0.5, 0.0, length(fract(bc) - 0.5) - 0.18 * life) * (1.0 - life);
        vec3 deep = vec3(0.02, 0.0, 0.06), mid = vec3(0.25, 0.1, 0.55);
        vec3 lawc = mix(vec3(0.6, 1.0, 0.95), vec3(1.0, 0.85, 0.4), hash(bi + 3.0));
        vec3 oc = mix(deep, mid, churn) + lawc * bub * 2.0;
        float oa = clamp((0.55 + 0.45 * churn) * smoothstep(1.0, 0.85, rad) * vertexColor.a, 0.0, 1.0);
        fragColor = vec4(oc, oa);
        return;
    }
    if (Palette > 1.5) {
        // spiral galaxy inside a pocket universe: logarithmic arms, golden core, blue outskirts
        float arms = pow(0.5 + 0.5 * sin(ang * 6.2831853 * 2.0 - log(rad + 0.03) * 5.0 - t * 0.15), 3.0);
        float stars = step(0.985, hash(floor(vec2(ang * 400.0, rad * 60.0))));
        float b2 = (exp(-rad * 6.0) * 1.6 + arms * (1.0 - rad) * 0.9) * (0.7 + 0.3 * n) + stars * 0.8;
        vec3 gc = mix(vec3(1.0, 0.85, 0.55), vec3(0.45, 0.6, 1.0), smoothstep(0.05, 0.6, rad));
        float ga = clamp(b2 * smoothstep(1.0, 0.75, rad) * vertexColor.a, 0.0, 1.0);
        if (ga < 0.003) discard;
        fragColor = vec4(gc * (0.5 + b2), ga);
        return;
    }
    if (Palette < 0.5) { hot = vec3(1.0, 0.97, 0.9); warm = vec3(1.0, 0.6, 0.18); cool = vec3(0.55, 0.12, 0.03); }
    else { hot = vec3(0.92, 0.95, 1.0); warm = vec3(0.45, 0.6, 1.0); cool = vec3(0.25, 0.05, 0.5); }
    vec3 c = mix(hot, warm, smoothstep(0.0, 0.45, rad));
    c = mix(c, cool, smoothstep(0.45, 1.0, rad));
    float edge = smoothstep(0.0, 0.06, rad) * smoothstep(1.0, 0.7, rad);
    float a2 = clamp(edge * bright * vertexColor.a * 1.6, 0.0, 1.0);
    if (a2 < 0.003) discard;
    fragColor = vec4(c * vertexColor.rgb * (0.6 + bright), a2);
}
