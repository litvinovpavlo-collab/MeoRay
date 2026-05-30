package dev.meoray.client.util.render;

import java.awt.Color;

public final class SkyConfig {
    public static Color BASE_COLOR = new Color(13, 26, 255);
    public static float[] SKY_ZENITH;
    public static float[] SKY_HORIZON;
    public static float[] NEB_COLOR1;
    public static float[] NEB_COLOR2;
    public static float NEB_INTENSITY = 0.75F;
    public static float[] STAR_COLOR;
    private static int lastRgb = Integer.MIN_VALUE;

    public static void compute() {
        int rgb = BASE_COLOR.getRGB();
        if (rgb == lastRgb) return;
        lastRgb = rgb;
        float[] hsb = Color.RGBtoHSB(BASE_COLOR.getRed(), BASE_COLOR.getGreen(), BASE_COLOR.getBlue(), null);
        float h = hsb[0], s = hsb[1], b = hsb[2];
        SKY_ZENITH = hsb2rgb(h, clamp(s * 0.9F), clamp(b * 0.12F));
        SKY_HORIZON = hsb2rgb(h, clamp(s * 0.7F), clamp(b * 0.28F));
        NEB_COLOR1 = hsb2rgb(hueShift(h, 20), clamp(s * 1.1F), clamp(b * 0.65F));
        NEB_COLOR2 = hsb2rgb(hueShift(h, -15), clamp(s * 0.85F), clamp(b * 0.4F));
        STAR_COLOR = hsb2rgb(hueShift(h, 40), clamp(s * 0.9F), clamp(b * 0.85F));
        SkyShaderHolder.markDirty();
    }

    public static void setBaseColor(Color c) { BASE_COLOR = c; compute(); }

    private static float hueShift(float h, float deg) {
        float s = h + deg / 360f;
        return s - (float) Math.floor(s);
    }

    private static float clamp(float v) { return Math.max(0, Math.min(1, v)); }

    private static float[] hsb2rgb(float h, float s, float b) {
        int p = Color.HSBtoRGB(h, s, b);
        return new float[]{
            (float) (p >> 16 & 255) * 0.003921569F,
            (float) (p >> 8 & 255) * 0.003921569F,
            (float) (p & 255) * 0.003921569F
        };
    }

    static { compute(); }
    private SkyConfig() {}
}
