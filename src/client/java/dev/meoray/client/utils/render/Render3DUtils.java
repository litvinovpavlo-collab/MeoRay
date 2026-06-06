package dev.meoray.client.utils.render;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

import java.awt.Color;

public class Render3DUtils {

    private static final MinecraftClient mc = MinecraftClient.getInstance();

    public static Box getInterpolatedBox(Entity entity, float tickDelta) {
        double x = MathHelper.lerp(tickDelta, entity.prevX, entity.getX());
        double y = MathHelper.lerp(tickDelta, entity.prevY, entity.getY());
        double z = MathHelper.lerp(tickDelta, entity.prevZ, entity.getZ());

        Box base = entity.getBoundingBox();
        double hw = (base.maxX - base.minX) / 2.0;
        double h = base.maxY - base.minY;

        return new Box(
                x - hw, y, z - hw,
                x + hw, y + h, z + hw
        );
    }

    // === Draw outlined box (12 edges) ===
    public static void drawBoxOutline(MatrixStack matrices, Box box, int color, float lineWidth) {
        Camera camera = mc.gameRenderer.getCamera();
        Vec3d camPos = camera.getPos();

        float a = ((color >> 24) & 0xFF) / 255f;
        float r = ((color >> 16) & 0xFF) / 255f;
        float g = ((color >> 8) & 0xFF) / 255f;
        float b = (color & 0xFF) / 255f;

        double x1 = box.minX - camPos.x;
        double y1 = box.minY - camPos.y;
        double z1 = box.minZ - camPos.z;
        double x2 = box.maxX - camPos.x;
        double y2 = box.maxY - camPos.y;
        double z2 = box.maxZ - camPos.z;

        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.disableDepthTest();
        RenderSystem.lineWidth(lineWidth);

        Matrix4f matrix = matrices.peek().getPositionMatrix();
        Tessellator tess = Tessellator.getInstance();
        BufferBuilder buf = tess.begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);

        // Bottom rectangle
        line(buf, matrix, x1, y1, z1, x2, y1, z1, r, g, b, a);
        line(buf, matrix, x2, y1, z1, x2, y1, z2, r, g, b, a);
        line(buf, matrix, x2, y1, z2, x1, y1, z2, r, g, b, a);
        line(buf, matrix, x1, y1, z2, x1, y1, z1, r, g, b, a);

        // Top rectangle
        line(buf, matrix, x1, y2, z1, x2, y2, z1, r, g, b, a);
        line(buf, matrix, x2, y2, z1, x2, y2, z2, r, g, b, a);
        line(buf, matrix, x2, y2, z2, x1, y2, z2, r, g, b, a);
        line(buf, matrix, x1, y2, z2, x1, y2, z1, r, g, b, a);

        // Vertical edges
        line(buf, matrix, x1, y1, z1, x1, y2, z1, r, g, b, a);
        line(buf, matrix, x2, y1, z1, x2, y2, z1, r, g, b, a);
        line(buf, matrix, x2, y1, z2, x2, y2, z2, r, g, b, a);
        line(buf, matrix, x1, y1, z2, x1, y2, z2, r, g, b, a);

        BufferRenderer.drawWithGlobalProgram(buf.end());

