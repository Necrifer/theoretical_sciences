#version 150

// Screen-space gravitational lensing.
// Sampler0 is a copy of the scene. Each fragment of the billboard looks up where its light
// "really" came from using the thin-lens equation  beta = theta - thetaE^2 / theta.
// Mode 0: black hole (Schwarzschild shadow + photon ring)
// Mode 1: white hole (time-reversed: rays diverge, blinding core)
// Mode 2: dark-matter halo (weak lensing, no horizon)

uniform sampler2D Sampler0;
uniform vec2 ScreenSize;
uniform float GameTime;
uniform vec2 Center;
uniform float Radius;
uniform float Mode;
uniform float Strength;

in vec2 texCoord0;
out vec4 fragColor;

vec3 scene(vec2 px) {
    vec2 uv = clamp(px / ScreenSize, vec2(0.0015), vec2(0.9985));
    return texture(Sampler0, uv).rgb;
}

void main() {
    vec2 q = texCoord0 * 2.0 - 1.0;
    float edge = length(q);
    if (edge > 1.0) discard;
    float fade = 1.0 - smoothstep(0.45, 1.0, edge);

    vec2 frag = gl_FragCoord.xy;
    vec2 d = frag - Center;
    float r = max(length(d), 0.0001);
    vec2 dir = d / r;
    float rs = max(Radius, 0.5);
    float t = GameTime * 1200.0;
    vec3 col;

    if (Mode < 0.5) {
        float thetaE = rs * 2.2 * Strength;
        float src = r - thetaE * thetaE / r;
        vec2 lensed = Center + dir * src;
        col = scene(mix(frag, lensed, fade));
        // slight chromatic smear along the Einstein ring
        float near = exp(-pow((r - thetaE) / (rs * 0.6), 2.0)) * fade;
        col.r = mix(col.r, scene(mix(frag, Center + dir * (src * 1.01), fade)).r, near);
        col.b = mix(col.b, scene(mix(frag, Center + dir * (src * 0.99), fade)).b, near);
        // photon ring
        float ringR = rs * 1.32;
        float ring = exp(-pow((r - ringR) / (rs * 0.07), 2.0));
        float flicker = 0.85 + 0.15 * sin(atan(d.y, d.x) * 7.0 + t * 3.0);
        col += vec3(1.0, 0.8, 0.55) * ring * flicker * 1.4;
        // gravitational redshift towards the shadow
        float dim = smoothstep(rs, rs * 1.9, r);
        col *= mix(1.0, dim, fade);
        if (r < rs) col = vec3(0.0);
    } else if (Mode < 1.5) {
        float thetaE = rs * 1.4 * Strength;
        float src = r + thetaE * thetaE / r * 0.5;
        col = scene(mix(frag, Center + dir * src, fade));
        float ang = atan(d.y, d.x);
        float rays = pow(max(0.0, sin(ang * 11.0 + t * 1.7)), 10.0) + pow(max(0.0, sin(ang * 5.0 - t * 1.1)), 14.0);
        float halo = exp(-r / (rs * 2.2));
        float core = smoothstep(rs * 1.2, rs * 0.6, r);
        col += vec3(1.0, 0.93, 0.8) * (rays * exp(-r / (rs * 5.0)) * 0.9 + halo * 0.8) * fade;
        col = mix(col, vec3(1.0, 0.99, 0.96), core);
    } else if (Mode > 5.5) {
        // reality breach: space shatters into Voronoi shards, each refracting a different offset of the scene
        vec2 g = frag / max(rs * 0.55, 3.0);
        vec2 cell = floor(g), best = vec2(0.0), second = vec2(9.0);
        float d1 = 9.0, d2 = 9.0;
        for (int j = -1; j <= 1; j++) for (int i = -1; i <= 1; i++) {
            vec2 c = cell + vec2(i, j);
            vec2 h = vec2(fract(sin(dot(c, vec2(127.1, 311.7))) * 43758.5453), fract(sin(dot(c, vec2(269.5, 183.3))) * 43758.5453));
            vec2 o = c + 0.5 + 0.4 * sin(t * 0.6 + 6.2831 * h);
            float dd = length(g - o);
            if (dd < d1) { d2 = d1; d1 = dd; best = c; } else if (dd < d2) { d2 = dd; }
        }
        float hsh = fract(sin(dot(best, vec2(41.3, 289.1))) * 15731.743);
        vec2 off = vec2(cos(hsh * 6.2831 + t * 0.3), sin(hsh * 6.2831 + t * 0.3)) * rs * (0.4 + hsh) * fade * Strength;
        col = scene(frag + off);
        if (hsh > 0.8) col = vec3(1.0) - col;                 // some shards show an inverted reality
        else if (hsh > 0.6) col = col.gbr;                    // others obey different colour laws
        float crack = 1.0 - smoothstep(0.0, 0.06, d2 - d1);
        col = mix(col, vec3(1.0, 0.95, 0.85), crack * fade);
        float core = smoothstep(rs * 0.9, rs * 0.2, r);
        col = mix(col, vec3(1.0), core * 0.9);
    } else if (Mode > 4.5) {
        // reality glitch: horizontal slices of the scene are displaced, colour channels split
        float band = floor(frag.y / max(rs * 0.35, 2.0));
        float tick = floor(t * 9.0);
        float h1 = fract(sin(band * 12.9898 + tick * 78.233) * 43758.5453);
        float h2 = fract(sin(band * 4.1414 + tick * 19.19) * 23421.631);
        float shift = (h2 > 0.55 ? (h1 - 0.5) * rs * 1.6 : 0.0) * fade;
        vec2 p0 = frag + vec2(shift, 0.0);
        float split = rs * 0.12 * fade;
        col = vec3(scene(p0 + vec2(split, 0.0)).r, scene(p0).g, scene(p0 - vec2(split, 0.0)).b);
        float scan = 0.85 + 0.15 * sin(frag.y * 1.7 + t * 20.0);
        col = mix(col, col * scan * vec3(0.7, 1.15, 0.8), fade * 0.6);
    } else if (Mode > 3.5) {
        // traversable wormhole: inside the throat you see a fisheye view of "elsewhere"
        float throat = rs * 1.6;
        if (r < throat) {
            float k = r / throat;
            float ang = atan(d.y, d.x) + t * 0.25 + (1.0 - k) * 2.5;
            vec2 other = Center + vec2(cos(ang), sin(ang)) * (throat * 3.2 * pow(k, 0.6));
            col = scene(other) * vec3(0.75, 0.9, 1.25);
            col += vec3(0.3, 0.55, 1.0) * pow(k, 6.0) * 0.8;
        } else {
            float thetaE = throat * 1.25 * Strength;
            float src = r - thetaE * thetaE / r * 0.6;
            col = scene(mix(frag, Center + dir * src, fade));
            float ring = exp(-pow((r - throat) / (rs * 0.12), 2.0));
            col += vec3(0.5, 0.75, 1.0) * ring * 1.2;
        }
    } else if (Mode > 2.5) {
        // multiverse rift: space is twisted around the tear, colours split between universes
        float swirl = (rs * 3.0 / (r + rs * 0.5)) * fade * Strength;
        float ang = atan(d.y, d.x) + swirl + sin(t * 1.3) * 0.15 * fade;
        vec2 sp = Center + vec2(cos(ang), sin(ang)) * r;
        vec2 shift = dir * rs * 0.08 * fade;
        col = vec3(scene(mix(frag, sp + shift, fade)).r, scene(mix(frag, sp, fade)).g, scene(mix(frag, sp - shift, fade)).b);
        col = mix(col, col * vec3(1.1, 0.8, 1.25), fade * 0.4);
    } else {
        float thetaE = rs * 1.1 * Strength;
        float wob = sin(r * 0.12 - t * 1.4) * 1.2 * fade;
        float src = r - thetaE * thetaE / (r + rs * 0.6) + wob;
        col = scene(mix(frag, Center + dir * src, fade));
        col = mix(col, col * vec3(0.82, 0.78, 1.18) + vec3(0.02, 0.0, 0.05), fade * 0.55);
    }
    fragColor = vec4(col, 1.0);
}
