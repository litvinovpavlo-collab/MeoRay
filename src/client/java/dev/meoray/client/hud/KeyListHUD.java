package dev.meoray.client.hud;

import dev.meoray.client.MeoRayClient;
import dev.meoray.client.gui.screen.MeoRayClickGUI;
import dev.meoray.client.gui.theme.Theme;
import dev.meoray.client.hud.draggable.Draggable;
import dev.meoray.client.managers.FontManager;
import dev.meoray.client.util.render.GlowRenderer;
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
import net.minecraft.client.util.InputUtil;
import org.joml.Matrix4f;
import org.lwjgl.glfw.GLFW;

public class KeyListHUD extends HudElement {
    private static final Draggable draggable = new Draggable("KeyListHUD", 10, 250, 70, 90);

    public KeyListHUD() {
        super(draggable, "KeyListHUD");
    }

    @Override
    public void render(DrawContext context) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;

        boolean editable = mc.currentScreen instanceof ChatScreen || mc.currentScreen instanceof MeoRayClickGUI;
        Theme theme = MeoRayClient.INSTANCE.getThemeManager().getRenderTheme();
        MsdfFont medium = FontManager.SUISSEINTMEDIUM.get();

        float keySize = 16f;
        float gap = 2f;
        float padX = 6f;
        float padY = 6f;

        float gridW = keySize * 3 + gap * 2;
        float w = gridW + padX * 2;
        float h = keySize * 4 + gap * 3 + padY * 2;

        draggable.setWidth(w);
        draggable.setHeight(h);

        float x = draggable.getX();
        float y = draggable.getY();

        Matrix4f matrix = context.getMatrices().peek().getPositionMatrix();

        int glowCol = (theme.accent() & 0x00FFFFFF) | 0xD0000000;
        GlowRenderer.drawGlow(matrix, x, y, w, h, glowCol, 18f, 4.0f);

        int bg = 0xE6121216;
        ((BuiltRectangle) Builder.rectangle()
            .size(new SizeState(w, h))
            .color(new QuadColorState(bg))
            .radius(new QuadRadiusState(4.0))
            .smoothness(1.15F)
            .build()).render(matrix, x, y);

        if (editable) drawEditDots(matrix, x, y, w, h);

        float gx = x + padX;
        float gy = y + padY;

        drawKey(matrix, medium, theme, "W", gx + keySize + gap, gy, keySize, keySize, GLFW.GLFW_KEY_W);
        drawKey(matrix, medium, theme, "A", gx, gy + keySize + gap, keySize, keySize, GLFW.GLFW_KEY_A);
        drawKey(matrix, medium, theme, "S", gx + keySize + gap, gy + keySize + gap, keySize, keySize, GLFW.GLFW_KEY_S);
        drawKey(matrix, medium, theme, "D", gx + (keySize + gap) * 2, gy + keySize + gap, keySize, keySize, GLFW.GLFW_KEY_D);
        drawKey(matrix, medium, theme, "SPACE", gx, gy + (keySize + gap) * 2, gridW, keySize, GLFW.GLFW_KEY_SPACE);
        drawKey(matrix, medium, theme, "SHIFT", gx, gy + (keySize + gap) * 3, gridW, keySize, GLFW.GLFW_KEY_LEFT_SHIFT);
    }

    private void drawKey(Matrix4f matrix, MsdfFont font, Theme theme,
                          String label, float x, float y, float w, float h, int keyCode) {
        boolean pressed = isPressed(keyCode);

        int bgCol = pressed
                ? (theme.accent() & 0x00FFFFFF) | 0xE0000000
                : 0xCC1A1A1F;
        int textCol = pressed ? 0xFFFFFFFF : 0xFFAAAAAA;

        ((BuiltRectangle) Builder.rectangle()
            .size(new SizeState(w, h))
            .color(new QuadColorState(bgCol))
            .radius(new QuadRadiusState(3.0))
            .smoothness(1.15F)
            .build()).render(matrix, x, y);

        float textSize = 5.0f;
        float tw = font.getWidth(label, textSize);
        ((BuiltText) Builder.text()
            .font(font).text(label)
            .color(textCol).size(textSize).thickness(0.04F)
            .build()).render(matrix, x + (w - tw) / 2f, y + (h - textSize) / 2f);
    }

    private boolean isPressed(int key) {
        long handle = MinecraftClient.getInstance().getWindow().getHandle();
        return InputUtil.isKeyPressed(handle, key);
    }

    private void drawEditDots(Matrix4f matrix, float x, float y, float w, float h) {
        int dotSpacing = 5;
        int dotSize = 2;
        int dotColor = 0x99FFFFFF;
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
}
