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
import net.minecraft.entity.LivingEntity;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import org.joml.Matrix4f;

public class TargetHUD extends HudElement {
    private static final Draggable draggable = new Draggable("TargetHUD", 10, 170, 140, 44);

    private static LivingEntity target;
    private static long lastTargetTime;

    public TargetHUD() {
        super(draggable, "TargetHUD");
    }

    public static void setTarget(LivingEntity entity) {
        target = entity;
        lastTargetTime = System.currentTimeMillis();
    }

    @Override
    public void render(DrawContext context) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || target == null) return;

        boolean editable = mc.currentScreen instanceof ChatScreen || mc.currentScreen instanceof MeoRayClickGUI;
        if (!editable) {
            long elapsed = System.currentTimeMillis() - lastTargetTime;
            long fadeMs = 3000;
            if (elapsed > fadeMs || !target.isAlive()) {
                target = null;
                return;
            }
        }

        Theme theme = MeoRayClient.INSTANCE.getThemeManager().getRenderTheme();
        MsdfFont medium = FontManager.SUISSEINTMEDIUM.get();

        float textSize = 6.0F;
        float padX = 8;
        float padY = 6;
        float w = 140;
        float h = 44;

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

        if (editable) {
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

        int headSize = 28;
        int headX = (int) (x + 6);
        int headY = (int) (y + 6);

        if (target instanceof AbstractClientPlayerEntity player) {
            try {
                var skin = player.getSkinTextures().texture();
                context.drawTexture(
                        renderLayer -> net.minecraft.client.render.RenderLayer.getEntityTranslucent(renderLayer),
                        skin, headX, headY, 8, 8, headSize, headSize, 64, 64
                );
            } catch (Exception ignored) {}
        }

        int textX = headX + headSize + 6;
        float textY = y + padY;

        String name = target.getName().getString();
        ((BuiltText) Builder.text()
            .font(medium).text(name)
            .color(0xFFCCCCCC).size(textSize).thickness(0.04F)
            .build()).render(matrix, textX, textY);

        String hp = String.format("HP: %.1f / %.1f", target.getHealth(), target.getMaxHealth());
        ((BuiltText) Builder.text()
            .font(medium).text(hp)
            .color(0xFFCCCCCC).size(textSize).thickness(0.04F)
            .build()).render(matrix, textX, textY + textSize + 1);

        float barX = textX;
        float barY = textY + (textSize + 1) * 2 + 2;
        float barW = w - (textX - x) - padX;
        float barH = 4;

        ((BuiltRectangle) Builder.rectangle()
            .size(new SizeState(barW, barH))
            .color(new QuadColorState(0x40202020))
            .radius(new QuadRadiusState(2.0))
            .smoothness(1.15F)
            .build()).render(matrix, barX, barY);

        float hpPercent = Math.min(1f, target.getHealth() / target.getMaxHealth());
        float filledW = barW * hpPercent;

        int barColor;
        if (hpPercent > 0.6f) barColor = 0xFF55FF55;
        else if (hpPercent > 0.3f) barColor = 0xFFAA00;
        else barColor = 0xFF3333;

        ((BuiltRectangle) Builder.rectangle()
            .size(new SizeState(filledW, barH))
            .color(new QuadColorState(barColor))
            .radius(new QuadRadiusState(2.0))
            .smoothness(1.15F)
            .build()).render(matrix, barX, barY);
    }
}
