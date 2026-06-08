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
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.collection.DefaultedList;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class ArmorHUD extends HudElement {

    private static final Draggable draggable = new Draggable("ArmorHUD", 10, 70, 160, 80);

    private static final Map<RegistryKey<Enchantment>, String> ENCHANT_SHORT_NAMES = Map.ofEntries(
        Map.entry(Enchantments.PROTECTION, "P"),
        Map.entry(Enchantments.FIRE_PROTECTION, "FP"),
        Map.entry(Enchantments.BLAST_PROTECTION, "BP"),
        Map.entry(Enchantments.PROJECTILE_PROTECTION, "PP"),
        Map.entry(Enchantments.THORNS, "Th"),
        Map.entry(Enchantments.RESPIRATION, "Rs"),
        Map.entry(Enchantments.AQUA_AFFINITY, "AA"),
        Map.entry(Enchantments.DEPTH_STRIDER, "DS"),
        Map.entry(Enchantments.FROST_WALKER, "FW"),
        Map.entry(Enchantments.SOUL_SPEED, "SS"),
        Map.entry(Enchantments.SWIFT_SNEAK, "SN"),
        Map.entry(Enchantments.FEATHER_FALLING, "FF"),
        Map.entry(Enchantments.SHARPNESS, "S"),
        Map.entry(Enchantments.SMITE, "Sm"),
        Map.entry(Enchantments.BANE_OF_ARTHROPODS, "BA"),
        Map.entry(Enchantments.FIRE_ASPECT, "FA"),
        Map.entry(Enchantments.LOOTING, "L"),
        Map.entry(Enchantments.SWEEPING_EDGE, "SE"),
        Map.entry(Enchantments.KNOCKBACK, "Kb"),
        Map.entry(Enchantments.POWER, "Pw"),
        Map.entry(Enchantments.PUNCH, "Pu"),
        Map.entry(Enchantments.FLAME, "Fl"),
        Map.entry(Enchantments.INFINITY, "Inf"),
        Map.entry(Enchantments.EFFICIENCY, "E"),
        Map.entry(Enchantments.SILK_TOUCH, "ST"),
        Map.entry(Enchantments.FORTUNE, "F"),
        Map.entry(Enchantments.UNBREAKING, "U"),
        Map.entry(Enchantments.MENDING, "M"),
        Map.entry(Enchantments.VANISHING_CURSE, "CV"),
        Map.entry(Enchantments.BINDING_CURSE, "CB")
    );

    public ArmorHUD() {
        super(draggable, "ArmorHUD");
    }

    @Override
    public void render(DrawContext context) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;

        boolean editable = mc.currentScreen instanceof ChatScreen
                || mc.currentScreen instanceof MeoRayClickGUI;

        Theme theme = MeoRayClient.INSTANCE.getThemeManager().getRenderTheme();
        MsdfFont medium = FontManager.SUISSEINTMEDIUM.get();
        MsdfFont icons = FontManager.ICONS.get();

        List<ItemStack> items = collectItems(mc);
        if (items.isEmpty() && !editable) return;

        float headerH = 16f;
        float padX = 8f;
        float padY = 6f;
        float headerTextSize = 6.5f;
        float itemSlotSize = 18f;
        float itemGap = 4f;
        float textSize = 5.5f;
        float infoHeight = 22f;

        String title = "Armor";
        float titleIconW = 10f;

        int itemCount = items.isEmpty() ? 5 : items.size();
        float itemsRowW = itemCount * itemSlotSize + (itemCount - 1) * itemGap;

        float titleW = medium.getWidth(title, headerTextSize) + titleIconW + 4f;
        float contentW = Math.max(itemsRowW, titleW);

        float w = contentW + padX * 2;
        float h = headerH + padY + itemSlotSize + infoHeight + padY;

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
                .font(icons).text("E")
                .color(theme.accent()).size(7.0f).thickness(0.05F)
                .build()).render(matrix, titleX, titleY - 0.5f);

        ((BuiltText) Builder.text()
                .font(medium).text(title)
                .color(0xFFCCCCCC).size(headerTextSize).thickness(0.04F)
                .build()).render(matrix, titleX + titleIconW, titleY);

        if (editable) {
            drawEditDots(matrix, x, y, w, h);
        }

        if (items.isEmpty()) {
            float emptyY = y + headerH + padY + itemSlotSize / 2f;
            ((BuiltText) Builder.text()
                    .font(medium).text("No armor equipped")
                    .color(0xFF666670).size(textSize).thickness(0.04F)
                    .build()).render(matrix, x + padX, emptyY);
            return;
        }

        float totalItemsW = items.size() * itemSlotSize + (items.size() - 1) * itemGap;
        float startX = x + (w - totalItemsW) / 2f;
        float itemY = y + headerH + padY;

        for (int i = 0; i < items.size(); i++) {
            ItemStack stack = items.get(i);
            float ix = startX + i * (itemSlotSize + itemGap);

            ((BuiltRectangle) Builder.rectangle()
                    .size(new SizeState(itemSlotSize, itemSlotSize))
                    .color(new QuadColorState(0x30FFFFFF))
                    .radius(new QuadRadiusState(2.0))
                    .smoothness(1.15F)
                    .build()).render(matrix, ix, itemY);

            int renderX = (int) (ix + (itemSlotSize - 16) / 2f);
            int renderY = (int) (itemY + (itemSlotSize - 16) / 2f);

            context.drawItem(stack, renderX, renderY);
            context.drawStackOverlay(mc.textRenderer, stack, renderX, renderY);

            float textY = itemY + itemSlotSize + 2f;

            if (stack.isDamageable()) {
                int maxDmg = stack.getMaxDamage();
                int currentDmg = stack.getDamage();
                int remaining = maxDmg - currentDmg;
                float durPercent = (float) remaining / maxDmg;

                int durColor = getDurabilityColor(durPercent);

                String durText = String.valueOf(remaining);
                float durTextW = medium.getWidth(durText, textSize);
                float durTextX = ix + (itemSlotSize - durTextW) / 2f;

                ((BuiltText) Builder.text()
                        .font(medium).text(durText)
                        .color(durColor).size(textSize).thickness(0.04F)
                        .build()).render(matrix, durTextX, textY);

                textY += textSize + 1.5f;
            }

            String enchantStr = buildEnchantString(stack);

            if (!enchantStr.isEmpty()) {
                float enchTextW = medium.getWidth(enchantStr, textSize - 0.5f);
                float enchTextX = ix + (itemSlotSize - enchTextW) / 2f;

                ((BuiltText) Builder.text()
                        .font(medium).text(enchantStr)
                        .color(0xFFAA88FF).size(textSize - 0.5f).thickness(0.04F)
                        .build()).render(matrix, enchTextX, textY);
            }
        }
    }

    private List<ItemStack> collectItems(MinecraftClient mc) {
        List<ItemStack> result = new ArrayList<>();
        if (mc.player == null) return result;

        DefaultedList<ItemStack> armorSlots = mc.player.getInventory().armor;

        for (int i = 3; i >= 0; i--) {
            ItemStack stack = armorSlots.get(i);
            if (!stack.isEmpty()) {
                result.add(stack);
            }
        }

        ItemStack offhand = mc.player.getOffHandStack();
        if (!offhand.isEmpty()) {
            result.add(offhand);
        }

        return result;
    }

    private int getDurabilityColor(float percent) {
        int r, g, b;

        if (percent > 0.6f) {
            float t = (percent - 0.6f) / 0.4f;
            r = (int) lerp(180, 50, t);
            g = (int) lerp(255, 255, t);
            b = (int) lerp(50, 80, t);
        } else if (percent > 0.3f) {
            float t = (percent - 0.3f) / 0.3f;
            r = (int) lerp(255, 180, t);
            g = (int) lerp(200, 255, t);
            b = (int) lerp(50, 50, t);
        } else {
            float t = percent / 0.3f;
            r = 255;
            g = (int) lerp(50, 200, t);
            b = (int) lerp(50, 50, t);
        }

        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    private float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }

    private String buildEnchantString(ItemStack stack) {
        ItemEnchantmentsComponent enchantments = stack.get(DataComponentTypes.ENCHANTMENTS);
        if (enchantments == null || enchantments.isEmpty()) return "";

        StringBuilder sb = new StringBuilder();
        int count = 0;
        int maxShow = 3;

        for (var entry : enchantments.getEnchantmentEntries()) {
            if (count >= maxShow) break;

            RegistryEntry<Enchantment> enchantEntry = entry.getKey();
            int level = entry.getIntValue();

            String shortName = null;
            Optional<RegistryKey<Enchantment>> keyOpt = enchantEntry.getKey();

            if (keyOpt.isPresent()) {
                shortName = ENCHANT_SHORT_NAMES.get(keyOpt.get());
            }

            if (shortName == null) {
                if (keyOpt.isPresent()) {
                    String path = keyOpt.get().getValue().getPath();
                    shortName = path.length() >= 2
                            ? path.substring(0, 2).toUpperCase()
                            : path.toUpperCase();
                } else {
                    shortName = "??";
                }
            }

            if (count > 0) sb.append(" ");
            sb.append(shortName);
            if (level > 1) {
                sb.append(level);
            }

            count++;
        }

        return sb.toString();
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

            if (dist < w) {
                dx = x + dist;
                dy = y;
            } else if (dist < w + h) {
                dx = x + w;
                dy = y + (dist - w);
            } else if (dist < w * 2 + h) {
                dx = x + w - (dist - w - h);
                dy = y + h;
            } else {
                dx = x;
                dy = y + h - (dist - w * 2 - h);
            }

            ((BuiltRectangle) Builder.rectangle()
                    .size(new SizeState(dotSize, dotSize))
                    .color(new QuadColorState(dotColor))
                    .radius(new QuadRadiusState(1.0))
                    .smoothness(1.15F)
                    .build()).render(matrix, dx, dy);
        }
    }
}
