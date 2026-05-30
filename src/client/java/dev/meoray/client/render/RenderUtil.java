package dev.meoray.client.render;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.math.MathHelper;

import java.awt.*;

public class RenderUtil {
    private RenderUtil() {}

    private static final int CORNER_SEGMENTS = 20;

    public static void drawRounded(DrawContext context, float x, float y, float width, float height, float radius, Color color) {
        if (width <= 0 || height <= 0) return;
        
        float maxR = Math.min(width, height) / 2f;
        radius = MathHelper.clamp(radius, 0, maxR);

        int colRGB = color.getRGB();
        int ix = (int) Math.floor(x);
        int iy = (int) Math.floor(y);
        int iw = (int) Math.ceil(x + width) - ix;
        int ih = (int) Math.ceil(y + height) - iy;
        int ir = (int) Math.ceil(radius);

        // Центральная часть (без углов)
        context.fill(ix + ir, iy, ix + iw - ir, iy + ih, colRGB);
        context.fill(ix, iy + ir, ix + ir, iy + ih - ir, colRGB);
        context.fill(ix + iw - ir, iy + ir, ix + iw, iy + ih - ir, colRGB);

        // Углы (простые круги через пиксели)
        drawCorner(context, ix + ir, iy + ir, ir, colRGB, -1, -1);
        drawCorner(context, ix + iw - ir, iy + ir, ir, colRGB, 1, -1);
        drawCorner(context, ix + iw - ir, iy + ih - ir, ir, colRGB, 1, 1);
        drawCorner(context, ix + ir, iy + ih - ir, ir, colRGB, -1, 1);
    }

    private static void drawCorner(DrawContext context, int cx, int cy, int radius, int color, int dirX, int dirY) {
        for (int dy = -radius; dy <= radius; dy++) {
            for (int dx = -radius; dx <= radius; dx++) {
                float dist = (float) Math.sqrt(dx * dx + dy * dy);
                if (dist <= radius) {
                    int px = cx + dx * dirX;
                    int py = cy + dy * dirY;
                    context.fill(px, py, px + 1, py + 1, color);
                }
            }
        }
    }

    public static void drawRoundedOutline(DrawContext context, float x, float y, float width, float height, float radius, Color color) {
        drawRoundedOutline(context, x, y, width, height, radius, 1.0f, color);
    }

    public static void drawRoundedOutline(DrawContext context, float x, float y, float width, float height, float radius, float thickness, Color color) {
        if (width <= 0 || height <= 0) return;
        
        float maxR = Math.min(width, height) / 2f;
        radius = MathHelper.clamp(radius, 0, maxR);

        int colRGB = color.getRGB();
        int ix = (int) Math.floor(x);
        int iy = (int) Math.floor(y);
        int iw = (int) Math.ceil(x + width) - ix;
        int ih = (int) Math.ceil(y + height) - iy;
        int ir = (int) Math.ceil(radius);
        int it = (int) Math.ceil(thickness);

        // Прямые линии обводки
        context.fill(ix + ir, iy, ix + iw - ir, iy + it, colRGB);
        context.fill(ix + ir, iy + ih - it, ix + iw - ir, iy + ih, colRGB);
        context.fill(ix, iy + ir, ix + it, iy + ih - ir, colRGB);
        context.fill(ix + iw - it, iy + ir, ix + iw, iy + ih - ir, colRGB);

        // Угловые дуги обводки
        drawCornerOutline(context, ix + ir, iy + ir, ir, (int) thickness, colRGB, -1, -1);
        drawCornerOutline(context, ix + iw - ir, iy + ir, ir, (int) thickness, colRGB, 1, -1);
        drawCornerOutline(context, ix + iw - ir, iy + ih - ir, ir, (int) thickness, colRGB, 1, 1);
        drawCornerOutline(context, ix + ir, iy + ih - ir, ir, (int) thickness, colRGB, -1, 1);
    }

    private static void drawCornerOutline(DrawContext context, int cx, int cy, int radius, int thickness, int color, int dirX, int dirY) {
        int innerR = radius - thickness;
        if (innerR < 0) innerR = 0;

        for (int dy = -radius; dy <= radius; dy++) {
            for (int dx = -radius; dx <= radius; dx++) {
                float dist = (float) Math.sqrt(dx * dx + dy * dy);
                if (dist >= innerR && dist <= radius) {
                    int px = cx + dx * dirX;
                    int py = cy + dy * dirY;
                    context.fill(px, py, px + 1, py + 1, color);
                }
            }
        }
    }

    public static void drawShadow(DrawContext context, float x, float y, float width, float height, int radius) {
        drawRounded(context, x - 2, y - 2, width + 4, height + 4, radius + 2, new Color(0, 0, 0, 40));
        drawRounded(context, x - 1, y - 1, width + 2, height + 2, radius + 1, new Color(0, 0, 0, 70));
    }

    public static void drawGlow(DrawContext context, float x, float y, float width, float height, float radius, Color color, float strength) {
        if (strength <= 0.01f) return;
        int a1 = (int) (30 * strength);
        int a2 = (int) (50 * strength);
        int baseR = color.getRed(), baseG = color.getGreen(), baseB = color.getBlue();

        Color outer = new Color(baseR, baseG, baseB, a1);
        Color inner = new Color(baseR, baseG, baseB, a2);

        drawRounded(context, x - 1, y - 1, width + 2, height + 2, radius, outer);
        drawRounded(context, x, y, width, height, radius, inner);
    }

    public static boolean hovered(float mx, float my, float x, float y, float w, float h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }
}
