package dev.meoray.client.util.math;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.MathHelper;

@Environment(EnvType.CLIENT)
public class MathUtil {

    public static float fast(float end, float start, float multiple) {
        return (1.0F - MathHelper.clamp((float)(deltaTime() * multiple), 0.0F, 1.0F)) * end
            + MathHelper.clamp((float)(deltaTime() * multiple), 0.0F, 1.0F) * start;
    }

    public static double deltaTime() {
        return MinecraftClient.getInstance().getCurrentFps() > 0
            ? 1.0 / MinecraftClient.getInstance().getCurrentFps()
            : 1.0;
    }

    public static boolean isHovered(int x, int y, int width, int height, int mouseX, int mouseY) {
        return x < mouseX && x + width > mouseX && y < mouseY && y + height > mouseY;
    }

    public static double interpolate(double current, double old, double scale) {
        return old + (current - old) * scale;
    }
}
