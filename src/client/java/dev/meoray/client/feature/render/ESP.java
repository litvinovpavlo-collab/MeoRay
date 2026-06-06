package dev.meoray.client.feature.render;

import dev.meoray.client.MeoRayClient;
import dev.meoray.client.core.Category;
import dev.meoray.client.core.Module;
import dev.meoray.client.core.setting.ColorSetting;
import dev.meoray.client.core.setting.GroupSetting;
import dev.meoray.client.core.setting.ModeSetting;
import dev.meoray.client.core.setting.NumberSetting;
import dev.meoray.client.managers.FontManager;
import dev.meoray.client.utils.render.Render2DUtils;
import dev.meoray.client.utils.render.Render3DUtils;
import dev.meoray.client.util.render.builders.Builder;
import dev.meoray.client.util.render.builders.states.QuadColorState;
import dev.meoray.client.util.render.builders.states.QuadRadiusState;
import dev.meoray.client.util.render.builders.states.SizeState;
import dev.meoray.client.util.render.msdf.MsdfFont;
import dev.meoray.client.util.render.renderers.impl.BuiltRectangle;
import dev.meoray.client.util.render.renderers.impl.BuiltText;
import org.joml.Matrix4f;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.Box;

import java.awt.Color;
import java.util.List;

public class ESP extends Module {

    private final ModeSetting dimension = new ModeSetting("Dimension", "2D", "2D", "3D");
    private final ModeSetting style2D = new ModeSetting("2D Style", "WexSide", "Corner", "Box", "WexSide", "Outline");
    private final ModeSetting style3D = new ModeSetting("3D Style", "Outline", "Outline", "Filled", "Both");

    // === Targets (group) ===
    private final GroupSetting targets = new GroupSetting("Targets", "Кого подсвечивать")
            .add("Players", true)
            .add("Mobs", true)
            .add("Animals", false)
            .add("Invisibles", true);

    // === Visuals (group) ===
    private final GroupSetting visuals = new GroupSetting("Visuals", "Что рисовать на боксе")
            .add("Health Bar", true)
            .add("Name Tag", true)
            .add("Armor", true)
            .add("Glow", true)
            .add("Filled", true)
            .add("Distance", true)
            .add("Tracers", false);

    private final NumberSetting cornerLen = new NumberSetting("Corner Length", 6.0, 2.0, 20.0, 1.0);
    private final NumberSetting lineWidth = new NumberSetting("Line Width", 1.0, 0.5, 4.0, 0.5);
    private final NumberSetting fillAlpha = new NumberSetting("Fill Alpha", 30.0, 0.0, 200.0, 5.0);

    private final ColorSetting boxColor = new ColorSetting("Box Color", new Color(180, 80, 255));
    private final ColorSetting outlineColor = new ColorSetting("Outline Color", new Color(0, 0, 0, 180));
    private final ColorSetting tracerColor = new ColorSetting("Tracer Color", new Color(180, 80, 255));
    private final ColorSetting friendColor = new ColorSetting("Friend Color", new Color(0, 200, 80));

    public ESP() {
        super("ESP", "Highlights entities with 2D or 3D boxes", Category.RENDER);

        style2D.visibleWhen(() -> dimension.getValue().equals("2D"));
        style3D.visibleWhen(() -> dimension.getValue().equals("3D"));
        cornerLen.visibleWhen(() -> dimension.getValue().equals("2D"));

        addSettings(
                dimension, style2D, style3D,
                targets, visuals,
                cornerLen, lineWidth, fillAlpha,
                boxColor, outlineColor, tracerColor, friendColor
        );
    }

