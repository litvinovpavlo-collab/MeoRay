#version 120

uniform sampler2D texture;
uniform vec2 texelSize;
uniform vec3 glowColor;
uniform float radius;
uniform float intensity;
uniform int horizontal;

float gaussian(float x, float sigma) {
    return exp(-(x * x) / (2.0 * sigma * sigma));
}

void main() {
    vec2 uv = gl_TexCoord[0].st;
    vec4 center = texture2D(texture, uv);

    float totalAlpha = 0.0;
    float totalWeight = 0.0;
    float sigma = radius / 2.5;

    for (float i = -radius; i <= radius; i++) {
        vec2 offset = (horizontal == 1)
            ? vec2(i, 0.0) * texelSize
            : vec2(0.0, i) * texelSize;

        float w = gaussian(i, sigma);
        totalAlpha += texture2D(texture, uv + offset).a * w;
        totalWeight += w;
    }

    totalAlpha /= totalWeight;
    totalAlpha  = clamp(totalAlpha * intensity, 0.0, 1.0);

    float glowMask = totalAlpha * (1.0 - center.a);

    vec3 finalColor = mix(center.rgb, glowColor, glowMask);
    float finalAlpha = max(center.a, totalAlpha);

    gl_FragColor = vec4(finalColor, finalAlpha);
}
