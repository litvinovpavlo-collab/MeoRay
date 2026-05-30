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

public class InfoHUD {
    private static float hudX = 10;
    private static float hudY = 10;
    private static boolean isDragging;
    private static float dragOffX;
    private static float dragOffY;
    private static boolean wasMouseDown;

    public static void render(DrawContext context) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.getNetworkHandler() == null) return;

        boolean editable = mc.currentScreen instanceof ChatScreen || mc.currentScreen instanceof MeoRayClickGUI;
        double mx = mc.mouse.getX();
        double my = mc.mouse.getY();
        float ms = MeoRayClient.mainScale;
        float scMx = (float) (mx / ms);
        float scMy = (float) (my / ms);

        Theme theme = MeoRayClient.INSTANCE.getThemeManager().getRenderTheme();
        MsdfFont medium = FontManager.SUISSEINTMEDIUM.get();
        MsdfFont icons = FontManager.ICONS.get();

        String fps = mc.fpsDebugString != null ? mc.fpsDebugString.split(" ")[0] : "0";
        int ping = 0;
        try {
            ping = mc.getNetworkHandler().getPlayerList()
                .stream().filter(e -> e.getProfile().getId().equals(mc.player.getUuid()))
                .findFirst().map(e -> e.getLatency()).orElse(0);
        } catch (Exception ignored) {}

        String nick = mc.player.getName().getString();
        String meoray = "MEORAY";
        String sep1 = "  |  ";
        String fpsLabel = "FPS: " + fps;
        String pingLabel = "PING: " + ping + " ms";
        String nickStr = nick;

        float textSize = 6.0F;
        float pawW = 9.0F;
        float spacer = 4.0F;
        float meorayW = medium.getWidth(meoray, textSize);
        float sepW = medium.getWidth(sep1, textSize);
        float nickW = medium.getWidth(nickStr, textSize);
        float fpsW = medium.getWidth(fpsLabel, textSize);
        float pingW = medium.getWidth(pingLabel, textSize);
        float textW = pawW + spacer + meorayW + sepW + nickW + sepW + fpsW + sepW + pingW;
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
            .font(icons).text("D")
            .color(theme.accent()).size(pawW).thickness(0.05F)
            .build()).render(matrix, cx, cy - 1);

        float cx2 = cx + pawW + spacer;
        ((BuiltText) Builder.text()
            .font(medium).text(meoray)
            .color(0xFFCCCCCC).size(textSize).thickness(0.04F)
            .build()).render(matrix, cx2, cy);

        float cx3 = cx2 + meorayW;
        ((BuiltText) Builder.text()
            .font(medium).text(sep1)
            .color(0xFF888896).size(textSize).thickness(0.04F)
            .build()).render(matrix, cx3, cy);

        float cx4 = cx3 + sepW;
        ((BuiltText) Builder.text()
            .font(medium).text(nickStr)
            .color(0xFFCCCCCC).size(textSize).thickness(0.04F)
            .build()).render(matrix, cx4, cy);

        float cx5 = cx4 + nickW;
        ((BuiltText) Builder.text()
            .font(medium).text(sep1)
            .color(0xFF888896).size(textSize).thickness(0.04F)
            .build()).render(matrix, cx5, cy);

        float cx6 = cx5 + sepW;
        ((BuiltText) Builder.text()
            .font(medium).text(fpsLabel)
            .color(0xFFCCCCCC).size(textSize).thickness(0.04F)
            .build()).render(matrix, cx6, cy);

        float cx7 = cx6 + fpsW;
        ((BuiltText) Builder.text()
            .font(medium).text(sep1)
            .color(0xFF888896).size(textSize).thickness(0.04F)
            .build()).render(matrix, cx7, cy);

        float cx8 = cx7 + sepW;
        ((BuiltText) Builder.text()
            .font(medium).text(pingLabel)
            .color(0xFFCCCCCC).size(textSize).thickness(0.04F)
            .build()).render(matrix, cx8, cy);
    }
}