    public void onRender2D(DrawContext ctx, float tickDelta) {
        if (!dimension.getValue().equals("2D")) return;
        if (mc.world == null || mc.player == null) return;

        for (Entity entity : mc.world.getEntities()) {
            if (entity == mc.player) continue;
            if (!isValid(entity)) continue;

            LivingEntity living = (LivingEntity) entity;
            double[] s = Render2DUtils.getEntityScreenPos(entity, tickDelta);
            if (s == null) continue;

            double x = Math.min(s[0], s[2]);
            double y = Math.min(s[1], s[3]);
            double x2 = Math.max(s[0], s[2]);
            double y2 = Math.max(s[1], s[3]);

            double w = x2 - x;
            double h = y2 - y;
            if (w < 1 || h < 1) continue;

            int color = getEntityColor(living);
            int outline = outlineColor.getRGB();

            if (visuals.get("Glow")) {
                drawGlow(ctx, x, y, w, h, color, 4);
            }

            if (visuals.get("Filled")) {
                int fillCol = (color & 0x00FFFFFF) | (fillAlpha.getValue().intValue() << 24);
                Render2DUtils.drawRect(ctx, x, y, w, h, fillCol);
            }

            switch (style2D.getValue()) {
                case "Corner" -> drawCleanCorners(ctx, x, y, w, h, color, outline);
                case "Box" -> drawCleanBox(ctx, x, y, w, h, color, outline);
                case "WexSide" -> drawWexSide(ctx, x, y, w, h, color, outline);
                case "Outline" -> drawOutlineOnly(ctx, x, y, w, h, color);
            }

            if (visuals.get("Health Bar")) {
                drawCleanHealthBar(ctx, x, y, h, living.getHealth(), living.getMaxHealth());
            }

            if (visuals.get("Name Tag")) {
                boolean friend = living instanceof PlayerEntity && MeoRayClient.INSTANCE.getFriendManager()
                    .isFriend(((PlayerEntity) living).getGameProfile().getName());
                drawNameTag(ctx, living, x, y, w, friend);
            }

            if (visuals.get("Distance")) {
                drawDistance(ctx, living, x, y2, w);
            }

            if (visuals.get("Armor") && living instanceof PlayerEntity player) {
                drawArmor(ctx, player, x, y2, w);
            }
        }
    }

    public void onWorldRender(MatrixStack matrices, float tickDelta) {
        if (!dimension.getValue().equals("3D")) return;
        if (mc.world == null || mc.player == null) return;

        for (Entity entity : mc.world.getEntities()) {
            if (entity == mc.player) continue;
            if (!isValid(entity)) continue;

            LivingEntity living = (LivingEntity) entity;
            Box box = Render3DUtils.getInterpolatedBox(entity, tickDelta);
            int color = getEntityColor(living);
            String mode = style3D.getValue();

            if (mode.equals("Filled") || mode.equals("Both")) {
                int fillCol = (color & 0x00FFFFFF) | (fillAlpha.getValue().intValue() << 24);
                Render3DUtils.drawBoxFilled(matrices, box, fillCol);
            }
            if (mode.equals("Outline") || mode.equals("Both")) {
                Render3DUtils.drawBoxOutline(matrices, box, color, lineWidth.getValue().floatValue() * 1.5f);
            }

            if (visuals.get("Tracers")) {
                int tc = tracerColor.getRGB() | 0xFF000000;
                Render3DUtils.drawTracer(matrices,
                        box.getCenter(),
                        tc, lineWidth.getValue().floatValue());
            }
        }
    }

    private void drawOutlineOnly(DrawContext ctx, double x, double y, double w, double h, int color) {
        double t = lineWidth.getValue();
        Render2DUtils.drawOutline(ctx, x - 0.5, y - 0.5, w + 1, h + 1, t + 0.5, 0xAA000000);
        Render2DUtils.drawOutline(ctx, x, y, w, h, t, color);
    }

    private void drawCleanBox(DrawContext ctx, double x, double y, double w, double h, int color, int outline) {
        double t = lineWidth.getValue();
        Render2DUtils.drawOutline(ctx, x - 0.5, y - 0.5, w + 1, h + 1, t + 0.5, outline);
        Render2DUtils.drawOutline(ctx, x, y, w, h, t, color);
    }

    private void drawCleanCorners(DrawContext ctx, double x, double y, double w, double h, int color, int outline) {
        double t = lineWidth.getValue();
        double cl = Math.min(cornerLen.getValue(), Math.min(w, h) / 3.0);

        drawCornerSet(ctx, x - 0.5, y - 0.5, w + 1, h + 1, cl + 0.5, t + 0.5, outline);
        drawCornerSet(ctx, x, y, w, h, cl, t, color);
    }

    private void drawWexSide(DrawContext ctx, double x, double y, double w, double h, int color, int outline) {
        double t = lineWidth.getValue();
        double cl = Math.min(cornerLen.getValue(), Math.min(w, h) / 2.5);

        Render2DUtils.drawOutline(ctx, x - 0.5, y - 0.5, w + 1, h + 1, 0.5, 0xC0000000);

        drawCornerSet(ctx, x - 0.5, y - 0.5, w + 1, h + 1, cl + 0.5, t + 1, 0xC0000000);

        drawCornerSet(ctx, x, y, w, h, cl, t, color);
    }

