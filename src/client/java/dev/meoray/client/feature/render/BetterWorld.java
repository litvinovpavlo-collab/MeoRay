package dev.meoray.client.feature.render;

import dev.meoray.client.core.Category;
import dev.meoray.client.core.Module;
import dev.meoray.client.core.setting.BooleanSetting;
import dev.meoray.client.core.setting.ModeSetting;
import dev.meoray.client.core.setting.NumberSetting;
import dev.meoray.client.util.render.SkyConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;

public class BetterWorld extends Module {

    private final BooleanSetting timeChanger = add(new BooleanSetting("Time Changer", false));
    private final NumberSetting time = add(new NumberSetting("Time", 1, 1, 40, 1));
    private final BooleanSetting fullBright = add(new BooleanSetting("FullBright", false));
    private final BooleanSetting fog = add(new BooleanSetting("Fog", false));
    private final NumberSetting fogStart = add(new NumberSetting("Fog Start", 3, 0, 20, 1));
    private final NumberSetting fogEnd = add(new NumberSetting("Fog End", 50, 0, 80, 1));
    private final NumberSetting fogAlpha = add(new NumberSetting("Fog Alpha", 90, 30, 100, 5));
    private final BooleanSetting skyShader = add(new BooleanSetting("Sky Shader", false));
    private final BooleanSetting weather = add(new BooleanSetting("Weather", false));
    private final ModeSetting weatherMode = add(new ModeSetting("Weather Mode", "Clear", "Clear", "Rain", "Storm"));

    private static BetterWorld instance;

    public BetterWorld() {
        super("BetterWorld", "World rendering tweaks", Category.RENDER);
        instance = this;
    }

    public static BetterWorld getInstance() { return instance; }

    @Override
    public void onTick() {
        var mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.world == null) return;

        if (timeChanger.getValue()) {
            mc.world.getLevelProperties().setTimeOfDay((long) (time.getValue().doubleValue() * 500L));
        }

        if (fullBright.getValue()) {
            mc.player.addStatusEffect(new StatusEffectInstance(StatusEffects.NIGHT_VISION, 400, 2));
        }

        if (weather.getValue()) {
            boolean rain = weatherMode.getValue().equals("Rain") || weatherMode.getValue().equals("Storm");
            boolean thunder = weatherMode.getValue().equals("Storm");
            mc.world.getLevelProperties().setRaining(rain);
            mc.world.setRainGradient(rain ? 1f : 0f);
            mc.world.setThunderGradient(thunder ? 1f : 0f);
        }
    }

    @Override
    public void onDisable() {
        var mc = MinecraftClient.getInstance();
        if (fullBright.getValue() && mc.player != null) {
            mc.player.removeStatusEffect(StatusEffects.NIGHT_VISION);
        }
    }

    public boolean isFogEnabled() { return isEnabled() && fog.getValue(); }
    public float getFogStart() { return fogStart.getValue().floatValue(); }
    public float getFogEnd() { return fogEnd.getValue().floatValue(); }
    public float getFogAlpha() { return fogAlpha.getValue().floatValue() / 100f; }
    public boolean isSkyShaderEnabled() { return isEnabled() && skyShader.getValue(); }
}
