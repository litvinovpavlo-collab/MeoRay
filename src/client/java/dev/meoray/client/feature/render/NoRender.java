package dev.meoray.client.feature.render;

import dev.meoray.client.core.Category;
import dev.meoray.client.core.Module;
import dev.meoray.client.core.setting.BooleanSetting;
import net.minecraft.client.MinecraftClient;

public class NoRender extends Module {
    public static boolean noWeather;
    public static boolean noFire;
    public static boolean noParticles;

    public final BooleanSetting weather = add(new BooleanSetting("Weather", false));
    public final BooleanSetting fire = add(new BooleanSetting("Fire", false));
    public final BooleanSetting particles = add(new BooleanSetting("Particles", false));

    public NoRender() {
        super("NoRender", "Отключает рендер погоды, огня и частиц", Category.RENDER);
    }

    @Override
    public void onTick() {
        noWeather = isEnabled() && weather.getValue();
        noFire = isEnabled() && fire.getValue();
        noParticles = isEnabled() && particles.getValue();

        if (noWeather) {
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc.world != null) {
                mc.world.setRainGradient(0);
                mc.world.setThunderGradient(0);
            }
        }
    }

    @Override
    protected void onDisable() {
        noWeather = false;
        noFire = false;
        noParticles = false;
    }
}