        RenderSystem.enableDepthTest();
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
        RenderSystem.lineWidth(1f);
    }

    // === Filled box ===
    public static void drawBoxFilled(MatrixStack matrices, Box box, int color) {
        Camera camera = mc.gameRenderer.getCamera();
        Vec3d camPos = camera.getPos();

        float a = ((color >> 24) & 0xFF) / 255f;
        float r = ((color >> 16) & 0xFF) / 255f;
        float g = ((color >> 8) & 0xFF) / 255f;
        float b = (color & 0xFF) / 255f;

        float x1 = (float) (box.minX - camPos.x);
        float y1 = (float) (box.minY - camPos.y);
        float z1 = (float) (box.minZ - camPos.z);
        float x2 = (float) (box.maxX - camPos.x);
        float y2 = (float) (box.maxY - camPos.y);
        float z2 = (float) (box.maxZ - camPos.z);

        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.disableDepthTest();

        Matrix4f matrix = matrices.peek().getPositionMatrix();
        Tessellator tess = Tessellator.getInstance();
        BufferBuilder buf = tess.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);

        // Bottom
        buf.vertex(matrix, x1, y1, z1).color(r, g, b, a);
        buf.vertex(matrix, x2, y1, z1).color(r, g, b, a);
        buf.vertex(matrix, x2, y1, z2).color(r, g, b, a);
        buf.vertex(matrix, x1, y1, z2).color(r, g, b, a);

        // Top
        buf.vertex(matrix, x1, y2, z1).color(r, g, b, a);
        buf.vertex(matrix, x1, y2, z2).color(r, g, b, a);
        buf.vertex(matrix, x2, y2, z2).color(r, g, b, a);
        buf.vertex(matrix, x2, y2, z1).color(r, g, b, a);

        // North
        buf.vertex(matrix, x1, y1, z1).color(r, g, b, a);
        buf.vertex(matrix, x1, y2, z1).color(r, g, b, a);
        buf.vertex(matrix, x2, y2, z1).color(r, g, b, a);
        buf.vertex(matrix, x2, y1, z1).color(r, g, b, a);

        // South
        buf.vertex(matrix, x1, y1, z2).color(r, g, b, a);
        buf.vertex(matrix, x2, y1, z2).color(r, g, b, a);
        buf.vertex(matrix, x2, y2, z2).color(r, g, b, a);
        buf.vertex(matrix, x1, y2, z2).color(r, g, b, a);

        // West
        buf.vertex(matrix, x1, y1, z1).color(r, g, b, a);
        buf.vertex(matrix, x1, y1, z2).color(r, g, b, a);
        buf.vertex(matrix, x1, y2, z2).color(r, g, b, a);
        buf.vertex(matrix, x1, y2, z1).color(r, g, b, a);

        // East
        buf.vertex(matrix, x2, y1, z1).color(r, g, b, a);
        buf.vertex(matrix, x2, y2, z1).color(r, g, b, a);
        buf.vertex(matrix, x2, y2, z2).color(r, g, b, a);
        buf.vertex(matrix, x2, y1, z2).color(r, g, b, a);

        BufferRenderer.drawWithGlobalProgram(buf.end());

        RenderSystem.enableDepthTest();
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }

    // === Tracer (line from camera to entity) ===
    public static void drawTracer(MatrixStack matrices, Vec3d targetPos, int color, float lineWidth) {
        Camera camera = mc.gameRenderer.getCamera();
        Vec3d camPos = camera.getPos();

        Vec3d look = Vec3d.fromPolar(camera.getPitch(), camera.getYaw());
        Vec3d start = look.multiply(1.0);

        double tx = targetPos.x - camPos.x;
        double ty = targetPos.y - camPos.y;
        double tz = targetPos.z - camPos.z;

        float a = ((color >> 24) & 0xFF) / 255f;
        float r = ((color >> 16) & 0xFF) / 255f;
        float g = ((color >> 8) & 0xFF) / 255f;
        float b = (color & 0xFF) / 255f;

        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.disableDepthTest();
        RenderSystem.lineWidth(lineWidth);

        Matrix4f matrix = matrices.peek().getPositionMatrix();
        Tessellator tess = Tessellator.getInstance();
        BufferBuilder buf = tess.begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);

        buf.vertex(matrix, (float) start.x, (float) start.y, (float) start.z).color(r, g, b, a);
        buf.vertex(matrix, (float) tx, (float) ty, (float) tz).color(r, g, b, a);

        BufferRenderer.drawWithGlobalProgram(buf.end());

        RenderSystem.enableDepthTest();
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
        RenderSystem.lineWidth(1f);
    }

    private static void line(BufferBuilder buf, Matrix4f matrix,
                              double x1, double y1, double z1,
                              double x2, double y2, double z2,
                              float r, float g, float b, float a) {
        buf.vertex(matrix, (float) x1, (float) y1, (float) z1).color(r, g, b, a);
        buf.vertex(matrix, (float) x2, (float) y2, (float) z2).color(r, g, b, a);
    }
}