    private void drawCornerSet(DrawContext ctx, double x, double y, double w, double h,
                                double cl, double t, int color) {
        Render2DUtils.drawRect(ctx, x, y, cl, t, color);
        Render2DUtils.drawRect(ctx, x, y, t, cl, color);
        Render2DUtils.drawRect(ctx, x + w - cl, y, cl, t, color);
        Render2DUtils.drawRect(ctx, x + w - t, y, t, cl, color);
        Render2DUtils.drawRect(ctx, x, y + h - t, cl, t, color);
        Render2DUtils.drawRect(ctx, x, y + h - cl, t, cl, color);
        Render2DUtils.drawRect(ctx, x + w - cl, y + h - t, cl, t, color);
        Render2DUtils.drawRect(ctx, x + w - t, y + h - cl, t, cl, color);
    }

    private void drawCleanHealthBar(DrawContext ctx, double x, double y, double h,
                                     float health, float maxHealth) {
        double barW = 2.5;
        double barX = x - barW - 2;
        double barY = y;

        Render2DUtils.drawRect(ctx, barX - 0.5, barY - 0.5, barW + 1, h + 1, 0xFF000000);
        Render2DUtils.drawRect(ctx, barX, barY, barW, h, 0xFF1A1A1A);

        double percent = Math.min(1.0, Math.max(0.0, health / maxHealth));
        double fillH = h * percent;
        int hpColor = getHealthColor(percent);

        Render2DUtils.drawRect(ctx, barX, barY + h - fillH, barW, fillH, hpColor);
    }

    private int getHealthColor(double percent) {
        if (percent > 0.6) {
            return 0xFF55DD55;
        } else if (percent > 0.3) {
            return 0xFFFFAA00;
        } else {
            return 0xFFFF3333;
        }
    }

    private void drawGlow(DrawContext ctx, double x, double y, double w, double h, int color, int size) {
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;

        for (int i = size; i >= 1; i--) {
            float ratio = (float) i / size;
            int alpha = (int) (50 * (1.0f - ratio));
            int glowColor = (alpha << 24) | (r << 16) | (g << 8) | b;
            Render2DUtils.drawOutline(ctx, x - i, y - i, w + i * 2, h + i * 2, 1, glowColor);
        }
    }

    private void drawNameTag(DrawContext ctx, LivingEntity entity, double x, double y, double w, boolean friend) {
        MsdfFont font = FontManager.SUISSEINTMEDIUM.get();
        float textSize = 6.5f;

        String typeLetter;
        int typeCol;
        if (friend) {
            typeLetter = "F";
            typeCol = friendColor.getRGB() | 0xFF000000;
        } else if (entity instanceof PlayerEntity) {
            typeLetter = "P";
            typeCol = 0xFFFF4444;
        } else if (entity instanceof net.minecraft.entity.mob.HostileEntity) {
            typeLetter = "M";
            typeCol = 0xFFFFAA22;
        } else if (entity instanceof net.minecraft.entity.passive.AnimalEntity) {
            typeLetter = "A";
            typeCol = 0xFF99DDFF;
        } else {
            typeLetter = null;
            typeCol = 0;
        }

        String name = entity.getName().getString();
        String hpStr = String.format("%.0f", entity.getHealth());

        float letterW = typeLetter != null ? font.getWidth(typeLetter, textSize) : 0;
        float gapAfterLetter = typeLetter != null ? 4f : 0f;
        float nameW = font.getWidth(name, textSize);
        float spaceW = font.getWidth(" ", textSize);
        float hpW = font.getWidth(hpStr, textSize);
        float heartW = font.getWidth("HP", textSize);

        float totalW = letterW + gapAfterLetter + nameW + spaceW + hpW + 2 + heartW;
        float padX = 5f;
        float padY = 3f;
        float bgW = totalW + padX * 2;
        float bgH = textSize + padY * 2;

        float tx = (float) (x + w / 2.0 - bgW / 2.0);
        float ty = (float) (y - bgH - 2);

        Matrix4f matrix = ctx.getMatrices().peek().getPositionMatrix();

        int bgCol = friend ? 0xCC0A2010 : 0xCC000000;
        ((BuiltRectangle) Builder.rectangle()
            .size(new SizeState(bgW, bgH))
            .color(new QuadColorState(bgCol))
            .radius(new QuadRadiusState(2.5))
            .smoothness(1.15F)
            .build()).render(matrix, tx, ty);

        int topLine = friend ? (friendColor.getRGB() | 0xFF000000) : 0x40FFFFFF;
        ((BuiltRectangle) Builder.rectangle()
            .size(new SizeState(bgW, 0.7f))
            .color(new QuadColorState(topLine))
            .radius(new QuadRadiusState(0.0))
            .smoothness(1.15F)
            .build()).render(matrix, tx, ty);

        float contentX = tx + padX;
        float contentY = ty + padY;

        if (typeLetter != null) {
            ((BuiltText) Builder.text()
                .font(font).text(typeLetter)
                .color(typeCol).size(textSize).thickness(0.06F)
                .build()).render(matrix, contentX, contentY);
            contentX += letterW + gapAfterLetter;
        }

        int nameCol = friend ? (friendColor.getRGB() | 0xFF000000) : 0xFFFFFFFF;
        ((BuiltText) Builder.text()
            .font(font).text(name)
            .color(nameCol).size(textSize).thickness(0.04F)
            .build()).render(matrix, contentX, contentY);

        float hpPercent = entity.getHealth() / entity.getMaxHealth();
        int hpColor = getHealthColor(hpPercent);
        float hpX = contentX + nameW + spaceW;
        ((BuiltText) Builder.text()
            .font(font).text(hpStr)
            .color(hpColor).size(textSize).thickness(0.04F)
            .build()).render(matrix, hpX, contentY);

        float heartX = hpX + hpW + 1;
        ((BuiltText) Builder.text()
            .font(font).text("HP")
            .color(hpColor).size(textSize).thickness(0.04F)
            .build()).render(matrix, heartX, contentY);
    }

