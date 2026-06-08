package dev.meoray.client.hud;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.meoray.client.MeoRayClient;
import dev.meoray.client.hud.draggable.Draggable;
import dev.meoray.client.util.render.builders.Builder;
import dev.meoray.client.util.render.builders.states.QuadColorState;
import dev.meoray.client.util.render.builders.states.QuadRadiusState;
import dev.meoray.client.util.render.builders.states.SizeState;
import dev.meoray.client.util.render.renderers.impl.BuiltRectangle;
import dev.meoray.client.util.render.renderers.impl.BuiltText;
import dev.meoray.client.managers.FontManager;
import dev.meoray.client.util.render.msdf.MsdfFont;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import org.joml.Matrix4f;

import java.awt.*;

public class TargetHUD extends HudElement {

    private final MinecraftClient mc = MinecraftClient.getInstance();
    private Entity currentTarget = null;
    private float animProgress = 0f;
    private float displayedHealth = 0f;

    private static final Draggable DRAGGABLE = new Draggable("target", 2, 120, 140, 50) {
        @Override public boolean canPush() { return true; }
    };

    public TargetHUD() {
        super(DRAGGABLE, "TargetHUD");
    }

    @Override
    public void render(DrawContext context) {
        if (mc.player == null || mc.world == null) return;

        Entity target = getTargetEntity();
        if (target instanceof ArmorStandEntity) target = null;

        if (target != currentTarget) {
            currentTarget = target;
            animProgress = 0f;
            if (target instanceof LivingEntity le) displayedHealth = le.getHealth();
        }

        if (target == null) {
            animProgress = Math.max(0, animProgress - 0.08f);
            if (animProgress <= 0) return;
        } else {
            animProgress = Math.min(1, animProgress + 0.08f);
        }

        Matrix4f matrix = context.getMatrices().peek().getPositionMatrix();
        MsdfFont medium = FontManager.SUISSEINTMEDIUM.get();

        float x = DRAGGABLE.getX();
        float y = DRAGGABLE.getY();
        float w = 140;
        float h = 50;
        DRAGGABLE.setWidth(w);
        DRAGGABLE.setHeight(h);

        float a = animProgress;
        int bg = 0xE6111116;

        // Главный фон
        ((BuiltRectangle) Builder.rectangle()
            .size(new SizeState(w, h))
            .color(new QuadColorState(alphaBlend(bg, a)))
            .radius(new QuadRadiusState(7f))
            .smoothness(1.15f)
            .build()).render(matrix, x, y);

        if (!(target instanceof LivingEntity living)) {
            if (isEditMode()) renderEditDots(context, x, y, w, h);
            return;
        }

        // Имя / тип
        String name = living.getName().getString();
        String type;
        if (living instanceof PlayerEntity) type = "Player";
        else if (living instanceof HostileEntity || living instanceof Monster) type = "Hostile";
        else if (living instanceof AnimalEntity) type = "Animal";
        else if (living instanceof VillagerEntity) type = "Villager";
        else type = "Entity";

        float health = living.getHealth();
        float maxHealth = living.getMaxHealth();

        // Плавное изменение HP
        displayedHealth = MathHelper.lerp(0.2f, displayedHealth, health);

        // === ГОЛОВА ===
        int headSize = 32;
        float headX = x + 9;
        float headY = y + (h - headSize) / 2f;

        // Подложка под голову
        ((BuiltRectangle) Builder.rectangle()
            .size(new SizeState(headSize + 2, headSize + 2))
            .color(new QuadColorState(alphaBlend(0xFF1A1A20, a)))
            .radius(new QuadRadiusState(5f))
            .smoothness(1.15f)
            .build()).render(matrix, headX - 1, headY - 1);

        if (living instanceof AbstractClientPlayerEntity player) {
            Identifier skinId = player.getSkinTextures().texture();

            context.getMatrices().push();

            // Масштаб = headSize / 8 (потому что регион головы 8x8)
            float scale = headSize / 8f;
            context.getMatrices().translate(headX, headY, 0);
            context.getMatrices().scale(scale, scale, 1f);

            // Базовый слой головы
            context.drawTexture(
                RenderLayer::getGuiTextured,
                skinId,
                0, 0,
                8f, 8f,
                8, 8,
                64, 64
            );

            // Overlay (шляпа)
            context.drawTexture(
                RenderLayer::getGuiTextured,
                skinId,
                0, 0,
                40f, 8f,
                8, 8,
                64, 64
            );

            context.getMatrices().pop();
        } else {
            // Для не-игроков просто иконка/буква
            String typeIcon = type.substring(0, 1);
            float iconW = medium.getWidth(typeIcon, 14f);
            ((BuiltText) Builder.text().font(medium).text(typeIcon)
                .color(alphaBlend(0xFFAAAAAA, a)).size(14f).thickness(0.05f).build())
                .render(matrix, headX + (headSize - iconW) / 2f, headY + 8);
        }

        // === ТЕКСТ ===
        float textX = headX + headSize + 8;
        float nameY = y + 9;
        float infoY = nameY + 9;

        int nameCol = alphaBlend(0xFFFFFFFF, a);
        int infoCol = alphaBlend(0xFFAAAAAA, a);

        ((BuiltText) Builder.text().font(medium).text(name)
            .color(nameCol).size(7f).thickness(0.04f).build())
            .render(matrix, textX, nameY);

        float dist = mc.player.distanceTo(living);
        String info = type + " \u2022 " + String.format("%.1f", dist) + "m";
        ((BuiltText) Builder.text().font(medium).text(info)
            .color(infoCol).size(5f).thickness(0.04f).build())
            .render(matrix, textX, infoY);

        // === HP TEXT (справа сверху) ===
        String hpStr = String.format("%.0f/%.0f", health, maxHealth);
        float hpStrSize = 5.5f;
        float hpStrW = medium.getWidth(hpStr, hpStrSize);
        ((BuiltText) Builder.text().font(medium).text(hpStr)
            .color(nameCol).size(hpStrSize).thickness(0.04f).build())
            .render(matrix, x + w - 9 - hpStrW, y + 9);

        // === HP БАР ===
        float barX = textX;
        float barW = w - (textX - x) - 9;
        float barH = 4f;
        float barY = y + h - 10;

        float healthPct = maxHealth > 0 ? MathHelper.clamp(health / maxHealth, 0, 1) : 0;
        float displayedPct = maxHealth > 0 ? MathHelper.clamp(displayedHealth / maxHealth, 0, 1) : 0;

        // Фон бара
        ((BuiltRectangle) Builder.rectangle()
            .size(new SizeState(barW, barH))
            .color(new QuadColorState(alphaBlend(0xFF2A2A2F, a)))
            .radius(new QuadRadiusState(2f))
            .smoothness(1.15f)
            .build()).render(matrix, barX, barY);

        // "Призрачный" хвост (анимация урона) - оранжевый позади
        if (displayedPct > healthPct + 0.001f) {
            ((BuiltRectangle) Builder.rectangle()
                .size(new SizeState(barW * displayedPct, barH))
                .color(new QuadColorState(alphaBlend(0xFFFFAA22, a * 0.8f)))
                .radius(new QuadRadiusState(2f))
                .smoothness(1.15f)
                .build()).render(matrix, barX, barY);
        }

        // Основной HP бар
        int healthColor;
        if (healthPct > 0.6f) healthColor = 0xFF44DD66;
        else if (healthPct > 0.3f) healthColor = 0xFFFFAA22;
        else healthColor = 0xFFFF4444;

        if (healthPct > 0.01f) {
            ((BuiltRectangle) Builder.rectangle()
                .size(new SizeState(barW * healthPct, barH))
                .color(new QuadColorState(alphaBlend(healthColor, a)))
                .radius(new QuadRadiusState(2f))
                .smoothness(1.15f)
                .build()).render(matrix, barX, barY);
        }

        if (isEditMode()) {
            renderEditDots(context, x, y, w, h);
        }
    }

    private Entity getTargetEntity() {
        if (mc.targetedEntity instanceof LivingEntity le && le.isAlive() && le != mc.player) {
            return le;
        }
        return null;
    }

    private boolean isEditMode() {
        return mc.currentScreen instanceof net.minecraft.client.gui.screen.ChatScreen
            || mc.currentScreen instanceof dev.meoray.client.gui.screen.MeoRayClickGUI;
    }

    private void renderEditDots(DrawContext context, float x, float y, float w, float h) {
        Matrix4f matrix = context.getMatrices().peek().getPositionMatrix();
        int dotSpacing = 5;
        int dotSize = 2;
        int dotColor = 0x99FFFFFF;
        float perimeter = 2 * (w + h);
        int dots = (int) (perimeter / dotSpacing);
        for (int i = 0; i < dots; i++) {
            float dist = i * dotSpacing;
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

    private int alphaBlend(int col, float alpha) {
        if (alpha >= 1) return col;
        int a = (int) (((col >> 24) & 0xFF) * alpha);
        return (col & 0x00FFFFFF) | (a << 24);
    }
}
