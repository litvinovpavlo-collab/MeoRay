package dev.meoray.client.feature.render;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.meoray.client.MeoRayClient;
import dev.meoray.client.core.Category;
import dev.meoray.client.core.Module;
import dev.meoray.client.core.setting.BooleanSetting;
import dev.meoray.client.core.setting.ColorSetting;
import dev.meoray.client.core.setting.ModeSetting;
import dev.meoray.client.core.setting.NumberSetting;
import dev.meoray.client.core.setting.SectionSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

import java.awt.Color;

public class ChinaHat extends Module {

    private final MinecraftClient mc = MinecraftClient.getInstance();

    // === SECTION: APPEARANCE ===
    public final SectionSetting appearanceSection = add(new SectionSetting("Appearance"));
    public final ModeSetting style = appearanceSection.add(new ModeSetting("Style", "Cone",
            "Cone", "Disk", "Ring", "Pyramid"));
    public final NumberSetting radius = appearanceSection.add(new NumberSetting("Radius", 0.7, 0.2, 2.0, 0.05));
    public final NumberSetting height = appearanceSection.add(new NumberSetting("Height", 0.4, 0.0, 1.5, 0.05));
    public final NumberSetting yOffset = appearanceSection.add(new NumberSetting("Y Offset", 0.3, -1.0, 2.0, 0.05));
    public final NumberSetting segments = appearanceSection.add(new NumberSetting("Segments", 32, 8, 64, 1));
    public final NumberSetting lineWidth = appearanceSection.add(new NumberSetting("Line Width", 2.0, 0.5, 5.0, 0.5));
    public final BooleanSetting filled = appearanceSection.add(new BooleanSetting("Filled", true));
    public final BooleanSetting outline = appearanceSection.add(new BooleanSetting("Outline", true));
    public final BooleanSetting rotate = appearanceSection.add(new BooleanSetting("Rotate", true));
    public final NumberSetting rotateSpeed = appearanceSection.add(new NumberSetting("Rotate Speed", 30, 5, 180, 1));
    public final BooleanSetting throughWalls = appearanceSection.add(new BooleanSetting("Through Walls", true));

    // === SECTION: TARGETS ===
    public final SectionSetting targetsSection = add(new SectionSetting("Targets"));
    public final BooleanSetting self = targetsSection.add(new BooleanSetting("Self", true));
    public final BooleanSetting otherPlayers = targetsSection.add(new BooleanSetting("Other Players", false));
    public final BooleanSetting friends = targetsSection.add(new BooleanSetting("Friends", true));

    // === SECTION: COLORS ===
    public final SectionSetting colorsSection = add(new SectionSetting("Colors"));
    public final ColorSetting fillColor = colorsSection.add(new ColorSetting("Fill", new Color(180, 80, 255, 120)));
    public final ColorSetting outlineColor = colorsSection.add(new ColorSetting("Outline", new Color(220, 130, 255, 220)));
    public final BooleanSetting rainbow = colorsSection.add(new BooleanSetting("Rainbow", false));
    public final NumberSetting rainbowSpeed = colorsSection.add(new NumberSetting("Rainbow Speed", 2.0, 0.1, 10.0, 0.1));

    private float rotation = 0f;

    public ChinaHat() {
        super("ChinaHat", "Рисует шляпу над игроками", Category.RENDER);
    }

    @Override
    public void onTick() {
        if (rotate.getValue()) {
            rotation += rotateSpeed.getValue().floatValue() / 20f;
            if (rotation > 360f) rotation -= 360f;
        }
    }

    public void onWorldRender(MatrixStack matrices, float tickDelta) {
        if (!isEnabled()) return;
        if (mc.world == null || mc.player == null) return;

        Camera cam = mc.gameRenderer.getCamera();
        if (cam == null) return;
        Vec3d camPos = cam.getPos();

        for (Entity entity : mc.world.getEntities()) {
            if (!(entity instanceof PlayerEntity p)) continue;
            if (!p.isAlive()) continue;
            try { if (dev.meoray.client.feature.misc.FakePlayer.isFake(entity)) continue; } catch (Throwable ignored) {}

            boolean isSelf = p == mc.player;
            boolean isFriend = false;
            try {
                isFriend = MeoRayClient.INSTANCE.getFriendManager()
                        .isFriend(p.getGameProfile().getName());
            } catch (Throwable ignored) {}

            if (isSelf && !self.getValue()) continue;
            if (isSelf && mc.options.getPerspective().isFirstPerson()) continue;
            if (!isSelf) {
                if (isFriend && !friends.getValue()) continue;
                if (!isFriend && !otherPlayers.getValue()) continue;
            }

            double px = MathHelper.lerp(tickDelta, p.prevX, p.getX());
            double py = MathHelper.lerp(tickDelta, p.prevY, p.getY()) + p.getStandingEyeHeight() + yOffset.getValue();
            double pz = MathHelper.lerp(tickDelta, p.prevZ, p.getZ());

            double rx = px - camPos.x;
            double ry = py - camPos.y;
            double rz = pz - camPos.z;

            renderHat(matrices, rx, ry, rz);
        }
    }

