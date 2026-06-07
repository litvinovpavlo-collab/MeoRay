package dev.meoray.client.hud;

import dev.meoray.client.MeoRayClient;
import dev.meoray.client.gui.theme.Theme;
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
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.MathHelper;
import org.joml.Matrix4f;

public class CustomHotbar {

    private static final float[] slotAnim = new float[9];
    private static int nameDisplayTicks = 0;
    private static int lastSelected = -1;

    public static void tick() {
        if (nameDisplayTicks > 0) nameDisplayTicks--;
    }

    public static void render(DrawContext context) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;
        PlayerEntity player = mc.player;

        Theme theme = MeoRayClient.INSTANCE.getThemeManager().getRenderTheme();
        MsdfFont medium = FontManager.SUISSEINTMEDIUM.get();

        int scaledW = mc.getWindow().getScaledWidth();
        int scaledH = mc.getWindow().getScaledHeight();
        float ms = MeoRayClient.mainScale;

        float slotSize = 22f;
        float slotGap = 2f;
        float padOut = 5f;
        float barW = slotSize * 9 + slotGap * 8 + padOut * 2;
        float barH = slotSize + padOut * 2;

        float x = (scaledW / ms - barW) / 2f;
        float y = scaledH / ms - barH - 5f;

        Matrix4f matrix = context.getMatrices().peek().getPositionMatrix();

        int accent = theme.accent();
        int accentRGB = accent & 0x00FFFFFF;

        int glowCol = accentRGB | 0xB0000000;
        GlowRenderer.drawGlow(matrix, x, y, barW, barH, glowCol, 18f, 6f);

        int bg = 0xE6121216;
        ((BuiltRectangle) Builder.rectangle()
                .size(new SizeState(barW, barH))
                .color(new QuadColorState(bg))
                .radius(new QuadRadiusState(6.0))
                .smoothness(1.15F)
                .build()).render(matrix, x, y);

        int selected = player.getInventory().selectedSlot;

        if (selected != lastSelected) {
            nameDisplayTicks = 40;
            lastSelected = selected;
        }

        for (int i = 0; i < 9; i++) {
            float targetAnim = (i == selected) ? 1f : 0f;
            slotAnim[i] = MathHelper.lerp(0.2f, slotAnim[i], targetAnim);
        }

        for (int i = 0; i < 9; i++) {
            float sx = x + padOut + i * (slotSize + slotGap);
            float sy = y + padOut;

            float anim = slotAnim[i];

            ((BuiltRectangle) Builder.rectangle()
                    .size(new SizeState(slotSize, slotSize))
                    .color(new QuadColorState(0xA0000000))
                    .radius(new QuadRadiusState(4.0))
                    .smoothness(1.15F)
                    .build()).render(matrix, sx, sy);

            if (anim > 0.01f) {
                int glowSelected = accentRGB | ((int)(0xC0 * anim) << 24);
                GlowRenderer.drawGlow(matrix, sx, sy, slotSize, slotSize, glowSelected, 8f, 4f);

                int hlBg = accentRGB | ((int)(0x30 * anim) << 24);
                ((BuiltRectangle) Builder.rectangle()
                        .size(new SizeState(slotSize, slotSize))
                        .color(new QuadColorState(hlBg))
                        .radius(new QuadRadiusState(4.0))
                        .smoothness(1.15F)
                        .build()).render(matrix, sx, sy);

                int borderSelected = accentRGB | ((int)(0xFF * anim) << 24);
                drawBorder(matrix, sx, sy, slotSize, slotSize, 1f, borderSelected);
            } else {
                drawBorder(matrix, sx, sy, slotSize, slotSize, 0.5f, 0x40FFFFFF);
            }
        }

        context.draw();
        for (int i = 0; i < 9; i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (stack.isEmpty()) continue;

            float sx = x + padOut + i * (slotSize + slotGap);
            float sy = y + padOut;

            int itemX = (int) (sx + (slotSize - 16f) / 2f);
            int itemY = (int) (sy + (slotSize - 16f) / 2f);

            context.drawItem(stack, itemX, itemY);
            context.drawStackOverlay(mc.textRenderer, stack, itemX, itemY, null);
        }
        context.draw();

        ItemStack heldStack = player.getInventory().getStack(selected);
        if (!heldStack.isEmpty() && nameDisplayTicks > 0) {
            float fadeAlpha = nameDisplayTicks > 10 ? 1f : nameDisplayTicks / 10f;

            String itemName = heldStack.getName().getString();
            float nameSize = 6.5f;
            float tw = medium.getWidth(itemName, nameSize);

            float padNameX = 8f;
            float padNameY = 4f;
            float nameBgW = tw + padNameX * 2;
            float nameBgH = nameSize + padNameY * 2;
            float nameX = (scaledW / ms - nameBgW) / 2f;
            float nameY = y - nameBgH - 4f;

            int nameBgAlpha = (int)(0xC0 * fadeAlpha);
            ((BuiltRectangle) Builder.rectangle()
                    .size(new SizeState(nameBgW, nameBgH))
                    .color(new QuadColorState((nameBgAlpha << 24)))
                    .radius(new QuadRadiusState(3.0))
                    .smoothness(1.15F)
                    .build()).render(matrix, nameX, nameY);

            int accentLineAlpha = (int)(0xFF * fadeAlpha);
            ((BuiltRectangle) Builder.rectangle()
                    .size(new SizeState(1.5f, nameBgH - 6f))
                    .color(new QuadColorState(accentRGB | (accentLineAlpha << 24)))
                    .radius(new QuadRadiusState(1.0))
                    .smoothness(1.15F)
                    .build()).render(matrix, nameX + 3f, nameY + 3f);

            int textAlpha = (int)(0xFF * fadeAlpha);
            int textCol = 0x00EEEEEE | (textAlpha << 24);
            ((BuiltText) Builder.text()
                    .font(medium).text(itemName)
                    .color(textCol).size(nameSize).thickness(0.04F)
                    .build()).render(matrix,
                    nameX + (nameBgW - tw) / 2f,
                    nameY + (nameBgH - nameSize) / 2f - 0.5f);
        }
    }

    private static void drawBorder(Matrix4f matrix, float x, float y, float w, float h, float t, int color) {
        ((BuiltRectangle) Builder.rectangle()
                .size(new SizeState(w, t)).color(new QuadColorState(color))
                .radius(new QuadRadiusState(0.0)).smoothness(1.15F).build()).render(matrix, x, y);
        ((BuiltRectangle) Builder.rectangle()
                .size(new SizeState(w, t)).color(new QuadColorState(color))
                .radius(new QuadRadiusState(0.0)).smoothness(1.15F).build()).render(matrix, x, y + h - t);
        ((BuiltRectangle) Builder.rectangle()
                .size(new SizeState(t, h)).color(new QuadColorState(color))
                .radius(new QuadRadiusState(0.0)).smoothness(1.15F).build()).render(matrix, x, y);
        ((BuiltRectangle) Builder.rectangle()
                .size(new SizeState(t, h)).color(new QuadColorState(color))
                .radius(new QuadRadiusState(0.0)).smoothness(1.15F).build()).render(matrix, x + w - t, y);
    }
}
