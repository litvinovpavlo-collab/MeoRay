package dev.meoray.client.render;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;

import java.awt.*;

public class FontUtil {
    private FontUtil() {}

    public static void drawString(DrawContext context, String text, float x, float y, Color color) {
        TextRenderer tr = MinecraftClient.getInstance().textRenderer;
        context.drawText(tr, text, (int) x, (int) y, color.getRGB(), true);
    }

    public static void drawString(DrawContext context, String text, float x, float y, int color) {
        TextRenderer tr = MinecraftClient.getInstance().textRenderer;
        context.drawText(tr, text, (int) x, (int) y, color, true);
    }

    public static float getWidth(String text) {
        return MinecraftClient.getInstance().textRenderer.getWidth(text);
    }

    public static float getHeight() {
        return MinecraftClient.getInstance().textRenderer.fontHeight;
    }
}
