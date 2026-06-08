package dev.meoray.client.util.render;

import com.mojang.blaze3d.systems.RenderSystem;
import java.util.ArrayDeque;
import java.util.Deque;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;

@Environment(EnvType.CLIENT)
public class Scissor {
    private static final Deque<int[]> stack = new ArrayDeque<>();

    public static void start(float x, float y, float width, float height) {
        MinecraftClient mc = MinecraftClient.getInstance();
        double scale = mc.getWindow().getScaleFactor();
        int sx = (int)(x * scale);
        int sy = (int)((mc.getWindow().getScaledHeight() - y - height) * scale);
        int sw = (int)(width * scale);
        int sh = (int)(height * scale);
        if (!stack.isEmpty()) {
            int[] p = stack.peek();
            int x1 = Math.max(sx, p[0]);
            int y1 = Math.max(sy, p[1]);
            int x2 = Math.min(sx + sw, p[0] + p[2]);
            int y2 = Math.min(sy + sh, p[1] + p[3]);
            sx = x1;
            sy = y1;
            sw = Math.max(0, x2 - x1);
            sh = Math.max(0, y2 - y1);
        }
        stack.push(new int[]{sx, sy, sw, sh});
        RenderSystem.enableScissor(sx, sy, sw, sh);
    }

    public static void stop() {
        if (!stack.isEmpty()) {
            stack.pop();
            if (!stack.isEmpty()) {
                int[] p = stack.peek();
                RenderSystem.enableScissor(p[0], p[1], p[2], p[3]);
            } else {
                RenderSystem.disableScissor();
            }
        }
    }
}
