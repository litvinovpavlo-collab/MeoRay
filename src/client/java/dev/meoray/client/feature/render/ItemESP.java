package dev.meoray.client.feature.render;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.meoray.client.core.Category;
import dev.meoray.client.core.Module;
import dev.meoray.client.core.setting.BooleanSetting;
import dev.meoray.client.core.setting.ColorSetting;
import dev.meoray.client.core.setting.NumberSetting;
import dev.meoray.client.core.setting.SectionSetting;
import dev.meoray.client.util.render.esp.EspMatrixHolder;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Vector4f;

import java.awt.Color;
import java.util.Locale;

public class ItemESP extends Module {

    private final MinecraftClient mc = MinecraftClient.getInstance();

    public final SectionSetting main = add(new SectionSetting("Main"));
    public final NumberSetting maxDistance = main.add(new NumberSetting("Max Distance", 64, 8, 256, 1));
    public final BooleanSetting throughWalls = main.add(new BooleanSetting("Through Walls", true));
    public final BooleanSetting showName = main.add(new BooleanSetting("Show Name", true));
    public final BooleanSetting showCount = main.add(new BooleanSetting("Show Count", true));
    public final BooleanSetting showDistance = main.add(new BooleanSetting("Show Distance", true));
    public final BooleanSetting tracers = main.add(new BooleanSetting("Tracers", true));
    public final NumberSetting tracerWidth = main.add(new NumberSetting("Tracer Width", 1.5, 0.5, 4.0, 0.1));
    public final ColorSetting color = main.add(new ColorSetting("Color", new Color(80, 220, 255, 230)));

    public ItemESP() {
        super("ItemESP", "Подсветка предметов на земле", Category.RENDER);
    }