    private void drawDistance(DrawContext ctx, LivingEntity entity, double x, double y2, double w) {
        MsdfFont font = FontManager.SUISSEINTMEDIUM.get();
        float textSize = 5.5f;

        float dist = mc.player.distanceTo(entity);
        String text = String.format("%.1fm", dist);

        float textW = font.getWidth(text, textSize);
        float padX = 4f;
        float padY = 2.5f;
        float bgW = textW + padX * 2;
        float bgH = textSize + padY * 2;

        float tx = (float) (x + w / 2.0 - bgW / 2.0);
        float ty = (float) (y2 + 3);

        Matrix4f matrix = ctx.getMatrices().peek().getPositionMatrix();

        ((BuiltRectangle) Builder.rectangle()
            .size(new SizeState(bgW, bgH))
            .color(new QuadColorState(0xC0000000))
            .radius(new QuadRadiusState(2.0))
            .smoothness(1.15F)
            .build()).render(matrix, tx, ty);

        ((BuiltText) Builder.text()
            .font(font).text(text)
            .color(0xFFAACCFF).size(textSize).thickness(0.04F)
            .build()).render(matrix, tx + padX, ty + padY);
    }

    private void drawArmor(DrawContext ctx, PlayerEntity player, double x, double y2, double w) {
        List<ItemStack> items = new java.util.ArrayList<>();

        try {
            ItemStack main = player.getMainHandStack();
            if (main != null && !main.isEmpty()) items.add(main);

            for (ItemStack s : player.getArmorItems()) {
                if (s != null && !s.isEmpty()) items.add(s);
            }

            ItemStack off = player.getOffHandStack();
            if (off != null && !off.isEmpty()) items.add(off);
        } catch (Throwable ignored) {
            return;
        }

        if (items.isEmpty()) return;

        int totalW = items.size() * 16;
        float startX = (float) (x + w / 2 - totalW / 2.0);
        float ay = (float) (y2 + (visuals.get("Distance") ? 16 : 3));

        for (int i = 0; i < items.size(); i++) {
            ctx.drawItem(items.get(i), (int) (startX + i * 16), (int) ay);
            ctx.drawStackOverlay(mc.textRenderer, items.get(i), (int) (startX + i * 16), (int) ay);
        }
    }

    private int getEntityColor(LivingEntity entity) {
        int base = boxColor.getRGB() | 0xFF000000;

        if (entity instanceof PlayerEntity) {
            if (entity.hurtTime > 0) return 0xFFFF3333;
            if (entity.isInvisible()) return 0xFFFFFF55;
            return base;
        }
        if (entity instanceof HostileEntity) {
            return 0xFFFF5555;
        }
        return 0xFF55DD55;
    }

    private boolean isValid(Entity entity) {
        if (!(entity instanceof LivingEntity living)) return false;
        if (!living.isAlive()) return false;
        if (entity == mc.player) return false;
        if (dev.meoray.client.feature.misc.FakePlayer.isFake(entity)) return false;
        if (living.isInvisible() && !targets.get("Invisibles")) return false;

        if (entity instanceof PlayerEntity) return targets.get("Players");
        if (entity instanceof net.minecraft.entity.passive.AnimalEntity) return targets.get("Animals");
        return targets.get("Mobs");
    }
}
