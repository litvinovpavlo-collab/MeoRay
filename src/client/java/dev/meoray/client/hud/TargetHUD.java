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
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.util.SkinTextures;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.SpawnEggItem;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import org.joml.Matrix4f;

public class TargetHUD extends HudElement {

    private static final Draggable draggable = new Draggable("TargetHUD", 10, 100, 140, 36);

    private float animAlpha = 0f;
    private float animHealth = 0f;
    private LivingEntity lastTarget = null;

    public TargetHUD() {
        super(draggable, "TargetHUD");
    }

    @Override
    public void render(DrawContext context) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;

        boolean editable = mc.currentScreen instanceof ChatScreen
                || mc.currentScreen instanceof MeoRayClickGUI;

        LivingEntity target = getTarget(mc);

        float targetAlpha = (target != null || editable) ? 1f : 0f;
        animAlpha = MathHelper.lerp(0.12f, animAlpha, targetAlpha);
        if (animAlpha < 0.01f && target == null && !editable) return;

        if (target != null) lastTarget = target;
        LivingEntity displayTarget = target != null ? target : lastTarget;

        Theme theme = MeoRayClient.INSTANCE.getThemeManager().getRenderTheme();
        MsdfFont medium = FontManager.SUISSEINTMEDIUM.get();

        float W = 140f;
        float H = 36f;
        float avatarSize = 24f;
        float padX = 5f;
        float padY = 5f;

        draggable.setWidth(W);
        draggable.setHeight(H);

        float x = draggable.getX();
        float y = draggable.getY();

        Matrix4f matrix = context.getMatrices().peek().getPositionMatrix();

        int accent = theme.accent();
        int accentRGB = accent & 0x00FFFFFF;

        // GLOW
        int glowAlpha = (int)(0xC0 * animAlpha);
        int glowCol = accentRGB | (glowAlpha << 24);
        GlowRenderer.drawGlow(matrix, x, y, W, H, glowCol, 14f, 4.5f);

        // BG
        int bgAlpha = (int)(0xE6 * animAlpha);
        int bg = (0x121216) | (bgAlpha << 24);
        ((BuiltRectangle) Builder.rectangle()
                .size(new SizeState(W, H))
                .color(new QuadColorState(bg))
                .radius(new QuadRadiusState(4.5))
                .smoothness(1.15F)
                .build()).render(matrix, x, y);

        // Акцент-полоса
        int barAlpha = (int)(0xFF * animAlpha);
        int barCol = accentRGB | (barAlpha << 24);
        ((BuiltRectangle) Builder.rectangle()
                .size(new SizeState(2f, H - 10f))
                .color(new QuadColorState(barCol))
                .radius(new QuadRadiusState(1.5))
                .smoothness(1.15F)
                .build()).render(matrix, x + 3.5f, y + 5f);

        // Аватар фон
        float avatarX = x + padX + 5f;
        float avatarY = y + (H - avatarSize) / 2f;

        int avatarBgAlpha = (int)(0x30 * animAlpha);
        int avatarBg = accentRGB | (avatarBgAlpha << 24);
        ((BuiltRectangle) Builder.rectangle()
                .size(new SizeState(avatarSize, avatarSize))
                .color(new QuadColorState(avatarBg))
                .radius(new QuadRadiusState(4.0))
                .smoothness(1.15F)
                .build()).render(matrix, avatarX, avatarY);

        if (displayTarget != null && animAlpha > 0.3f) {
            context.draw();
            drawEntityHead(context, displayTarget, avatarX, avatarY, avatarSize, animAlpha);
            context.draw();
        }

        // Рамка
        int borderAlpha = (int)(0x70 * animAlpha);
        int borderCol = accentRGB | (borderAlpha << 24);
        drawBorder(matrix, avatarX, avatarY, avatarSize, avatarSize, 1f, borderCol);

        // ТЕКСТ
        float infoX = avatarX + avatarSize + 6f;

        String name = displayTarget != null ? displayTarget.getName().getString() : "Unknown";
        float nameSize = 5.5f;
        int nameAlpha = (int)(0xFF * animAlpha);
        int nameCol = 0x00CCCCCC | (nameAlpha << 24);
        ((BuiltText) Builder.text()
                .font(medium).text(name)
                .color(nameCol).size(nameSize).thickness(0.04F)
                .build()).render(matrix, infoX, y + padY - 1f);

