#version 150

// Theoretical Sciences - space distortion inside higher-dimensional structures (round 14).
// The whole frame is resampled so the view takes on the shape of the object you stand in.
//  1 TESSERACT     nested cubes: the view recurses inward through square frames, each one turned a little,
//                  and slowly flows outward - the inner cube becoming the outer one
//  2 FRACTAL       a Droste spiral: the scene contains itself, shrinking forever along a log spiral
//  3 KALEIDOSCOPE  mirrored through the five-fold symmetry of a Platonic solid
//  4 INFINITE      an endless corridor of rooms (Hilbert's Hotel, the ordinal spire)
//  5 TWIST         space wound round itself, as a Calabi-Yau fibre or a Hopf ring
//  6 LENS          an Einstein ring: light bent round a horizon, blue-shifted coming, red-shifted going
//  7 HYPERREAL     the waves of a reality whose laws flow
//  8 GLITCH        the anomaly engine tearing the picture into displaced slices
//  9 TRANSCENDENT  everything at once
// The middle of the screen is distorted least so the crosshair stays usable.

uniform sampler2D Sampler0;
uniform vec2 InSize;
uniform vec2 ScreenSize;
uniform float Mode;
uniform float Intensity;
uniform float Clock;

in vec2 texCoord0;
out vec4 fragColor;

const float PI = 3.14159265;
const float TAU = 6.2831853;

float aspect() { return ScreenSize.x / max(ScreenSize.y, 1.0); }

// screen-centred coordinates (height = 1) <-> texture coordinates
vec2 toP(vec2 uv) { return (uv - 0.5) * vec2(aspect(), 1.0); }
vec2 toUV(vec2 p) { return clamp(p / vec2(aspect(), 1.0) + 0.5, vec2(0.001), vec2(0.999)); }
vec3 at(vec2 p) { return texture(Sampler0, toUV(p)).rgb; }

mat2 rot(float a) { float c = cos(a), s = sin(a); return mat2(c, -s, s, c); }

float hash(vec2 v) { return fract(sin(dot(v, vec2(127.1, 311.7))) * 43758.5453); }

vec3 hue(vec3 c, float a) {
    const vec3 k = vec3(0.57735);
    float ca = cos(a);
    return c * ca + cross(k, c) * sin(a) + k * dot(k, c) * (1.0 - ca);
}

// 1 - nested, rotated square frames flowing outward
vec3 tesseract(vec2 p, float t, out float edge) {
    float m = max(abs(p.x), abs(p.y));
    m = max(m, 1e-4);
    float k = log2(m / 0.22) - t * 0.18;
    float lvl = floor(k);
    float f = fract(k);
    float R = 0.22 * exp2(f);
    vec2 q = p * (R / m);
    // each nested cube is turned a little further, as if seen through the fourth dimension
    q = rot(lvl * 0.35 + sin(t * 0.4) * 0.08) * q;
    edge = exp(-(f / 0.035) * (f / 0.035)) + exp(-((1.0 - f) / 0.035) * ((1.0 - f) / 0.035));
    vec3 c = at(q);
    return c * mix(1.0, 0.82, clamp(-lvl * 0.25, 0.0, 1.0)) + vec3(0.35, 0.8, 1.0) * edge * 0.5;
}

// 2 - a log-spiral Droste: the view contains itself
vec3 fractal(vec2 p, float t) {
    float r = max(length(p), 1e-4);
    float a = atan(p.y, p.x + 1e-7);
    float k = (log2(r / 0.2) + a / TAU) - t * 0.15;
    float lvl = floor(k);
    float rr = 0.2 * exp2(fract(k));
    vec2 q = vec2(cos(a), sin(a)) * rr;
    q = rot(t * 0.05) * q;
    vec3 c = at(q);
    return hue(c, lvl * 0.35);
}

// 3 - five mirrors meeting at the centre
vec3 kaleido(vec2 p, float t, float n, out float edge) {
    float r = length(p);
    float a = atan(p.y, p.x + 1e-7) + t * 0.04;
    float seg = TAU / n;
    float am = mod(a, seg);
    am = abs(am - seg * 0.5);
    edge = exp(-(am * r / 0.004) * (am * r / 0.004)) * smoothstep(0.03, 0.15, r);
    vec2 q = vec2(cos(am), sin(am)) * r;
    q = rot(PI * 0.5 - seg * 0.5) * q;
    return at(q) + vec3(1.0, 0.9, 0.6) * edge * 0.6;
}

// 4 - an endless corridor: the far end of the room is another room, and another
vec3 corridor(vec2 p, float t) {
    vec2 s = vec2(abs(p.x) / (aspect() * 0.5), abs(p.y) / 0.5);
    float m = max(max(s.x, s.y), 1e-4);
    float k = log2(m / 0.3) - t * 0.25;
    float lvl = floor(k);
    float R = 0.3 * exp2(fract(k));
    vec2 q = p * (R / m);
    vec3 c = at(q);
    float doorway = exp(-(fract(k) / 0.03) * (fract(k) / 0.03));
    return c * (0.75 + 0.25 * exp2(lvl * 0.5)) + vec3(1.0, 0.85, 0.55) * doorway * 0.35;
}

