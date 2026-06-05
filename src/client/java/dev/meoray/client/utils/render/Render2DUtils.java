package dev.meoray.client.utils.render;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.Camera;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.awt.Color;

public class Render2DUtils {

    private static final MinecraftClient mc = MinecraftClient.getInstance();

    // === PROJECTION 3D -> 2D ===
    public static Vec3d worldToScreen(Vec3d pos) {
        Camera camera = mc.gameRenderer.getCamera();
        if (camera == null) return null;

        Vec3d camPos = camera.getPos();
        double x = pos.x - camPos.x;
        double y = pos.y - camPos.y;
        double z = pos.z - camPos.z;

        // Используем quaternion камеры через её Rotation
        org.joml.Quaternionf rot = new org.joml.Quaternionf(camera.getRotation());
        rot.conjugate(); // обратный поворот (мир -> camera space)

        org.joml.Vector3f vec = new org.joml.Vector3f((float) x, (float) y, (float) z);
        rot.transform(vec);

        // В Minecraft camera смотрит в -Z
        // Но после rotation forward = -Z, поэтому:
        float vx = vec.x;
        float vy = vec.y;
        float vz = -vec.z; // инвертируем чтобы "вперёд" было положительным

        if (vz <= 0.01) return null;

        double fov = mc.options.getFov().getValue();
        double f = 1.0 / Math.tan(Math.toRadians(fov) / 2.0);

        int w = mc.getWindow().getScaledWidth();
        int h = mc.getWindow().getScaledHeight();
        double aspect = (double) w / h;

        double ndcX = (vx * f / aspect) / vz;
        double ndcY = (vy * f) / vz;

        double screenX = (ndcX + 1.0) / 2.0 * w;
        double screenY = (1.0 - ndcY) / 2.0 * h;

        return new Vec3d(screenX, screenY, vz);
    }

    public static double[] getEntityScreenPos(Entity entity, float tickDelta) {
        double x = MathHelper.lerp(tickDelta, entity.prevX, entity.getX());
        double y = MathHelper.lerp(tickDelta, entity.prevY, entity.getY());
        double z = MathHelper.lerp(tickDelta, entity.prevZ, entity.getZ());

        Box box = entity.getBoundingBox();
        double hw = (box.maxX - box.minX) / 2.0;
        double bh = box.maxY - box.minY;

        Vec3d[] corners = {
                new Vec3d(x - hw, y, z - hw),
                new Vec3d(x - hw, y, z + hw),
                new Vec3d(x + hw, y, z - hw),
                new Vec3d(x + hw, y, z + hw),
                new Vec3d(x - hw, y + bh, z - hw),
                new Vec3d(x - hw, y + bh, z + hw),
                new Vec3d(x + hw, y + bh, z - hw),
                new Vec3d(x + hw, y + bh, z + hw),
        };

        double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE;
        double maxX = -Double.MAX_VALUE, maxY = -Double.MAX_VALUE;
        boolean anyVisible = false;

        for (Vec3d c : corners) {
            Vec3d s = worldToScreen(c);
            if (s == null) continue;
            anyVisible = true;
            minX = Math.min(minX, s.x);
            minY = Math.min(minY, s.y);
            maxX = Math.max(maxX, s.x);
            maxY = Math.max(maxY, s.y);
        }

        if (!anyVisible) return null;
        return new double[]{minX, minY, maxX, maxY};
    }

    // === DRAW ===
    public static void drawRect(DrawContext ctx, double x, double y, double w, double h, int color) {
        ctx.fill((int) x, (int) y, (int) (x + w), (int) (y + h), color);
    }

    public static void drawOutline(DrawContext ctx, double x, double y, double w, double h, double thickness, int color) {
        drawRect(ctx, x, y, w, thickness, color);
        drawRect(ctx, x, y + h - thickness, w, thickness, color);
        drawRect(ctx, x, y, thickness, h, color);
        drawRect(ctx, x + w - thickness, y, thickness, h, color);
    }

    public static void drawCornerBox(DrawContext ctx, double x, double y, double w, double h,
                                     double cornerLen, double thickness, int color) {
        double cl = Math.min(cornerLen, Math.min(w, h) / 3.0);
        drawRect(ctx, x, y, cl, thickness, color);
        drawRect(ctx, x, y, thickness, cl, color);
        drawRect(ctx, x + w - cl, y, cl, thickness, color);
        drawRect(ctx, x + w - thickness, y, thickness, cl, color);
        drawRect(ctx, x, y + h - thickness, cl, thickness, color);
        drawRect(ctx, x, y + h - cl, thickness, cl, color);
        drawRect(ctx, x + w - cl, y + h - thickness, cl, thickness, color);
        drawRect(ctx, x + w - thickness, y + h - cl, thickness, cl, color);
    }

    public static void drawCornerBoxOutlined(DrawContext ctx, double x, double y, double w, double h,
                                              double cornerLen, double thickness, int color, int outlineColor) {
        drawCornerBox(ctx, x - 0.5, y - 0.5, w + 1, h + 1, cornerLen + 0.5, thickness + 1, outlineColor);
        drawCornerBox(ctx, x, y, w, h, cornerLen, thickness, color);
    }

    public static void drawGradientRect(DrawContext ctx, int x1, int y1, int x2, int y2,
                                         int colorTop, int colorBottom) {
        ctx.fillGradient(x1, y1, x2, y2, colorTop, colorBottom);
    }

    public static void drawHealthBar(DrawContext ctx, double x, double y, double h,
                                      double barWidth, float health, float maxHealth) {
        double p = Math.min(health / maxHealth, 1.0);
        double fh = h * p;
        drawRect(ctx, x - barWidth - 2, y - 0.5, barWidth + 1, h + 1, 0xAA000000);
        int r = (int) ((1.0 - p) * 2 * 255);
        int g = (int) (p * 2 * 255);
        r = Math.max(0, Math.min(255, r));
        g = Math.max(0, Math.min(255, g));
        int color = new Color(r, g, 0).getRGB();
        drawRect(ctx, x - barWidth - 1.5, y + h - fh + 0.5, barWidth - 0.5, fh - 0.5, color);
    }

    public static void drawShadowBox(DrawContext ctx, double x, double y, double w, double h,
                                      int shadowSize, int baseColor) {
        int alpha = (baseColor >> 24) & 0xFF;
        int r = (baseColor >> 16) & 0xFF;
        int g = (baseColor >> 8) & 0xFF;
        int b = baseColor & 0xFF;
        for (int i = shadowSize; i >= 1; i--) {
            float ratio = (float) i / shadowSize;
            int sa = (int) (alpha * 0.15f * (1.0f - ratio));
            int sc = (sa << 24) | (r << 16) | (g << 8) | b;
            drawOutline(ctx, x - i, y - i, w + i * 2, h + i * 2, 1, sc);
        }
    }
}
