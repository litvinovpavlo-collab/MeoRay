package dev.meoray.client.feature.render;

import dev.meoray.client.core.Category;
import dev.meoray.client.core.Module;
import dev.meoray.client.core.setting.BooleanSetting;
import dev.meoray.client.core.setting.ColorSetting;
import dev.meoray.client.core.setting.ModeSetting;
import dev.meoray.client.core.setting.NumberSetting;
import dev.meoray.client.utils.render.Render2DUtils;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;

import java.awt.Color;
import java.util.List;

public class ESP extends Module {

    private final ModeSetting mode = new ModeSetting("Mode", "Corner", "Corner", "Box", "WexSide");
    private final BooleanSetting players = new BooleanSetting("Players", true);
    private final BooleanSetting mobs = new BooleanSetting("Mobs", true);
    private final BooleanSetting invisibles = new BooleanSetting("Invisibles", true);

    private final BooleanSetting healthBar = new BooleanSetting("Health Bar", true);
    private final BooleanSetting nameTag = new BooleanSetting("Name Tag", true);
    private final BooleanSetting armorDisplay = new BooleanSetting("Armor", true);
    private final BooleanSetting shadow = new BooleanSetting("Shadow", true);
    private final BooleanSetting filledBox = new BooleanSetting("Filled", true);

    private final NumberSetting cornerLength = new NumberSetting("Corner Length", 8.0, 3.0, 20.0, 1.0);
    private final NumberSetting lineWidth = new NumberSetting("Line Width", 1.5, 0.5, 5.0, 0.5);
    private final NumberSetting shadowSize = new NumberSetting("Shadow Size", 5.0, 1.0, 15.0, 1.0);

    private final ColorSetting boxColor = new ColorSetting("Box Color", new Color(180, 60, 255));
    private final ColorSetting filledColor = new ColorSetting("Fill Color", new Color(180, 60, 255, 35));
    private final ColorSetting outlineColor = new ColorSetting("Outline Color", new Color(0, 0, 0, 180));

    public ESP() {
        super("ESP", "Draws player boxes", Category.RENDER);
        addSettings(mode, players, mobs, invisibles,
                healthBar, nameTag, armorDisplay, shadow, filledBox,
                cornerLength, lineWidth, shadowSize,
                boxColor, filledColor, outlineColor);
    }

    public void onRender2D(DrawContext ctx, float tickDelta) {
        if (mc.world == null || mc.player == null) return;

        for (Entity entity : mc.world.getEntities()) {
            if (entity == mc.player) continue;
            if (!isValid(entity)) continue;

            LivingEntity living = (LivingEntity) entity;
            double[] screenPos = Render2DUtils.getEntityScreenPos(entity, tickDelta);
            if (screenPos == null) continue;

            double x = Math.min(screenPos[0], screenPos[2]);
            double y = Math.min(screenPos[1], screenPos[3]);
            double x2 = Math.max(screenPos[0], screenPos[2]);
            double y2 = Math.max(screenPos[1], screenPos[3]);

            double w = x2 - x;
            double h = y2 - y;

            System.out.println(entity.getName().getString() + " w=" + w + " h=" + h + " x=" + x + " y=" + y);

            if (w < 1 || h < 1) continue;

            int color = getEntityColor(living);
            int fillColor = filledColor.getRGB();
            int outline = outlineColor.getRGB();

            if (shadow.getValue()) {
                Render2DUtils.drawShadowBox(ctx, x, y, w, h,
                        shadowSize.getValue().intValue(), color);
            }

            if (filledBox.getValue()) {
                Render2DUtils.drawRect(ctx, x, y, w, h, fillColor);
            }

            switch (mode.getValue()) {
                case "Corner" -> {
                    Render2DUtils.drawCornerBoxOutlined(ctx, x, y, w, h,
                            cornerLength.getValue(), lineWidth.getValue(), color, outline);
                }
                case "Box" -> {
                    Render2DUtils.drawOutline(ctx, x - 0.5, y - 0.5, w + 1, h + 1,
                            lineWidth.getValue() + 0.5, outline);
                    Render2DUtils.drawOutline(ctx, x, y, w, h, lineWidth.getValue(), color);
                }
                case "WexSide" -> {
                    renderWexSideBox(ctx, x, y, w, h, color, outline);
                }
            }

            if (healthBar.getValue()) {
                Render2DUtils.drawHealthBar(ctx, x, y, h, 3,
                        living.getHealth(), living.getMaxHealth());
            }

            if (nameTag.getValue()) {
                String name = living.getName().getString();
                String healthStr = String.format(" \u00A7a%.1f\u2764", living.getHealth());
                String text = "\u00A7f" + name + healthStr;

                int textWidth = mc.textRenderer.getWidth(text);
                float textX = (float) (x + w / 2 - textWidth / 2);
                float textY = (float) (y - 12);

                Render2DUtils.drawRect(ctx, textX - 3, textY - 2,
                        textWidth + 6, 11, 0x90000000);

                ctx.drawTextWithShadow(mc.textRenderer, text, (int) textX, (int) textY, 0xFFFFFFFF);
            }

            if (armorDisplay.getValue() && living instanceof PlayerEntity player) {
                renderArmor(ctx, player, x, y2, w);
            }
        }
    }

