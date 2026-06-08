package dev.meoray.client.util.render;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.DrawContext;

@Environment(EnvType.CLIENT)
public class AnimationUtil {
    public static void sizeAnimation(DrawContext drawContext, double width, double height, double scale) {
        drawContext.getMatrices().translate(width, height, 0.0);
        drawContext.getMatrices().scale((float)scale, (float)scale, (float)scale);
        drawContext.getMatrices().translate(-width, -height, 0.0);
    }
}