        boolean isPlayer = displayTarget instanceof PlayerEntity;
        String typeLabel = isPlayer ? "Player" : (displayTarget != null ? "Entity" : "---");
        float typeSize = 4.0f;
        int typeAlpha = (int)(0xFF * animAlpha);
        int typeCol = accentRGB | (typeAlpha << 24);
        ((BuiltText) Builder.text()
                .font(medium).text(typeLabel)
                .color(typeCol).size(typeSize).thickness(0.04F)
                .build()).render(matrix, infoX, y + padY + nameSize + 0.5f);

        String distLabel = "";
        if (displayTarget != null) {
            double dist = mc.player.distanceTo(displayTarget);
            distLabel = String.format("%.1fm", dist);
        }
        float distSize = 4.0f;
        int distAlpha = (int)(0xAA * animAlpha);
        int distCol = 0x00888896 | (distAlpha << 24);
        float distW = medium.getWidth(distLabel, distSize);
        ((BuiltText) Builder.text()
                .font(medium).text(distLabel)
                .color(distCol).size(distSize).thickness(0.04F)
                .build()).render(matrix, x + W - padX - distW, y + padY - 0.5f);

        float hpLabelSize = 4.0f;
        float currentHp = displayTarget != null ? displayTarget.getHealth() : 0f;
        float maxHp = displayTarget != null ? displayTarget.getMaxHealth() : 20f;

        float targetHpRatio = maxHp > 0 ? currentHp / maxHp : 0f;
        animHealth = MathHelper.lerp(0.1f, animHealth, targetHpRatio);

        String hpText = displayTarget != null
                ? String.format("%.1f / %.0f", currentHp, maxHp)
                : "0 / 20";

        int hpTextAlpha = (int)(0xCC * animAlpha);
        int hpTextCol = 0x00CCCCCC | (hpTextAlpha << 24);
        float hpLabelW = medium.getWidth(hpText, hpLabelSize);
        ((BuiltText) Builder.text()
                .font(medium).text(hpText)
                .color(hpTextCol).size(hpLabelSize).thickness(0.04F)
                .build()).render(matrix, x + W - padX - hpLabelW, y + padY + nameSize + 0.5f);

        // HP bar
        float barY = y + H - 7f;
        float barX = infoX;
        float barW = W - (infoX - x) - padX;
        float barH = 2.5f;

        int barBgAlpha = (int)(0x40 * animAlpha);
        ((BuiltRectangle) Builder.rectangle()
                .size(new SizeState(barW, barH))
                .color(new QuadColorState(0x00FFFFFF | (barBgAlpha << 24)))
                .radius(new QuadRadiusState(1.5))
                .smoothness(1.15F)
                .build()).render(matrix, barX, barY);

        float hp = animHealth;
        int hpR, hpG, hpB;
        if (hp > 0.5f) {
            float t = (hp - 0.5f) * 2f;
            hpR = (int)(255 * (1f - t));
            hpG = 220;
            hpB = 80;
        } else {
            float t = hp * 2f;
            hpR = 220;
            hpG = (int)(220 * t);
            hpB = 40;
        }
        int hpBarAlpha = (int)(0xEE * animAlpha);
        int hpBarCol = (hpBarAlpha << 24) | (hpR << 16) | (hpG << 8) | hpB;

        float filledW = barW * MathHelper.clamp(animHealth, 0f, 1f);
        if (filledW > 1f) {
            ((BuiltRectangle) Builder.rectangle()
                    .size(new SizeState(filledW, barH))
                    .color(new QuadColorState(hpBarCol))
                    .radius(new QuadRadiusState(1.5))
                    .smoothness(1.15F)
                    .build()).render(matrix, barX, barY);
        }

        if (target == null && animAlpha < 0.05f) {
            lastTarget = null;
        }

