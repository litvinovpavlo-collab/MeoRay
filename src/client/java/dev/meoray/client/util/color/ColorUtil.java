package dev.meoray.client.util.color;

import java.awt.Color;

public class ColorUtil {
    private ColorUtil() {}

    public static Color setAlpha(double alpha, Color color) {
        int a = (int) Math.max(0, Math.min(255, alpha * 255));
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), a);
    }

    public static Color setAlpha(int alpha, Color color) {
        int a = Math.max(0, Math.min(255, alpha));
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), a);
    }

    public static Color lerp(Color a, Color b, float t) {
        t = Math.max(0, Math.min(1, t));
        int r = (int) (a.getRed() + (b.getRed() - a.getRed()) * t);
        int g = (int) (a.getGreen() + (b.getGreen() - a.getGreen()) * t);
        int bl = (int) (a.getBlue() + (b.getBlue() - a.getBlue()) * t);
        int al = (int) (a.getAlpha() + (b.getAlpha() - a.getAlpha()) * t);
        return new Color(r, g, bl, al);
    }

    public static Color withAlpha(Color color, int alpha) {
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha);
    }

    public static int toInt(Color color) {
        return color.getRGB();
    }

    public static float[] normalize(int color) {
        return new float[] {
            (float)(color >> 16 & 0xFF) / 255.0F,
            (float)(color >> 8 & 0xFF) / 255.0F,
            (float)(color & 0xFF) / 255.0F,
            (float)(color >> 24 & 0xFF) / 255.0F
        };
    }
}