// 5 - space wound round itself
vec3 twist(vec2 p, float t) {
    float r = length(p);
    float a = atan(p.y, p.x + 1e-7);
    float wind = 1.6 * sin(t * 0.3) * exp(-r * 1.6) + 0.5 * sin(r * 9.0 - t * 1.2);
    // a second, counter-rotating fibre (the Hopf linking)
    wind += 0.6 * cos(a * 2.0 + t * 0.5) * smoothstep(0.1, 0.6, r);
    vec2 q = vec2(cos(a + wind), sin(a + wind)) * r;
    return at(q);
}

// 6 - an Einstein ring with Doppler colours
vec3 lens(vec2 p, float t) {
    float r = max(length(p), 1e-4);
    vec2 dir = p / r;
    float thetaE = 0.17;
    float src = r - thetaE * thetaE / r;
    vec2 q = dir * src;
    vec3 c = at(q);
    float ring = exp(-((r - thetaE) / 0.012) * ((r - thetaE) / 0.012));
    float a = atan(p.y, p.x + 1e-7);
    float dop = 0.5 + 0.5 * cos(a - t * 0.6);
    c *= mix(vec3(1.25, 0.85, 0.7), vec3(0.7, 0.9, 1.3), dop);
    c += vec3(1.0, 0.85, 0.6) * ring * 0.8;
    c *= smoothstep(0.0, 0.12, r) * 0.6 + 0.4;
    return c;
}

// 7 - flowing waves with chromatic fringes
vec3 hyperreal(vec2 p, float t) {
    vec2 w = vec2(sin(p.y * 11.0 + t * 1.9) + 0.5 * sin(p.y * 23.0 - t * 2.7),
                  cos(p.x * 9.0 + t * 1.5) + 0.5 * cos(p.x * 19.0 + t * 2.3)) * 0.018;
    vec2 q = p + w;
    vec2 ab = p * 0.012;
    return vec3(at(q + ab).r, at(q).g, at(q - ab).b);
}

// 8 - torn slices, split channels
vec3 glitch(vec2 p, float t) {
    float band = floor((p.y + 0.5) * 38.0);
    float tick = floor(t * 7.0);
    float h1 = hash(vec2(band, tick));
    float h2 = hash(vec2(band * 3.1, tick + 7.0));
    float shift = h2 > 0.72 ? (h1 - 0.5) * 0.18 : 0.0;
    vec2 blk = floor((p + 0.5) * vec2(10.0, 7.0));
    if (hash(blk + tick) > 0.94) shift += (hash(blk * 1.7 + tick) - 0.5) * 0.25;
    vec2 q = p + vec2(shift, 0.0);
    float split = 0.006 + 0.01 * h1;
    vec3 c = vec3(at(q + vec2(split, 0.0)).r, at(q).g, at(q - vec2(split, 0.0)).b);
    return c * (0.9 + 0.1 * sin((p.y + 0.5) * ScreenSize.y * 1.6 + t * 30.0));
}

void main() {
    vec2 p = toP(texCoord0);
    vec3 orig = texture(Sampler0, texCoord0).rgb;
    float t = Clock;
    float r = length(p);
    int mode = int(Mode + 0.5);
    float e = 0.0;
    vec3 c = orig;
    if (mode == 1) c = tesseract(p, t, e);
    else if (mode == 2) c = fractal(p, t);
    else if (mode == 3) c = kaleido(p, t, 5.0, e);
    else if (mode == 4) c = corridor(p, t);
    else if (mode == 5) c = twist(p, t);
    else if (mode == 6) c = lens(p, t);
    else if (mode == 7) c = hyperreal(p, t);
    else if (mode == 8) c = glitch(p, t);
    else if (mode == 9) {
        // the transcendental: seven mirrors, nested and flowing, the colours of every law at once
        float e2;
        vec3 k = kaleido(p, t, 7.0, e2);
        vec3 d = tesseract(rot(t * 0.07) * p, t * 1.3, e);
        c = mix(k, d, 0.5 + 0.5 * sin(t * 0.21));
        c = hue(c, t * 0.4 + r * 3.0) * 0.9 + vec3(1.0, 0.95, 0.85) * (e + e2) * 0.25;
    }
    // strongest at the edges of sight, gentle round the crosshair
    float w = 0.85 * Intensity * (0.4 + 0.6 * smoothstep(0.04, 0.45, r));
    fragColor = vec4(mix(orig, c, clamp(w, 0.0, 1.0)), 1.0);
}
