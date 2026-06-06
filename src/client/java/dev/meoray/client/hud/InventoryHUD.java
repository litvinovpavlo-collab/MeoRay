package dev.meoray.client.hud;

import dev.meoray.client.MeoRayClient;
import dev.meoray.client.gui.screen.MeoRayClickGUI;
import dev.meoray.client.gui.theme.Theme;
import dev.meoray.client.hud.draggable.Draggable;
import dev.meoray.client.util.render.GlowRenderer;
import dev.meoray.client.util.render.builders.Builder;
import dev.meoray.client.util.render.builders.states.QuadColorState;
import dev.meoray.client.util.render.builders.states.QuadRadiusState;
import dev.meoray.client.util.render.builders.states.SizeState;
import dev.meoray.client.util.render.renderers.impl.BuiltRectangle;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.item.ItemStack;
import org.joml.Matrix4f;

public class InventoryHUD extends HudElement {
    private static final Draggable draggable = new Draggable("InventoryHUD", 10, 100, 170, 60);

    public InventoryHUD() {
        super(draggable, "InventoryHUD");
    }

    @Override
    public void render(DrawContext context) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;

        boolean editable = mc.currentScreen instanceof ChatScreen || mc.currentScreen instanceof MeoRayClickGUI;
        Theme theme = MeoRayClient.INSTANCE.getThemeManager().getRenderTheme();

        int slotSize = 17;
        int cols = 9;
        int rows = 3;
        float padX = 5;
        float padY = 5;
        float gap = 1;

        float w = cols * slotSize + (cols - 1) * gap + padX * 2;
        float h = rows * slotSize + (rows - 1) * gap + padY * 2;

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

        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                int slotIndex = 9 + row * cols + col;
                ItemStack stack = mc.player.getInventory().getStack(slotIndex);

                float sx = x + padX + col * (slotSize + gap);
                float sy = y + padY + row * (slotSize + gap);

                ((BuiltRectangle) Builder.rectangle()
                    .size(new SizeState(slotSize, slotSize))
                    .color(new QuadColorState(0x30FFFFFF))
                    .radius(new QuadRadiusState(2.0))
                    .smoothness(1.15F)
                    .build()).render(matrix, sx, sy);

                if (!stack.isEmpty()) {
                    context.drawItem(stack, (int) (sx + 0.5f), (int) (sy + 0.5f));
                    context.drawStackOverlay(mc.textRenderer, stack, (int) (sx + 0.5f), (int) (sy + 0.5f));
                }
            }
        }
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
