#version 150


uniform float GameTime;
uniform vec4  FogColor;
uniform float FogStart;
uniform float FogEnd;


uniform vec3  SkyZenith;
uniform vec3  SkyHorizon;
uniform vec3  NebColor1;
uniform vec3  NebColor2;
uniform float NebIntensity;
uniform vec3  StarColor;

in  vec3 vRayDir;
out vec4 fragColor;


float hash(vec2 p) {
    uvec2 q = uvec2(ivec2(p)) * uvec2(1597334673u, 3812015801u);
    return float((q.x ^ q.y) * 1597334673u) * (1.0/4294967296.0);
}


float hash3(vec3 p) {
    uvec3 q = uvec3(ivec3(p)) * uvec3(1597334673u, 3812015801u, 2798796415u);
    return float((q.x ^ q.y ^ q.z) * 1597334673u) * (1.0/4294967296.0);
}


float vnoise(vec3 p) {
    vec3 i = floor(p), f = fract(p);
    vec3 u = f*f*(3.0-2.0*f);


    float c000 = hash3(i);
    float c100 = hash3(i + vec3(1,0,0));
    float c010 = hash3(i + vec3(0,1,0));
    float c110 = hash3(i + vec3(1,1,0));
    float c001 = hash3(i + vec3(0,0,1));
    float c101 = hash3(i + vec3(1,0,1));
    float c011 = hash3(i + vec3(0,1,1));
    float c111 = hash3(i + vec3(1,1,1));


    vec2 x0 = mix(vec2(c000,c001), vec2(c100,c101), u.x);
    vec2 x1 = mix(vec2(c010,c011), vec2(c110,c111), u.x);
    vec2 y  = mix(x0, x1, u.y);
    return mix(y.x, y.y, u.z);
}


float fbm(vec3 p) {
    const mat3 rot = mat3(0.8,-0.6,0.0, 0.6,0.8,0.0, 0.0,0.0,1.0);
    const vec3 off = vec3(1.7, 9.2, 5.3);

    float v  = 0.5000 * vnoise(p); p = rot*p*2.1 + off;
    v       += 0.2500 * vnoise(p); p = rot*p*2.1 + off;
    v       += 0.1250 * vnoise(p); p = rot*p*2.1 + off;
    v       += 0.0625 * vnoise(p);
    return v * (1.0/0.9375);   // mul быстрее div
}


float starLayer(vec3 dir, float scale, float threshold, float seed) {

    vec2 uv = vec2(atan(dir.z, dir.x) * (1.0/6.2832) + 0.5,
                   dir.y * 0.5 + 0.5) * scale;
    vec2 id  = floor(uv);
    float h  = hash(id + seed);
    if (h < threshold) return 0.0;

    vec2 gv  = fract(uv) - 0.5;

    vec2 pos = (vec2(hash(id + (seed + 13.7)),
                     hash(id + (seed + 71.3))) - 0.5) * 0.7;

    return smoothstep(0.05, 0.0, length(gv - pos))
         * (0.7 + 0.3*sin(GameTime*600.0 + h*400.0));
}

void main() {
    vec3  dir = normalize(vRayDir);
    float up  = clamp( dir.y, 0.0, 1.0);
    float dn  = clamp(-dir.y, 0.0, 1.0);


    vec3 sky = mix(SkyHorizon, SkyZenith, pow(up, 0.6));
    sky = mix(sky, SkyZenith * 0.4,   pow(dn,        1.5));
    sky = mix(sky, SkyHorizon,         pow(1.0-up, 3.0) * clamp(1.0-dn*4.0, 0.0, 1.0));


    float aboveH = smoothstep(-0.08, 0.25, dir.y);


    vec3  nebBase1 = dir*2.0 + vec3(0.0, 0.0, GameTime*0.004);
    float neb1 = smoothstep(0.38, 0.72, fbm(nebBase1));


    vec3  nebBase2 = dir*4.5 + vec3(1.3, 2.7, GameTime*0.007);
    float neb2 = smoothstep(0.45, 0.75, fbm(nebBase2));

    sky += NebColor1 * (neb1 * aboveH * (NebIntensity * 0.75));
    sky += NebColor2 * (neb2 * aboveH * (NebIntensity * 0.45));


    float s1 = starLayer(dir, 120.0, 0.968,  0.0);
    float s2 = starLayer(dir,  75.0, 0.980, 17.3);
    float s3 = starLayer(dir,  40.0, 0.990, 43.7);
    float s4 = starLayer(dir,  20.0, 0.994, 91.1);

    float hv     = hash(floor(vec2(atan(dir.z, dir.x), dir.y) * 35.0));
    vec3  cSmall = mix(vec3(0.65,0.70,1.0), vec3(0.80,0.88,1.0), hv);


    float s3s = s3*s3, s4s = s4*s4;

    vec3 starContrib = cSmall * (s1*0.45 + s2*0.70)
                     + StarColor * (s3*1.20 + s4*1.80)
                     + StarColor * StarColor * (s3s*0.5 + s4s*1.2) * 0.5;
    sky += starContrib * aboveH;


    sky += SkyHorizon * (pow(1.0 - abs(dir.y), 8.0) * 0.08);


    float fogRange = max(FogEnd - FogStart, 1.0);
    float fogAmt   = clamp((1.0-abs(dir.y))*FogEnd - FogStart, 0.0, fogRange) / fogRange;
    fogAmt *= fogAmt;
    sky = mix(sky, FogColor.rgb, fogAmt * FogColor.a * 0.85);

    fragColor = vec4(clamp(sky / (sky + 0.22) * 1.40, 0.0, 1.0), 1.0);
}
