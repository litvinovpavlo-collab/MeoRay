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
import net.minecraft.client.network.PlayerListEntry;
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

    private static final Identifier GUI_ATLAS = Identifier.of("minecraft", "textures/gui/sprites/container/villager/experience_notch.png");

    private final MinecraftClient mc = MinecraftClient.getInstance();
    private Entity currentTarget = null;
    private float animProgress = 0f;

    private static final Draggable DRAGGABLE = new Draggable("target", 2, 120, 130, 45) {
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
        }

        if (target == null) {
            animProgress = Math.max(0, animProgress - 0.05f);
            if (animProgress <= 0) return;
        } else {
            animProgress = Math.min(1, animProgress + 0.05f);
        }

        Matrix4f matrix = context.getMatrices().peek().getPositionMatrix();
        MsdfFont medium = FontManager.SUISSEINTMEDIUM.get();

        float x = DRAGGABLE.getX();
        float y = DRAGGABLE.getY();
        float w = 130;
        float h = 45;
        DRAGGABLE.setWidth(w);
        DRAGGABLE.setHeight(h);

        int bg = 0xCC111116;
        float a = animProgress;
        ((BuiltRectangle) Builder.rectangle()
            .size(new SizeState(w, h))
            .color(new QuadColorState(alphaBlend(bg, a)))
            .radius(new QuadRadiusState(6f))
            .smoothness(1.15f)
            .build()).render(matrix, x, y);

        ((BuiltRectangle) Builder.rectangle()
            .size(new SizeState(w, 1.5f))
            .color(new QuadColorState(alphaBlend(0xFFAB47BC, a)))
            .radius(new QuadRadiusState(0f))
            .smoothness(1.0f)
            .build()).render(matrix, x, y + h - 1.5f);

        if (!(target instanceof LivingEntity living)) return;

        String name = "Unknown";
        float health = 0;
        float maxHealth = 0;
        String type = "Entity";

        if (living instanceof PlayerEntity p) {
            name = p.getName().getString();
            type = "Player";
        } else if (living instanceof HostileEntity || living instanceof Monster) {
            name = living.getName().getString();
            type = "Hostile";
        } else if (living instanceof AnimalEntity) {
            name = living.getName().getString();
            type = "Animal";
        } else if (living instanceof VillagerEntity) {
            name = living.getName().getString();
            type = "Villager";
        }

        if (living instanceof PlayerEntity p) {
            health = p.getHealth();
            maxHealth = p.getMaxHealth();
        } else {
            health = living.getHealth();
            maxHealth = living.getMaxHealth();
        }

        int headSize = 26;
        float headX = x + 6;
        float headY = y + (h - headSize) / 2f;

        ((BuiltRectangle) Builder.rectangle()
            .size(new SizeState(headSize, headSize))
            .color(new QuadColorState(alphaBlend(0xFF2A2A30, a)))
            .radius(new QuadRadiusState(4f))
            .smoothness(1.15f)
            .build()).render(matrix, headX, headY);

        if (living instanceof AbstractClientPlayerEntity player) {
            Identifier skinId = player.getSkinTextures().texture();
            RenderSystem.setShaderTexture(0, skinId);
            context.drawTexture(RenderLayer::getGuiTextured, skinId, (int) headX, (int) headY, 8, 8, 8, 8, 64, 64);
        }

        float textX = headX + headSize + 8;
        float nameY = y + 8;
        float infoY = y + 10 + 6;

        int nameCol = alphaBlend(Color.WHITE.getRGB(), a);
        int infoCol = alphaBlend(0xFFAAAAAA, a);

        ((BuiltText) Builder.text().font(medium).text(name)
            .color(nameCol).size(6f).thickness(0.04f).build())
            .render(matrix, textX, nameY);

        float dist = mc.player.distanceTo(living);
        String info = type + " | " + String.format("%.1f", dist) + "m";
        ((BuiltText) Builder.text().font(medium).text(info)
            .color(infoCol).size(4.5f).thickness(0.04f).build())
            .render(matrix, textX, infoY);

        float hpStrX = x + w - 8;
        String hpStr = String.format("%.0f/%.0f", health, maxHealth);
        float hpStrW = medium.getWidth(hpStr, 5f);
        ((BuiltText) Builder.text().font(medium).text(hpStr)
            .color(nameCol).size(5f).thickness(0.04f).build())
            .render(matrix, hpStrX - hpStrW, y + 8);

        float barY = y + h - 8;
        float barX = textX;
        float barW = w - (textX - x) - 8;
        float barH = 3;

        float healthPct = maxHealth > 0 ? MathHelper.clamp(health / maxHealth, 0, 1) : 0;

        ((BuiltRectangle) Builder.rectangle()
            .size(new SizeState(barW, barH))
            .color(new QuadColorState(alphaBlend(0xFF333333, a)))
            .radius(new QuadRadiusState(1.5f))
            .smoothness(1.15f)
            .build()).render(matrix, barX, barY);

        int healthColor;
        if (healthPct > 0.6f) healthColor = 0xFF44FF66;
        else if (healthPct > 0.3f) healthColor = 0xFFFFAA22;
        else healthColor = 0xFFFF4444;

        if (healthPct > 0.01f) {
            ((BuiltRectangle) Builder.rectangle()
                .size(new SizeState(barW * healthPct, barH))
                .color(new QuadColorState(alphaBlend(healthColor, a)))
                .radius(new QuadRadiusState(1.5f))
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