    private void renderHat(MatrixStack matrices, double x, double y, double z) {
        matrices.push();
        matrices.translate(x, y, z);
        if (rotate.getValue()) {
            matrices.multiply(new org.joml.Quaternionf().rotateY((float) Math.toRadians(rotation)));
        }
        Matrix4f mat = matrices.peek().getPositionMatrix();

        int fillCol = getColor(fillColor.getValue());
        int outlineCol = getColor(outlineColor.getValue());

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        if (throughWalls.getValue()) RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);

        float r = radius.getValue().floatValue();
        float h = height.getValue().floatValue();
        int seg = segments.getValue().intValue();

        switch (style.getValue()) {
            case "Cone" -> drawCone(mat, r, h, seg, fillCol, outlineCol);
            case "Disk" -> drawDisk(mat, r, seg, fillCol, outlineCol);
            case "Ring" -> drawRing(mat, r, seg, outlineCol);
            case "Pyramid" -> drawPyramid(mat, r, h, fillCol, outlineCol);
        }

        RenderSystem.depthMask(true);
        if (throughWalls.getValue()) RenderSystem.enableDepthTest();
        RenderSystem.enableCull();
        RenderSystem.disableBlend();

        matrices.pop();
    }

    private void drawCone(Matrix4f mat, float radius, float height, int segments,
                          int fillCol, int outlineCol) {
        if (filled.getValue()) {
            float fr = ((fillCol >> 16) & 0xFF) / 255f;
            float fg = ((fillCol >> 8) & 0xFF) / 255f;
            float fb = (fillCol & 0xFF) / 255f;
            float fa = ((fillCol >> 24) & 0xFF) / 255f;

            Tessellator tess = Tessellator.getInstance();
            BufferBuilder buf = tess.begin(VertexFormat.DrawMode.TRIANGLE_FAN, VertexFormats.POSITION_COLOR);

            buf.vertex(mat, 0f, height, 0f).color(fr, fg, fb, fa);
            for (int i = 0; i <= segments; i++) {
                float a = (float) (Math.PI * 2 * i / segments);
                float vx = (float) Math.cos(a) * radius;
                float vz = (float) Math.sin(a) * radius;
                buf.vertex(mat, vx, 0f, vz).color(fr, fg, fb, fa * 0.4f);
            }
            BufferRenderer.drawWithGlobalProgram(buf.end());
        }

        if (outline.getValue()) {
            float or = ((outlineCol >> 16) & 0xFF) / 255f;
            float og = ((outlineCol >> 8) & 0xFF) / 255f;
            float ob = (outlineCol & 0xFF) / 255f;
            float oa = ((outlineCol >> 24) & 0xFF) / 255f;

            RenderSystem.lineWidth(lineWidth.getValue().floatValue());

            Tessellator tess = Tessellator.getInstance();
            BufferBuilder buf = tess.begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);

            for (int i = 0; i < segments; i++) {
                float a1 = (float) (Math.PI * 2 * i / segments);
                float a2 = (float) (Math.PI * 2 * (i + 1) / segments);
                float x1 = (float) Math.cos(a1) * radius;
                float z1 = (float) Math.sin(a1) * radius;
                float x2 = (float) Math.cos(a2) * radius;
                float z2 = (float) Math.sin(a2) * radius;
                buf.vertex(mat, x1, 0f, z1).color(or, og, ob, oa);
                buf.vertex(mat, x2, 0f, z2).color(or, og, ob, oa);
            }

            int rayStep = Math.max(1, segments / 16);
            for (int i = 0; i < segments; i += rayStep) {
                float a = (float) (Math.PI * 2 * i / segments);
                float vx = (float) Math.cos(a) * radius;
                float vz = (float) Math.sin(a) * radius;
                buf.vertex(mat, 0f, height, 0f).color(or, og, ob, oa);
                buf.vertex(mat, vx, 0f, vz).color(or, og, ob, oa);
            }

            BufferRenderer.drawWithGlobalProgram(buf.end());
            RenderSystem.lineWidth(1f);
        }
    }

    private void drawDisk(Matrix4f mat, float radius, int segments, int fillCol, int outlineCol) {
        if (filled.getValue()) {
            float fr = ((fillCol >> 16) & 0xFF) / 255f;
            float fg = ((fillCol >> 8) & 0xFF) / 255f;
            float fb = (fillCol & 0xFF) / 255f;
            float fa = ((fillCol >> 24) & 0xFF) / 255f;

            Tessellator tess = Tessellator.getInstance();
            BufferBuilder buf = tess.begin(VertexFormat.DrawMode.TRIANGLE_FAN, VertexFormats.POSITION_COLOR);

            buf.vertex(mat, 0f, 0f, 0f).color(fr, fg, fb, fa);
            for (int i = 0; i <= segments; i++) {
                float a = (float) (Math.PI * 2 * i / segments);
                buf.vertex(mat, (float) Math.cos(a) * radius, 0f, (float) Math.sin(a) * radius)
                   .color(fr, fg, fb, fa * 0.3f);
            }
            BufferRenderer.drawWithGlobalProgram(buf.end());
        }

        if (outline.getValue()) {
            float or = ((outlineCol >> 16) & 0xFF) / 255f;
            float og = ((outlineCol >> 8) & 0xFF) / 255f;
            float ob = (outlineCol & 0xFF) / 255f;
            float oa = ((outlineCol >> 24) & 0xFF) / 255f;

            RenderSystem.lineWidth(lineWidth.getValue().floatValue());

            Tessellator tess = Tessellator.getInstance();
            BufferBuilder buf = tess.begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);
            for (int i = 0; i < segments; i++) {
                float a1 = (float) (Math.PI * 2 * i / segments);
                float a2 = (float) (Math.PI * 2 * (i + 1) / segments);
                buf.vertex(mat, (float) Math.cos(a1) * radius, 0f, (float) Math.sin(a1) * radius).color(or, og, ob, oa);
                buf.vertex(mat, (float) Math.cos(a2) * radius, 0f, (float) Math.sin(a2) * radius).color(or, og, ob, oa);
            }
            BufferRenderer.drawWithGlobalProgram(buf.end());
            RenderSystem.lineWidth(1f);
        }
    }

    private void drawRing(Matrix4f mat, float radius, int segments, int outlineCol) {
        float or = ((outlineCol >> 16) & 0xFF) / 255f;
        float og = ((outlineCol >> 8) & 0xFF) / 255f;
        float ob = (outlineCol & 0xFF) / 255f;
        float oa = ((outlineCol >> 24) & 0xFF) / 255f;

        RenderSystem.lineWidth(lineWidth.getValue().floatValue() * 1.5f);

        Tessellator tess = Tessellator.getInstance();
        BufferBuilder buf = tess.begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);
        for (int i = 0; i < segments; i++) {
            float a1 = (float) (Math.PI * 2 * i / segments);
            float a2 = (float) (Math.PI * 2 * (i + 1) / segments);
            buf.vertex(mat, (float) Math.cos(a1) * radius, 0f, (float) Math.sin(a1) * radius).color(or, og, ob, oa);
            buf.vertex(mat, (float) Math.cos(a2) * radius, 0f, (float) Math.sin(a2) * radius).color(or, og, ob, oa);
        }
        BufferRenderer.drawWithGlobalProgram(buf.end());
        RenderSystem.lineWidth(1f);
    }

    private void drawPyramid(Matrix4f mat, float radius, float height, int fillCol, int outlineCol) {
        float r = radius;
        float[][] base = {
                { r, 0,  r}, {-r, 0,  r},
                {-r, 0, -r}, { r, 0, -r}
        };

        if (filled.getValue()) {
            float fr = ((fillCol >> 16) & 0xFF) / 255f;
            float fg = ((fillCol >> 8) & 0xFF) / 255f;
            float fb = (fillCol & 0xFF) / 255f;
            float fa = ((fillCol >> 24) & 0xFF) / 255f;

            Tessellator tess = Tessellator.getInstance();
            BufferBuilder buf = tess.begin(VertexFormat.DrawMode.TRIANGLES, VertexFormats.POSITION_COLOR);
            for (int i = 0; i < 4; i++) {
                int j = (i + 1) % 4;
                buf.vertex(mat, 0f, height, 0f).color(fr, fg, fb, fa);
                buf.vertex(mat, base[i][0], 0f, base[i][2]).color(fr, fg, fb, fa * 0.4f);
                buf.vertex(mat, base[j][0], 0f, base[j][2]).color(fr, fg, fb, fa * 0.4f);
            }
            BufferRenderer.drawWithGlobalProgram(buf.end());
        }

        if (outline.getValue()) {
            float or = ((outlineCol >> 16) & 0xFF) / 255f;
            float og = ((outlineCol >> 8) & 0xFF) / 255f;
            float ob = (outlineCol & 0xFF) / 255f;
            float oa = ((outlineCol >> 24) & 0xFF) / 255f;

            RenderSystem.lineWidth(lineWidth.getValue().floatValue());

            Tessellator tess = Tessellator.getInstance();
            BufferBuilder buf = tess.begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);
            for (int i = 0; i < 4; i++) {
                int j = (i + 1) % 4;
                buf.vertex(mat, 0f, height, 0f).color(or, og, ob, oa);
                buf.vertex(mat, base[i][0], 0f, base[i][2]).color(or, og, ob, oa);
                buf.vertex(mat, base[i][0], 0f, base[i][2]).color(or, og, ob, oa);
                buf.vertex(mat, base[j][0], 0f, base[j][2]).color(or, og, ob, oa);
            }
            BufferRenderer.drawWithGlobalProgram(buf.end());
            RenderSystem.lineWidth(1f);
        }
    }

    private int getColor(Color base) {
        if (rainbow.getValue()) {
            long time = System.currentTimeMillis();
            float speed = rainbowSpeed.getValue().floatValue();
            float hue = (time % (long)(3000 / speed)) / (3000f / speed);
            int rgb = Color.HSBtoRGB(hue, 0.8f, 1f);
            return (base.getAlpha() << 24) | (rgb & 0x00FFFFFF);
        }
        return base.getRGB();
    }
}
