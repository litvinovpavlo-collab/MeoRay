package dev.meoray.client.hud;

import dev.meoray.client.MeoRayClient;
import dev.meoray.client.gui.screen.MeoRayClickGUI;
import dev.meoray.client.gui.theme.Theme;
import dev.meoray.client.managers.FontManager;
import dev.meoray.client.util.render.builders.Builder;
import dev.meoray.client.util.render.builders.states.QuadColorState;
import dev.meoray.client.util.render.builders.states.QuadRadiusState;
import dev.meoray.client.util.render.builders.states.SizeState;
import dev.meoray.client.util.render.msdf.MsdfFont;
import dev.meoray.client.util.render.renderers.impl.BuiltRectangle;
import dev.meoray.client.util.render.renderers.impl.BuiltText;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;
import org.joml.Matrix4f;
import org.lwjgl.glfw.GLFW;

public class CoordsHUD {
    private static float hudX = 10;
    private static float hudY = 0;
    private static boolean isDragging;
    private static float dragOffX;
    private static float dragOffY;
    private static boolean wasMouseDown;

    public static void render(DrawContext context) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;

        if (hudY == 0) {
            hudY = mc.getWindow().getScaledHeight() - 60;
        }

        boolean editable = mc.currentScreen instanceof ChatScreen || mc.currentScreen instanceof MeoRayClickGUI;
        double mx = mc.mouse.getX();
        double my = mc.mouse.getY();
        float ms = MeoRayClient.mainScale;
        float scMx = (float) (mx / ms);
        float scMy = (float) (my / ms);

        Theme theme = MeoRayClient.INSTANCE.getThemeManager().getRenderTheme();
        MsdfFont medium = FontManager.SUISSEINTMEDIUM.get();

        String bpsStr = String.format("%.1f", Math.sqrt(mc.player.getVelocity().x * mc.player.getVelocity().x + mc.player.getVelocity().z * mc.player.getVelocity().z) * 20.0);
        String xStr = String.format("%.1f", mc.player.getPos().x);
        String yStr = String.format("%.1f", mc.player.getPos().y);
        String zStr = String.format("%.1f", mc.player.getPos().z);
        String xyzStr = xStr + ", " + yStr + ", " + zStr;
        String bpsLabel = "BPS: ";
        String xyzLabel = "XYZ: ";
        String separator = "  |  ";

        float textSize = 6.0F;
        float bpsW = medium.getWidth(bpsLabel + bpsStr, textSize);
        float sepW = medium.getWidth(separator, textSize);
        float xyzW = medium.getWidth(xyzLabel + xyzStr, textSize);
        float textW = bpsW + sepW + xyzW;
        float padX = 8;
        float padY = 6;
        float w = textW + padX + padX;
        float h = textSize + padY * 2;

        boolean mouseDown = GLFW.glfwGetMouseButton(mc.getWindow().getHandle(), GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;

        if (editable) {
            if (mouseDown && !wasMouseDown) {
                if (scMx >= hudX && scMx <= hudX + w && scMy >= hudY && scMy <= hudY + h) {
                    isDragging = true;
                    dragOffX = scMx - hudX;
                    dragOffY = scMy - hudY;
                }
            }
            if (isDragging && mouseDown) {
                hudX = scMx - dragOffX;
                hudY = scMy - dragOffY;
            }
            if (!mouseDown) {
                isDragging = false;
            }
        } else {
            isDragging = false;
        }
        wasMouseDown = mouseDown;

        float x = hudX;
        float y = hudY;

        Matrix4f matrix = context.getMatrices().peek().getPositionMatrix();

        int bg = 0xC8121216;
        ((BuiltRectangle) Builder.rectangle()
            .size(new SizeState(w, h))
            .color(new QuadColorState(bg))
            .radius(new QuadRadiusState(4.0))
            .smoothness(1.15F)
            .build()).render(matrix, x, y);

        if (editable) {
            int dotSpacing = 6;
            int dotSize = 2;
            int dotColor = 0x66FFFFFF;
            float perimeter = 2 * (w + h);
            int dots = (int) (perimeter / dotSpacing);
            for (int di = 0; di < dots; di++) {
                float dist = di * dotSpacing;
                float dx, dy;
                if (dist < w) { dx = x + dist; dy = y; }
                else if (dist < w + h) { dx = x + w; dy = y + (dist - w); }
                else if (dist < w * 2 + h) { dx = x + w - (dist - w - h); dy = y + h; }
                else { dx = x; dy = y + h - (dist - w * 2 - h); }
                ((BuiltRectangle) Builder.rectangle()
                    .size(new SizeState(dotSize, dotSize))
                    .color(new QuadColorState(dotColor))
                    .radius(new QuadRadiusState(1.0))
                    .smoothness(1.15F)
                    .build()).render(matrix, dx, dy);
            }
        }

        float cx = x + padX;
        float cy = y + padY - 1;

        ((BuiltText) Builder.text()
            .font(medium).text(bpsLabel)
            .color(0xFFCCCCCC).size(textSize).thickness(0.04F)
            .build()).render(matrix, cx, cy);
        float bpsLabelW = medium.getWidth(bpsLabel, textSize);

        ((BuiltText) Builder.text()
            .font(medium).text(bpsStr)
            .color(0xFFCCCCCC).size(textSize).thickness(0.04F)
            .build()).render(matrix, cx + bpsLabelW, cy);

        ((BuiltText) Builder.text()
            .font(medium).text(separator)
            .color(0xFF888896).size(textSize).thickness(0.04F)
            .build()).render(matrix, cx + bpsW, cy);

        ((BuiltText) Builder.text()
            .font(medium).text(xyzLabel)
            .color(0xFFCCCCCC).size(textSize).thickness(0.04F)
            .build()).render(matrix, cx + bpsW + sepW, cy);
        float xyzLabelW = medium.getWidth(xyzLabel, textSize);

        ((BuiltText) Builder.text()
            .font(medium).text(xyzStr)
            .color(0xFFCCCCCC).size(textSize).thickness(0.04F)
            .build()).render(matrix, cx + bpsW + sepW + xyzLabelW, cy);
    }
}
