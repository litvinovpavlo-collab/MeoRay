package dev.meoray.client.hud;

import dev.meoray.client.MeoRayClient;
import dev.meoray.client.core.Module;
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
import org.joml.Matrix4f;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class KeyBindHUD extends HudElement {

    private static final Draggable draggable = new Draggable("KeyBindHUD", 10, 160, 150, 60);

    private boolean activeOnly = true;

    private final Map<String, Float> fadeMap = new HashMap<>();

    private static final float FADE_SPEED = 0.08f;

    public KeyBindHUD() {
        super(draggable, "KeyBindHUD");
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

        List<Module> allModules = MeoRayClient.INSTANCE.moduleManager.getModules();
        List<BindEntry> visibleEntries = new ArrayList<>();

        for (Module module : allModules) {
            int keyCode = module.getKey();
            if (keyCode == GLFW.GLFW_KEY_UNKNOWN || keyCode == 0) continue;

            String name = module.getName();
            boolean enabled = module.isEnabled();

            boolean shouldShow = activeOnly ? enabled : true;
            float targetFade = shouldShow ? 1.0f : 0.0f;

            float currentFade = fadeMap.getOrDefault(name, 0.0f);

            if (currentFade < targetFade) {
                currentFade = Math.min(targetFade, currentFade + FADE_SPEED);
            } else if (currentFade > targetFade) {
                currentFade = Math.max(targetFade, currentFade - FADE_SPEED);
            }

            fadeMap.put(name, currentFade);

            if (currentFade > 0.001f) {
                String keyName = getKeyName(keyCode);
                visibleEntries.add(new BindEntry(name, keyName, enabled, currentFade, module));
            }
        }

        fadeMap.entrySet().removeIf(entry -> entry.getValue() <= 0.001f
                && allModules.stream().noneMatch(m -> m.getName().equals(entry.getKey())));

        visibleEntries.sort(Comparator
                .comparingDouble((BindEntry e) -> -medium.getWidth(e.name, 6.0f)));

        if (visibleEntries.isEmpty() && !editable) return;

        float headerH = 16f;
        float lineH = 13f;
        float padX = 8f;
        float padY = 5f;
        float nameTextSize = 6.0f;
        float keyTextSize = 5.5f;
        float headerTextSize = 6.5f;
        float keyBracketGap = 6f;

        String title = "KeyBinds";
        float titleIconW = 10f;
        float titleW = medium.getWidth(title, headerTextSize) + titleIconW + 4f;

        float maxRowW = titleW;

        for (BindEntry entry : visibleEntries) {
            float nameW = medium.getWidth(entry.name, nameTextSize);
            float keyW = medium.getWidth("[" + entry.keyName + "]", keyTextSize);
            float rowW = nameW + keyBracketGap + keyW;
            if (rowW > maxRowW) maxRowW = rowW;
        }

        int displayCount = visibleEntries.isEmpty() ? 1 : visibleEntries.size();
        float w = maxRowW + padX * 2;
        float h = headerH + displayCount * lineH + padY;

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
                .font(icons).text("G")
                .color(theme.accent()).size(7.0f).thickness(0.05F)
                .build()).render(matrix, titleX, titleY - 0.5f);

        ((BuiltText) Builder.text()
                .font(medium).text(title)
                .color(0xFFCCCCCC).size(headerTextSize).thickness(0.04F)
                .build()).render(matrix, titleX + titleIconW, titleY);

        long activeCount = visibleEntries.stream().filter(e -> e.enabled).count();
        String countText = String.valueOf(activeCount);
        float countW = medium.getWidth(countText, keyTextSize);
        float countX = x + w - padX - countW;

        ((BuiltText) Builder.text()
                .font(medium).text(countText)
                .color(theme.accent()).size(keyTextSize).thickness(0.04F)
                .build()).render(matrix, countX, titleY + 0.5f);

        if (editable) {
            drawEditDots(matrix, x, y, w, h);
        }

        if (visibleEntries.isEmpty()) {
            float emptyY = y + headerH + (lineH - nameTextSize) / 2f;
            ((BuiltText) Builder.text()
                    .font(medium).text("No binds set")
                    .color(0xFF666670).size(nameTextSize).thickness(0.04F)
                    .build()).render(matrix, x + padX, emptyY);
            return;
        }

        float currentY = y + headerH;

        for (int i = 0; i < visibleEntries.size(); i++) {
            BindEntry entry = visibleEntries.get(i);
            float fade = entry.fade;

            float entryHeight = lineH * fade;
            float textY = currentY + (entryHeight - nameTextSize) / 2f;

            int nameAlpha = (int) (0xCC * fade);
            int keyAlpha = (int) (0xFF * fade);

            if (entry.enabled) {
                int highlightAlpha = (int) (0x18 * fade);
                int highlight = (highlightAlpha << 24) | (theme.accent() & 0x00FFFFFF);

                ((BuiltRectangle) Builder.rectangle()
                        .size(new SizeState(w - 2, entryHeight))
                        .color(new QuadColorState(highlight))
                        .radius(new QuadRadiusState(2.0))
                        .smoothness(1.15F)
                        .build()).render(matrix, x + 1, currentY);
            }

            if (entry.enabled) {
                int dotAlpha = (int) (0xFF * fade);
                int dotColor = (dotAlpha << 24) | (theme.accent() & 0x00FFFFFF);

                ((BuiltRectangle) Builder.rectangle()
                        .size(new SizeState(2f, entryHeight - 4f))
                        .color(new QuadColorState(dotColor))
                        .radius(new QuadRadiusState(1.0))
                        .smoothness(1.15F)
                        .build()).render(matrix, x + 2f, currentY + 2f);
            }

            int nameColor;
            if (entry.enabled) {
                nameColor = (nameAlpha << 24) | 0x00EEEEEE;
            } else {
                nameColor = (nameAlpha << 24) | 0x00888896;
            }

            float nameX = x + padX + (entry.enabled ? 2f : 0f);

            ((BuiltText) Builder.text()
                    .font(medium).text(entry.name)
                    .color(nameColor).size(nameTextSize).thickness(0.04F)
                    .build()).render(matrix, nameX, textY);

            String keyDisplay = "[" + entry.keyName + "]";
            float keyW = medium.getWidth(keyDisplay, keyTextSize);
            float keyX = x + w - padX - keyW;

            int keyColor;
            if (entry.enabled) {
                keyColor = (keyAlpha << 24) | (theme.accent() & 0x00FFFFFF);
            } else {
                keyColor = ((int) (0x88 * fade) << 24) | 0x00666670;
            }

            ((BuiltText) Builder.text()
                    .font(medium).text(keyDisplay)
                    .color(keyColor).size(keyTextSize).thickness(0.04F)
                    .build()).render(matrix, keyX, textY + 0.3f);

            if (i < visibleEntries.size() - 1 && fade > 0.5f) {
                int sepAlpha = (int) (0x20 * fade);
                int sepColor = (sepAlpha << 24) | 0x00FFFFFF;

                ((BuiltRectangle) Builder.rectangle()
                        .size(new SizeState(w - padX * 2, 0.5f))
                        .color(new QuadColorState(sepColor))
                        .radius(new QuadRadiusState(0.0))
                        .smoothness(1.15F)
                        .build()).render(matrix, x + padX, currentY + entryHeight - 0.25f);
            }

            currentY += entryHeight;
        }
    }

    private String getKeyName(int keyCode) {
        if (keyCode == GLFW.GLFW_KEY_UNKNOWN || keyCode == 0) return "NONE";

        if (keyCode < 0) {
            int mouseButton = -keyCode - 1;
            return switch (mouseButton) {
                case 0 -> "LMB";
                case 1 -> "RMB";
                case 2 -> "MMB";
                default -> "M" + (mouseButton + 1);
            };
        }

        String glfwName = GLFW.glfwGetKeyName(keyCode, 0);
        if (glfwName != null && !glfwName.isEmpty()) {
            return glfwName.toUpperCase();
        }

        return switch (keyCode) {
            case GLFW.GLFW_KEY_LEFT_SHIFT -> "LSHIFT";
            case GLFW.GLFW_KEY_RIGHT_SHIFT -> "RSHIFT";
            case GLFW.GLFW_KEY_LEFT_CONTROL -> "LCTRL";
            case GLFW.GLFW_KEY_RIGHT_CONTROL -> "RCTRL";
            case GLFW.GLFW_KEY_LEFT_ALT -> "LALT";
            case GLFW.GLFW_KEY_RIGHT_ALT -> "RALT";
            case GLFW.GLFW_KEY_SPACE -> "SPACE";
            case GLFW.GLFW_KEY_ENTER -> "ENTER";
            case GLFW.GLFW_KEY_ESCAPE -> "ESC";
            case GLFW.GLFW_KEY_TAB -> "TAB";
            case GLFW.GLFW_KEY_BACKSPACE -> "BACK";
            case GLFW.GLFW_KEY_DELETE -> "DEL";
            case GLFW.GLFW_KEY_INSERT -> "INS";
            case GLFW.GLFW_KEY_HOME -> "HOME";
            case GLFW.GLFW_KEY_END -> "END";
            case GLFW.GLFW_KEY_PAGE_UP -> "PGUP";
            case GLFW.GLFW_KEY_PAGE_DOWN -> "PGDN";
            case GLFW.GLFW_KEY_UP -> "UP";
            case GLFW.GLFW_KEY_DOWN -> "DOWN";
            case GLFW.GLFW_KEY_LEFT -> "LEFT";
            case GLFW.GLFW_KEY_RIGHT -> "RIGHT";
            case GLFW.GLFW_KEY_CAPS_LOCK -> "CAPS";
            case GLFW.GLFW_KEY_NUM_LOCK -> "NUM";
            case GLFW.GLFW_KEY_F1 -> "F1";
            case GLFW.GLFW_KEY_F2 -> "F2";
            case GLFW.GLFW_KEY_F3 -> "F3";
            case GLFW.GLFW_KEY_F4 -> "F4";
            case GLFW.GLFW_KEY_F5 -> "F5";
            case GLFW.GLFW_KEY_F6 -> "F6";
            case GLFW.GLFW_KEY_F7 -> "F7";
            case GLFW.GLFW_KEY_F8 -> "F8";
            case GLFW.GLFW_KEY_F9 -> "F9";
            case GLFW.GLFW_KEY_F10 -> "F10";
            case GLFW.GLFW_KEY_F11 -> "F11";
            case GLFW.GLFW_KEY_F12 -> "F12";
            case GLFW.GLFW_KEY_KP_0 -> "NP0";
            case GLFW.GLFW_KEY_KP_1 -> "NP1";
            case GLFW.GLFW_KEY_KP_2 -> "NP2";
            case GLFW.GLFW_KEY_KP_3 -> "NP3";
            case GLFW.GLFW_KEY_KP_4 -> "NP4";
            case GLFW.GLFW_KEY_KP_5 -> "NP5";
            case GLFW.GLFW_KEY_KP_6 -> "NP6";
            case GLFW.GLFW_KEY_KP_7 -> "NP7";
            case GLFW.GLFW_KEY_KP_8 -> "NP8";
            case GLFW.GLFW_KEY_KP_9 -> "NP9";
            default -> "KEY" + keyCode;
        };
    }

    private record BindEntry(
            String name,
            String keyName,
            boolean enabled,
            float fade,
            Module module
    ) {}

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