    private void renderWexSideBox(DrawContext ctx, double x, double y, double w, double h,
                                   int color, int outline) {
        double cl = Math.min(cornerLength.getValue(), Math.min(w, h) / 3.0);
        double thick = lineWidth.getValue();

        Render2DUtils.drawCornerBox(ctx, x - 0.5, y - 0.5, w + 1, h + 1,
                cl + 1, thick + 1, outline);

        Render2DUtils.drawCornerBox(ctx, x, y, w, h, cl, thick, color);

        int leftColor = color;
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;

        float[] hsb = Color.RGBtoHSB(r, g, b, null);
        Color rightC = Color.getHSBColor(hsb[0] + 0.3f, hsb[1], hsb[2]);
        int rightColor = rightC.getRGB() | 0xFF000000;

        Render2DUtils.drawRect(ctx, x - 0.5, y - 3.5, w + 1, 1, outline);
        Render2DUtils.drawGradientRect(ctx, (int) x, (int) (y - 3), (int) (x + w), (int) (y - 1),
                leftColor | 0xFF000000, rightColor);
    }

    private void renderArmor(DrawContext ctx, PlayerEntity player, double x, double y2, double w) {
        List<ItemStack> armorItems = new java.util.ArrayList<>();

        ItemStack mainHand = player.getMainHandStack();
        if (!mainHand.isEmpty()) armorItems.add(mainHand);

        for (ItemStack stack : player.getArmorItems()) {
            if (!stack.isEmpty()) armorItems.add(stack);
        }

        ItemStack offHand = player.getOffHandStack();
        if (!offHand.isEmpty()) armorItems.add(offHand);

        if (armorItems.isEmpty()) return;

        int totalWidth = armorItems.size() * 18;
        float startX = (float) (x + w / 2 - totalWidth / 2.0);
        float armorY = (float) (y2 + 3);

        Render2DUtils.drawRect(ctx, startX - 2, armorY - 1,
                totalWidth + 4, 18, 0x80000000);

        for (int i = 0; i < armorItems.size(); i++) {
            ctx.drawItem(armorItems.get(i), (int) (startX + i * 18), (int) armorY);
        }
    }

    private int getEntityColor(LivingEntity entity) {
        int baseColor = boxColor.getRGB() | 0xFF000000;

        if (entity instanceof PlayerEntity) {
            if (entity.hurtTime > 0) {
                return new Color(255, 50, 50).getRGB();
            }
            if (entity.isInvisible()) {
                return new Color(255, 255, 80).getRGB();
            }
            return baseColor;
        }

        if (entity instanceof HostileEntity) {
            return new Color(255, 80, 80).getRGB();
        }

        if (entity instanceof AnimalEntity) {
            return new Color(80, 255, 80).getRGB();
        }

        return baseColor;
    }

    private boolean isValid(Entity entity) {
        if (!(entity instanceof LivingEntity living)) return false;
        if (!living.isAlive()) return false;
        if (entity == mc.player) return false;
        if (living.isInvisible() && !invisibles.getValue()) return false;

        if (entity instanceof PlayerEntity) return players.getValue();

        return mobs.getValue();
    }
}