        if (editable) drawEditDots(matrix, x, y, W, H);
    }

    /** Рисует голову игрока (целиком на аватаре) или spawn egg моба */
    private void drawEntityHead(DrawContext context, LivingEntity entity,
                                 float x, float y, float size, float alpha) {
        // === ИГРОК — голова со скина, на весь аватар ===
        if (entity instanceof AbstractClientPlayerEntity player) {
            Identifier skin = getSkinTexture(player);
            if (skin != null) {
                int ix = (int) Math.round(x);
                int iy = (int) Math.round(y);
                int is = (int) Math.round(size);

                int alphaInt = (int) (255 * alpha);
                int argb = (alphaInt << 24) | 0x00FFFFFF;

                // Базовый слой (8,8) 8x8
                context.drawTexture(
                        RenderLayer::getGuiTextured,
                        skin,
                        ix, iy,
                        8f, 8f,
                        is, is,
                        8, 8,
                        64, 64,
                        argb
                );
                // Шапка (40,8) 8x8
                context.drawTexture(
                        RenderLayer::getGuiTextured,
                        skin,
                        ix, iy,
                        40f, 8f,
                        is, is,
                        8, 8,
                        64, 64,
                        argb
                );
                return;
            }
        }

        // === МОБЫ — spawn egg как иконка ===
        ItemStack egg = getSpawnEgg(entity);
        if (egg != null && !egg.isEmpty()) {
            // Spawn egg рендерится в 16x16, масштабируем до размера аватара
            float pad = size * 0.10f;
            float ix = x + pad;
            float iy = y + pad;
            float is = size - pad * 2;

            MatrixStack matrices = context.getMatrices();
            matrices.push();
            matrices.translate(ix, iy, 0);
            float scl = is / 16f;
            matrices.scale(scl, scl, 1f);
            context.drawItem(egg, 0, 0);
            matrices.pop();
            return;
        }

        // Фолбэк — буква
        drawLetterFallback(context, entity, x, y, size, alpha);
    }

    /** Получаем spawn egg для типа entity */
    private ItemStack getSpawnEgg(LivingEntity entity) {
        try {
            // Прямой API: SpawnEggItem.forEntity()
            SpawnEggItem egg = SpawnEggItem.forEntity(entity.getType());
            if (egg != null) {
                return new ItemStack(egg);
            }
        } catch (Throwable ignored) {}

        // Fallback: ищем по id <entity>_spawn_egg
        try {
            Identifier typeId = Registries.ENTITY_TYPE.getId(entity.getType());
            if (typeId != null) {
                Identifier eggId = Identifier.of(typeId.getNamespace(), typeId.getPath() + "_spawn_egg");
                var item = Registries.ITEM.get(eggId);
                if (item != Items.AIR) {
                    return new ItemStack(item);
                }
            }
        } catch (Throwable ignored) {}

        return null;
    }

    private Identifier getSkinTexture(AbstractClientPlayerEntity player) {
        try {
            SkinTextures skin = player.getSkinTextures();
            return skin != null ? skin.texture() : null;
        } catch (Throwable t) {
            return null;
        }
    }

    private void drawLetterFallback(DrawContext context, LivingEntity entity,
                                     float x, float y, float size, float alpha) {
        MsdfFont medium = FontManager.SUISSEINTMEDIUM.get();
        Theme theme = MeoRayClient.INSTANCE.getThemeManager().getRenderTheme();
        int accentRGB = theme.accent() & 0x00FFFFFF;

        Matrix4f matrix = context.getMatrices().peek().getPositionMatrix();
        String letter = entity.getName().getString().substring(0, 1).toUpperCase();
        float letterSize = size * 0.5f;
        float letterW = medium.getWidth(letter, letterSize);
        int letterAlpha = (int)(0xFF * alpha);
        int letterCol = accentRGB | (letterAlpha << 24);

        ((BuiltText) Builder.text()
                .font(medium).text(letter)
                .color(letterCol).size(letterSize).thickness(0.03F)
                .build()).render(matrix,
                x + (size - letterW) / 2f,
                y + (size - letterSize) / 2f - 1f);
    }

    private void drawBorder(Matrix4f matrix, float x, float y, float w, float h, float t, int color) {
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

    private LivingEntity getTarget(MinecraftClient mc) {
        if (mc.targetedEntity instanceof LivingEntity le && le.isAlive()) {
            return le;
        }
        return null;
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
