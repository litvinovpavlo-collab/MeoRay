package dev.meoray.client.feature.render;

import dev.meoray.client.core.Category;
import dev.meoray.client.core.Module;
import dev.meoray.client.core.setting.BooleanSetting;
import dev.meoray.client.core.setting.ColorSetting;
import dev.meoray.client.core.setting.ModeSetting;
import dev.meoray.client.core.setting.NumberSetting;
import dev.meoray.client.core.setting.SectionSetting;
import net.minecraft.client.MinecraftClient;

import java.awt.Color;

public class AspectRatio extends Module {

    public static AspectRatio INSTANCE;
    private final MinecraftClient mc = MinecraftClient.getInstance();

    public final SectionSetting ratioSection = add(new SectionSetting("Aspect Ratio"));
    public final ModeSetting preset = ratioSection.add(
            new ModeSetting("Preset", "21:9 Ultrawide",
                    "21:9 Ultrawide",
                    "16:9 Standard",
                    "16:10",
                    "4:3 Classic",
                    "1:1 Square",
                    "9:16 Vertical",
                    "3:2",
                    "32:9 Super Ultrawide",
                    "Custom"));

    public final NumberSetting customWidth = ratioSection.add(
            new NumberSetting("Custom Width", 21.0, 1.0, 64.0, 0.1));
    public final NumberSetting customHeight = ratioSection.add(
            new NumberSetting("Custom Height", 9.0, 1.0, 64.0, 0.1));

    public final SectionSetting visualSection = add(new SectionSetting("Visual"));
    public final ModeSetting barColor = visualSection.add(
            new ModeSetting("Bars Color", "Black",
                    "Black", "White", "Transparent", "Blurred", "Custom"));
    public final ColorSetting customBarColor = visualSection.add(
            new ColorSetting("Custom Bar Color", new Color(0, 0, 0, 255)));
    public final BooleanSetting smoothTransition = visualSection.add(
            new BooleanSetting("Smooth Transition", true));
    public final NumberSetting transitionSpeed = visualSection.add(
            new NumberSetting("Transition Speed", 0.15, 0.02, 1.0, 0.02));

    public final SectionSetting behaviorSection = add(new SectionSetting("Behavior"));
    public final BooleanSetting hideHud = behaviorSection.add(
            new BooleanSetting("Hide HUD On Bars", false));
    public final BooleanSetting affectFOV = behaviorSection.add(
            new BooleanSetting("Affect FOV", true));
    public final BooleanSetting onlyInGame = behaviorSection.add(
            new BooleanSetting("Disable In Menus", true));

    private float currentAspect = 16f / 9f;
    private float targetAspect = 16f / 9f;
    private float currentBarHorizontal = 0f;
    private float currentBarVertical = 0f;

    public AspectRatio() {
        super("AspectRatio", "Изменение соотношения сторон экрана", Category.RENDER);
        INSTANCE = this;
    }

    @Override
    public void onEnable() {
        if (mc.getWindow() != null) {
            currentAspect = (float) mc.getWindow().getFramebufferWidth()
                    / mc.getWindow().getFramebufferHeight();
        }
        super.onEnable();
    }

    @Override
    public void onDisable() {
        currentBarHorizontal = 0f;
        currentBarVertical = 0f;
        super.onDisable();
    }

    public void update(float deltaTime) {
        if (!isEnabled()) return;
        if (mc.getWindow() == null) return;

        targetAspect = getTargetAspect();

        if (smoothTransition.getValue()) {
            float speed = transitionSpeed.getValue().floatValue();
            float t = 1.0f - (float) Math.exp(-deltaTime / speed);
            currentAspect += (targetAspect - currentAspect) * t;
        } else {
            currentAspect = targetAspect;
        }

        calculateBars();
    }

    private float getTargetAspect() {
        return switch (preset.getValue()) {
            case "21:9 Ultrawide"        -> 21f / 9f;
            case "16:9 Standard"         -> 16f / 9f;
            case "16:10"                 -> 16f / 10f;
            case "4:3 Classic"           -> 4f / 3f;
            case "1:1 Square"            -> 1f;
            case "9:16 Vertical"         -> 9f / 16f;
            case "3:2"                   -> 3f / 2f;
            case "32:9 Super Ultrawide"  -> 32f / 9f;
            case "Custom" -> customWidth.getValue().floatValue()
                          / customHeight.getValue().floatValue();
            default -> 16f / 9f;
        };
    }

    private void calculateBars() {
        if (mc.getWindow() == null) return;

        float screenWidth  = mc.getWindow().getFramebufferWidth();
        float screenHeight = mc.getWindow().getFramebufferHeight();
        float screenAspect = screenWidth / screenHeight;

        if (currentAspect > screenAspect) {
            float targetHeight = screenWidth / currentAspect;
            currentBarVertical = (screenHeight - targetHeight) / 2f;
            currentBarHorizontal = 0f;
        } else if (currentAspect < screenAspect) {
            float targetWidth = screenHeight * currentAspect;
            currentBarHorizontal = (screenWidth - targetWidth) / 2f;
            currentBarVertical = 0f;
        } else {
            currentBarHorizontal = 0f;
            currentBarVertical = 0f;
        }
    }

    public float getCurrentAspect() {
        return currentAspect;
    }

    public float getBarHorizontal() {
        return currentBarHorizontal;
    }

    public float getBarVertical() {
        return currentBarVertical;
    }

    public int getBarColorARGB() {
        return switch (barColor.getValue()) {
            case "Black"       -> 0xFF000000;
            case "White"       -> 0xFFFFFFFF;
            case "Transparent" -> 0x00000000;
            case "Custom"      -> customBarColor.getValue().getRGB();
            case "Blurred"     -> 0xCC000000;
            default            -> 0xFF000000;
        };
    }

    public boolean shouldHideHud() {
        return hideHud.getValue();
    }

    public boolean shouldAffectFOV() {
        return affectFOV.getValue();
    }

    public boolean shouldDisableInMenu() {
        return onlyInGame.getValue();
    }

    public boolean isActiveNow() {
        if (!isEnabled()) return false;
        if (onlyInGame.getValue() && mc.currentScreen != null) return false;
        return mc.world != null && mc.player != null;
    }
}
