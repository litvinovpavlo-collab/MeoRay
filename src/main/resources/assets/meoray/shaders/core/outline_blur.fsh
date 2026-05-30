#version 150

uniform sampler2D InSampler;
uniform sampler2D MainSampler;

uniform vec2 InSize;
uniform float BlurRadius;
uniform vec4 TintColor;

in vec2 texCoord;
out vec4 fragColor;

vec4 gaussianBlur(sampler2D tex, vec2 uv, vec2 texelSize, float radius) {
    vec4 result = vec4(0.0);
    float total = 0.0;
    int steps = int(radius) * 2 + 1;

    for (int x = -int(radius); x <= int(radius); x++) {
        for (int y = -int(radius); y <= int(radius); y++) {
            float weight = exp(-(x*x + y*y) / (2.0 * radius * radius));
            result += texture(tex, uv + vec2(x, y) * texelSize) * weight;
            total += weight;
        }
    }
    return result / total;
}

void main() {
    vec2 texelSize = 1.0 / InSize;
    vec4 scene = texture(MainSampler, texCoord);
    vec4 silhouette = texture(InSampler, texCoord);

    vec4 blurred = gaussianBlur(InSampler, texCoord, texelSize, BlurRadius);

    float outlineAlpha = clamp(blurred.a - silhouette.a, 0.0, 1.0);
    vec4 outlineColor = vec4(TintColor.rgb, outlineAlpha * TintColor.a);

    fragColor = mix(scene, outlineColor, outlineColor.a);
}