    public void onWorldRender(MatrixStack matrices, float tickDelta) {
        if (!isEnabled()) return;
        if (mc.world == null || mc.player == null) return;

        Camera cam = mc.gameRenderer.getCamera();
        if (cam == null) return;
        Vec3d camPos = cam.getPos();

        int col = color.getValue().getRGB();

        for (Entity entity : mc.world.getEntities()) {
            if (!(entity instanceof ItemEntity itemEntity)) continue;
            if (!itemEntity.isAlive()) continue;

            double dist = itemEntity.distanceTo(mc.player);
            if (dist > maxDistance.getValue().doubleValue()) continue;

            double px = MathHelper.lerp((double) tickDelta, itemEntity.prevX, itemEntity.getX());
            double py = MathHelper.lerp((double) tickDelta, itemEntity.prevY, itemEntity.getY());
            double pz = MathHelper.lerp((double) tickDelta, itemEntity.prevZ, itemEntity.getZ());

            float size = 0.35f;
            float half = size / 2f;
            float yOffset = 0.1f;

            double rx = px - camPos.x;
            double ry = py - camPos.y;
            double rz = pz - camPos.z;

            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.disableCull();
            if (throughWalls.getValue()) RenderSystem.disableDepthTest();
            RenderSystem.depthMask(false);
            RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);

            matrices.push();
            matrices.translate(rx, ry, rz);
            Matrix4f mat = matrices.peek().getPositionMatrix();

            float minX = -half, minY = yOffset, minZ = -half;
            float maxX = half, maxY = yOffset + size, maxZ = half;

            drawBoxFilled(mat, minX, minY, minZ, maxX, maxY, maxZ, withAlpha(col, 0.12f));
            drawBoxOutline(mat, minX, minY, minZ, maxX, maxY, maxZ, col);

            matrices.pop();

            RenderSystem.depthMask(true);
            if (throughWalls.getValue()) RenderSystem.enableDepthTest();
            RenderSystem.enableCull();
            RenderSystem.disableBlend();
        }
    }

    public void onHudRender(DrawContext context, float tickDelta) {
        if (!isEnabled()) return;
        if (mc.world == null || mc.player == null) return;

        Camera cam = mc.gameRenderer.getCamera();
        if (cam == null) return;

        Matrix4f projView = EspMatrixHolder.projView;
        if (projView == null) return;

        Vec3d camPos = cam.getPos();
        int col = color.getValue().getRGB();
        net.minecraft.client.font.TextRenderer tr = mc.textRenderer;

        int scW = mc.getWindow().getScaledWidth();
        int scH = mc.getWindow().getScaledHeight();

        float tracerStartX = scW / 2f;
        float tracerStartY = scH;

        for (Entity entity : mc.world.getEntities()) {
            if (!(entity instanceof ItemEntity itemEntity)) continue;
            if (!itemEntity.isAlive()) continue;

            double dist = itemEntity.distanceTo(mc.player);
            if (dist > maxDistance.getValue().doubleValue()) continue;

            double px = MathHelper.lerp((double) tickDelta, itemEntity.prevX, itemEntity.getX());
            double py = MathHelper.lerp((double) tickDelta, itemEntity.prevY, itemEntity.getY());
            double pz = MathHelper.lerp((double) tickDelta, itemEntity.prevZ, itemEntity.getZ());

            double cx = px - camPos.x;
            double cy = py - camPos.y + 0.25;
            double cz = pz - camPos.z;

            Vector4f centerPos = new Vector4f((float) cx, (float) cy, (float) cz, 1f);
            projView.transform(centerPos);
            if (centerPos.w <= 0) continue;

            float invWC = 1f / centerPos.w;
            float itemScreenX = (centerPos.x * invWC * 0.5f + 0.5f) * scW;
            float itemScreenY = (1f - (centerPos.y * invWC * 0.5f + 0.5f)) * scH;

            if (tracers.getValue()) {
                drawLine2D(context, tracerStartX, tracerStartY, itemScreenX, itemScreenY, col);
            }

            if (showName.getValue()) {
                double wx = px - camPos.x;
                double wy = py - camPos.y + 0.55;
                double wz = pz - camPos.z;

                Vector4f pos = new Vector4f((float) wx, (float) wy, (float) wz, 1f);
                projView.transform(pos);
                if (pos.w <= 0) continue;

                float invW = 1f / pos.w;
                float screenX = (pos.x * invW * 0.5f + 0.5f) * scW;
                float screenY = (1f - (pos.y * invW * 0.5f + 0.5f)) * scH;

                String name = itemEntity.getStack().getName().getString();
                int count = itemEntity.getStack().getCount();
                String label = name;
                if (showCount.getValue() && count > 1) label = label + " x" + count;
                String distText = showDistance.getValue() ? String.format(Locale.ROOT, "%.1fm", dist) : null;

                int labelW = tr.getWidth(label);
                int textX = (int) (screenX - labelW / 2f);
                int textY = (int) screenY;

                int pad = 3;
                int bgCol = 0xB0000000;
                context.fill(textX - pad, textY - 2, textX + labelW + pad, textY + 10, bgCol);
                context.fill(textX - pad, textY + 9, textX + labelW + pad, textY + 10, col | 0xFF000000);
                context.drawText(tr, label, textX, textY, 0xFFFFFFFF, false);

                if (distText != null) {
                    int distW = tr.getWidth(distText);
                    int distX = (int) (screenX - distW / 2f);
                    int distY = textY + 13;
                    context.fill(distX - pad, distY - 1, distX + distW + pad, distY + 9, bgCol);
                    context.drawText(tr, distText, distX, distY, 0xFFCCCCCC, false);
                }
            }
        }
    }

    private void drawLine2D(DrawContext context, float x1, float y1, float x2, float y2, int col) {
        Matrix4f mat = context.getMatrices().peek().getPositionMatrix();
        float[] c = unpack(col);

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
        RenderSystem.lineWidth(tracerWidth.getValue().floatValue());

        Tessellator tess = Tessellator.getInstance();
        BufferBuilder buf = tess.begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);
        buf.vertex(mat, x1, y1, 0f).color(c[0], c[1], c[2], c[3] * 0.2f);
        buf.vertex(mat, x2, y2, 0f).color(c[0], c[1], c[2], c[3]);
        BufferRenderer.drawWithGlobalProgram(buf.end());

        RenderSystem.lineWidth(1f);
    }

    private void drawBoxOutline(Matrix4f mat, float minX, float minY, float minZ,
                                 float maxX, float maxY, float maxZ, int col) {
        float[] c = unpack(col);
        RenderSystem.lineWidth(1.5f);
        Tessellator tess = Tessellator.getInstance();
        BufferBuilder buf = tess.begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);
        line(buf, mat, minX, minY, minZ, maxX, minY, minZ, c);
        line(buf, mat, maxX, minY, minZ, maxX, minY, maxZ, c);
        line(buf, mat, maxX, minY, maxZ, minX, minY, maxZ, c);
        line(buf, mat, minX, minY, maxZ, minX, minY, minZ, c);
        line(buf, mat, minX, maxY, minZ, maxX, maxY, minZ, c);
        line(buf, mat, maxX, maxY, minZ, maxX, maxY, maxZ, c);
        line(buf, mat, maxX, maxY, maxZ, minX, maxY, maxZ, c);
        line(buf, mat, minX, maxY, maxZ, minX, maxY, minZ, c);
        line(buf, mat, minX, minY, minZ, minX, maxY, minZ, c);
        line(buf, mat, maxX, minY, minZ, maxX, maxY, minZ, c);
        line(buf, mat, maxX, minY, maxZ, maxX, maxY, maxZ, c);
        line(buf, mat, minX, minY, maxZ, minX, maxY, maxZ, c);
        BufferRenderer.drawWithGlobalProgram(buf.end());
        RenderSystem.lineWidth(1f);
    }

    private void line(BufferBuilder buf, Matrix4f mat,
                       float x1, float y1, float z1, float x2, float y2, float z2, float[] c) {
        buf.vertex(mat, x1, y1, z1).color(c[0], c[1], c[2], c[3]);
        buf.vertex(mat, x2, y2, z2).color(c[0], c[1], c[2], c[3]);
    }

    private void drawBoxFilled(Matrix4f mat, float minX, float minY, float minZ,
                                float maxX, float maxY, float maxZ, int col) {
        float[] c = unpack(col);
        Tessellator tess = Tessellator.getInstance();
        BufferBuilder buf = tess.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        quad(buf, mat, minX, minY, minZ, maxX, minY, minZ, maxX, minY, maxZ, minX, minY, maxZ, c);
        quad(buf, mat, minX, maxY, minZ, maxX, maxY, minZ, maxX, maxY, maxZ, minX, maxY, maxZ, c);
        quad(buf, mat, minX, minY, minZ, maxX, minY, minZ, maxX, maxY, minZ, minX, maxY, minZ, c);
        quad(buf, mat, minX, minY, maxZ, maxX, minY, maxZ, maxX, maxY, maxZ, minX, maxY, maxZ, c);
        quad(buf, mat, minX, minY, minZ, minX, minY, maxZ, minX, maxY, maxZ, minX, maxY, minZ, c);
        quad(buf, mat, maxX, minY, minZ, maxX, minY, maxZ, maxX, maxY, maxZ, maxX, maxY, minZ, c);
        BufferRenderer.drawWithGlobalProgram(buf.end());
    }

    private void quad(BufferBuilder buf, Matrix4f mat,
                       float x1, float y1, float z1, float x2, float y2, float z2,
                       float x3, float y3, float z3, float x4, float y4, float z4, float[] c) {
        buf.vertex(mat, x1, y1, z1).color(c[0], c[1], c[2], c[3]);
        buf.vertex(mat, x2, y2, z2).color(c[0], c[1], c[2], c[3]);
        buf.vertex(mat, x3, y3, z3).color(c[0], c[1], c[2], c[3]);
        buf.vertex(mat, x4, y4, z4).color(c[0], c[1], c[2], c[3]);
    }

    private void renderNameTag(MatrixStack matrices, Camera cam,
                                double rx, double ry, double rz,
                                String text, String distText, int col) {
        matrices.push();
        matrices.translate(rx, ry, rz);

        matrices.multiply(cam.getRotation());
        float scale = 0.025f;
        matrices.scale(-scale, -scale, scale);

        net.minecraft.client.font.TextRenderer tr = mc.textRenderer;
        int textW = tr.getWidth(text);
        int halfW = textW / 2;
        int distW = distText != null ? tr.getWidth(distText) : 0;
        int distHalfW = distW / 2;

        Matrix4f mat = matrices.peek().getPositionMatrix();

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);

        float bgA = 0.75f;
        float pad = 2.5f;
        float bgY1 = -2f;
        float bgY2 = 9f;

        Tessellator tess = Tessellator.getInstance();
        BufferBuilder buf = tess.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        buf.vertex(mat, -halfW - pad, bgY1, 0).color(0f, 0f, 0f, bgA);
        buf.vertex(mat, -halfW - pad, bgY2, 0).color(0f, 0f, 0f, bgA);
        buf.vertex(mat, halfW + pad, bgY2, 0).color(0f, 0f, 0f, bgA);
        buf.vertex(mat, halfW + pad, bgY1, 0).color(0f, 0f, 0f, bgA);
        BufferRenderer.drawWithGlobalProgram(buf.end());

        float[] c = unpack(col);
        BufferBuilder buf2 = tess.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        buf2.vertex(mat, -halfW - pad, bgY2 - 0.6f, 0).color(c[0], c[1], c[2], 1f);
        buf2.vertex(mat, -halfW - pad, bgY2, 0).color(c[0], c[1], c[2], 1f);
        buf2.vertex(mat, halfW + pad, bgY2, 0).color(c[0], c[1], c[2], 1f);
        buf2.vertex(mat, halfW + pad, bgY2 - 0.6f, 0).color(c[0], c[1], c[2], 1f);
        BufferRenderer.drawWithGlobalProgram(buf2.end());

        if (distText != null) {
            BufferBuilder buf3 = tess.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
            float dY1 = bgY2 + 1.5f;
            float dY2 = dY1 + 9f;
            buf3.vertex(mat, -distHalfW - pad, dY1, 0).color(0f, 0f, 0f, bgA);
            buf3.vertex(mat, -distHalfW - pad, dY2, 0).color(0f, 0f, 0f, bgA);
            buf3.vertex(mat, distHalfW + pad, dY2, 0).color(0f, 0f, 0f, bgA);
            buf3.vertex(mat, distHalfW + pad, dY1, 0).color(0f, 0f, 0f, bgA);
            BufferRenderer.drawWithGlobalProgram(buf3.end());
        }

        net.minecraft.client.render.VertexConsumerProvider.Immediate vcp =
                mc.getBufferBuilders().getEntityVertexConsumers();

        tr.draw(text, -halfW, 0, 0xFFFFFFFF, false,
                mat, vcp,
                net.minecraft.client.font.TextRenderer.TextLayerType.NORMAL,
                0, 0xF000F0);

        if (distText != null) {
            tr.draw(distText, -distHalfW, 11, 0xFFCCCCCC, false,
                    mat, vcp,
                    net.minecraft.client.font.TextRenderer.TextLayerType.NORMAL,
                    0, 0xF000F0);
        }

        vcp.draw();

        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();

        matrices.pop();
    }

    private float[] unpack(int col) {
        return new float[]{
                ((col >> 16) & 0xFF) / 255f,
                ((col >> 8) & 0xFF) / 255f,
                (col & 0xFF) / 255f,
                ((col >> 24) & 0xFF) / 255f
        };
    }

    private int withAlpha(int col, float mul) {
        int a = (int) (((col >> 24) & 0xFF) * mul);
        return (Math.max(0, Math.min(255, a)) << 24) | (col & 0x00FFFFFF);
    }
}
