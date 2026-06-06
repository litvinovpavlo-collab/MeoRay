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
import net.minecraft.client.texture.Sprite;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffectUtil;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.text.Text;
import org.joml.Matrix4f;

import java.util.Collection;

public class PotionsHUD extends HudElement {
    private static final Draggable draggable = new Draggable("PotionsHUD", 10, 40, 140, 60);

    public PotionsHUD() {
        super(draggable, "PotionsHUD");
    }

    @Override
    public void render(DrawContext context) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;

        Collection<StatusEffectInstance> effects = mc.player.getStatusEffects();
        boolean editable = mc.currentScreen instanceof ChatScreen || mc.currentScreen instanceof MeoRayClickGUI;

        if (effects.isEmpty() && !editable) return;

        Theme theme = MeoRayClient.INSTANCE.getThemeManager().getRenderTheme();
        MsdfFont medium = FontManager.SUISSEINTMEDIUM.get();
        MsdfFont icons = FontManager.ICONS.get();

        float headerH = 16f;
        float lineH = 14f;
        float padX = 8f;
        float padY = 5f;
        float iconSize = 10f;
        float iconTextGap = 5f;
        float timeGap = 10f;
        float nameTextSize = 6.0f;
        float timeTextSize = 6.0f;
        float headerTextSize = 6.5f;

        String title = "Potion-list";
        float titleW = medium.getWidth(title, headerTextSize) + 14;

        float maxRowW = titleW;
        if (effects.isEmpty()) {
            String placeholder = "Empty";
            float pw = iconSize + iconTextGap + medium.getWidth(placeholder, nameTextSize) + timeGap + medium.getWidth("00:00", timeTextSize);
            if (pw > maxRowW) maxRowW = pw;
        } else {
            for (StatusEffectInstance ef : effects) {
                String name = buildName(ef);
                String time = buildTime(ef, mc);
                float rowW = iconSize + iconTextGap + medium.getWidth(name, nameTextSize)
                        + timeGap + medium.getWidth(time, timeTextSize);
                if (rowW > maxRowW) maxRowW = rowW;
            }
        }

        int count = effects.isEmpty() ? 1 : effects.size();
        float w = maxRowW + padX * 2;
        float h = headerH + count * lineH + padY;

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

        ((BuiltRectangle) Builder.rectangle()
            .size(new SizeState(w, headerH))
            .color(new QuadColorState(0x40000000))
            .radius(new QuadRadiusState(4.0, 4.0, 0.0, 0.0))
            .smoothness(1.15F)
            .build()).render(matrix, x, y);

        ((BuiltRectangle) Builder.rectangle()
            .size(new SizeState(w - 4, 0.5f))
            .color(new QuadColorState(0x40FFFFFF))
            .radius(new QuadRadiusState(0.0))
            .smoothness(1.15F)
            .build()).render(matrix, x + 2, y + headerH - 0.5f);

        float titleX = x + padX;
        float titleY = y + (headerH - headerTextSize) / 2f - 0.5f;

        ((BuiltText) Builder.text()
            .font(icons).text("F")
            .color(0xFF55DD55).size(7.0f).thickness(0.05F)
            .build()).render(matrix, titleX, titleY - 0.5f);

        ((BuiltText) Builder.text()
            .font(medium).text(title)
            .color(0xFFCCCCCC).size(headerTextSize).thickness(0.04F)
            .build()).render(matrix, titleX + 10, titleY);

        if (editable) drawEditDots(matrix, x, y, w, h);

        if (effects.isEmpty()) {
            float ry = y + headerH + (lineH - nameTextSize) / 2f;
            ((BuiltText) Builder.text()
                .font(medium).text("Empty")
                .color(0xFF666670).size(nameTextSize).thickness(0.04F)
                .build()).render(matrix, x + padX, ry);
            return;
        }

        int i = 0;
        for (StatusEffectInstance ef : effects) {
            float ry = y + headerH + i * lineH + (lineH - iconSize) / 2f;
            float textY = y + headerH + i * lineH + (lineH - nameTextSize) / 2f;

            String name = buildName(ef);
            String time = buildTime(ef, mc);

            float iconX = x + padX;
            try {
                RegistryEntry<StatusEffect> entry = ef.getEffectType();
                Sprite sprite = mc.getStatusEffectSpriteManager().getSprite(entry);

                context.drawSpriteStretched(
                        net.minecraft.client.render.RenderLayer::getGuiTextured,
                        sprite,
                        (int) iconX, (int) ry,
                        (int) iconSize, (int) iconSize
                );
            } catch (Exception ignored) {}

            float nameX = iconX + iconSize + iconTextGap;
            ((BuiltText) Builder.text()
                .font(medium).text(name)
                .color(0xFFEEEEEE).size(nameTextSize).thickness(0.04F)
                .build()).render(matrix, nameX, textY);

            float timeW = medium.getWidth(time, timeTextSize);
            float timeX = x + w - padX - timeW;
            ((BuiltText) Builder.text()
                .font(medium).text(time)
                .color(0xFFAAAAAA).size(timeTextSize).thickness(0.04F)
                .build()).render(matrix, timeX, textY);

            i++;
        }
    }

    private String buildName(StatusEffectInstance ef) {
        String name = Text.translatable(ef.getTranslationKey()).getString();
        if (ef.getAmplifier() > 0) {
            name += " " + toRoman(ef.getAmplifier() + 1);
        }
        return name;
    }

    private String buildTime(StatusEffectInstance ef, MinecraftClient mc) {
        return StatusEffectUtil.getDurationText(ef, 1.0f,
                mc.world != null ? mc.world.getTickManager().getTickRate() : 20).getString();
    }

    private String toRoman(int n) {
        if (n <= 0) return "";
        return switch (n) {
            case 1 -> "I";
            case 2 -> "II";
            case 3 -> "III";
            case 4 -> "IV";
            case 5 -> "V";
            case 6 -> "VI";
            case 7 -> "VII";
            case 8 -> "VIII";
            case 9 -> "IX";
            case 10 -> "X";
            default -> String.valueOf(n);
        };
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
