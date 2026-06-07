package dev.meoray.client.feature.render;

import com.mojang.blaze3d.systems.RenderSystem;
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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.util.UUID;

public class JumpCircles extends Module {

    private final MinecraftClient mc = MinecraftClient.getInstance();

    // === MODE ===
    public final SectionSetting modeSection = add(new SectionSetting("Mode"));
    public final ModeSetting mode = modeSection.add(new ModeSetting("Mode", "Ritual",
            "Simple", "Ritual", "Demon Summon", "Pentagram", "Runes"));

    // === APPEARANCE ===
    public final SectionSetting appearanceSection = add(new SectionSetting("Appearance"));
    public final NumberSetting radius = appearanceSection.add(new NumberSetting("Radius", 1.2, 0.5, 3.0, 0.05));
    public final NumberSetting lineWidth = appearanceSection.add(new NumberSetting("Line Width", 2.0, 0.5, 5.0, 0.1));
    public final NumberSetting segments = appearanceSection.add(new NumberSetting("Segments", 64, 16, 128, 1));
    public final NumberSetting yOffset = appearanceSection.add(new NumberSetting("Y Offset", 0.02, -1.0, 2.0, 0.01));
    public final BooleanSetting rotate = appearanceSection.add(new BooleanSetting("Rotate", true));
    public final NumberSetting rotateSpeed = appearanceSection.add(new NumberSetting("Rotate Speed", 25, 0, 200, 1));
    public final BooleanSetting throughWalls = appearanceSection.add(new BooleanSetting("Through Walls", true));
    public final BooleanSetting pulse = appearanceSection.add(new BooleanSetting("Pulse", true));

    // === TARGETS ===
    public final SectionSetting targetsSection = add(new SectionSetting("Targets"));
    public final BooleanSetting self = targetsSection.add(new BooleanSetting("Self", true));
    public final BooleanSetting otherPlayers = targetsSection.add(new BooleanSetting("Other Players", true));
    public final BooleanSetting onlyOnJump = targetsSection.add(new BooleanSetting("Only On Jump", false));
    public final BooleanSetting onlyMidAir = targetsSection.add(new BooleanSetting("Only Mid Air", false));

    // === COLORS ===
    public final SectionSetting colorsSection = add(new SectionSetting("Colors"));
    public final ColorSetting primaryColor = colorsSection.add(new ColorSetting("Primary", new Color(255, 30, 30, 255)));
    public final ColorSetting secondaryColor = colorsSection.add(new ColorSetting("Secondary", new Color(180, 0, 255, 200)));
    public final ColorSetting accentColor = colorsSection.add(new ColorSetting("Accent", new Color(255, 200, 0, 220)));
    public final BooleanSetting rainbow = colorsSection.add(new BooleanSetting("Rainbow", false));
    public final NumberSetting rainbowSpeed = colorsSection.add(new NumberSetting("Rainbow Speed", 2.0, 0.1, 10.0, 0.1));

    // === EFFECTS ===
    public final SectionSetting effectsSection = add(new SectionSetting("Effects"));
    public final BooleanSetting particles = effectsSection.add(new BooleanSetting("Particles", true));
    public final NumberSetting particleCount = effectsSection.add(new NumberSetting("Particle Count", 20, 0, 60, 1));
    public final BooleanSetting fadeInOut = effectsSection.add(new BooleanSetting("Fade In/Out", true));

    private float rotation = 0f;
    private final Map<UUID, PlayerData> playerData = new ConcurrentHashMap<>();

    private static class PlayerData {
        boolean wasOnGround = true;
        long jumpTime = 0;
        float alpha = 0f;
        List<Particle> particles = new ArrayList<>();
    }

    private static class Particle {
        float angle;
        float distance;
        float height;
        float life;
        float maxLife;
        float speed;

        Particle(float angle, float distance, float speed) {
            this.angle = angle;
            this.distance = distance;
            this.height = 0f;
            this.maxLife = 20f + (float)(Math.random() * 20f);
            this.life = maxLife;
            this.speed = speed;
        }
    }

    public JumpCircles() {
        super("JumpCircles", "Ритуальные круги под игроками", Category.RENDER);
    }

    @Override
    public void onTick() {
        if (rotate.getValue()) {
            rotation += rotateSpeed.getValue().floatValue() / 20f;
            if (rotation > 360f) rotation -= 360f;
        }

        if (mc.world == null) return;

        for (Entity entity : mc.world.getEntities()) {
            if (!(entity instanceof PlayerEntity p)) continue;
            PlayerData data = playerData.computeIfAbsent(p.getUuid(), k -> new PlayerData());

            if (data.wasOnGround && !p.isOnGround()) {
                data.jumpTime = System.currentTimeMillis();
            }
            data.wasOnGround = p.isOnGround();

            boolean shouldShow = shouldShowFor(p, data);

            if (fadeInOut.getValue()) {
                if (shouldShow) {
                    data.alpha = Math.min(1f, data.alpha + 0.08f);
                } else {
                    data.alpha = Math.max(0f, data.alpha - 0.08f);
                }
            } else {
                data.alpha = shouldShow ? 1f : 0f;
            }

            if (particles.getValue() && shouldShow && data.particles.size() < particleCount.getValue().intValue()) {
                float angle = (float)(Math.random() * Math.PI * 2);
                float dist = radius.getValue().floatValue() * (0.6f + (float)Math.random() * 0.4f);
                float speed = 0.02f + (float)Math.random() * 0.04f;
                data.particles.add(new Particle(angle, dist, speed));
            }

            data.particles.removeIf(particle -> {
                particle.life -= 1f;
                particle.height += particle.speed;
                particle.angle += 0.03f;
                return particle.life <= 0;
            });
        }

        playerData.entrySet().removeIf(e -> mc.world.getPlayers().stream()
                .noneMatch(pl -> pl.getUuid().equals(e.getKey())));
    }

    private boolean shouldShowFor(PlayerEntity p, PlayerData data) {
        if (!p.isAlive()) return false;
        boolean isSelf = p == mc.player;
        if (isSelf && !self.getValue()) return false;
        if (!isSelf && !otherPlayers.getValue()) return false;

        if (onlyMidAir.getValue() && p.isOnGround()) return false;
        if (onlyOnJump.getValue()) {
            long elapsed = System.currentTimeMillis() - data.jumpTime;
            if (elapsed > 800) return false;
        }
        return true;
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

            PlayerData data = playerData.get(p.getUuid());
            if (data == null || data.alpha <= 0.01f) continue;

            boolean isSelf = p == mc.player;
            if (isSelf && mc.options.getPerspective().isFirstPerson()) continue;

            double px = MathHelper.lerp(tickDelta, p.prevX, p.getX());
            double py = MathHelper.lerp(tickDelta, p.prevY, p.getY()) + yOffset.getValue();
            double pz = MathHelper.lerp(tickDelta, p.prevZ, p.getZ());

            double rx = px - camPos.x;
            double ry = py - camPos.y;
            double rz = pz - camPos.z;

            renderCircle(matrices, rx, ry, rz, data);
        }
    }

    private void renderCircle(MatrixStack matrices, double x, double y, double z, PlayerData data) {
        matrices.push();
        matrices.translate(x, y, z);

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        if (throughWalls.getValue()) RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);

        float pulseScale = 1f;
        if (pulse.getValue()) {
            pulseScale = 1f + (float)Math.sin(System.currentTimeMillis() / 300.0) * 0.05f;
        }

        float r = radius.getValue().floatValue() * pulseScale;
        int seg = segments.getValue().intValue();
        float alpha = data.alpha;

        matrices.push();
        if (rotate.getValue()) {
            matrices.multiply(new org.joml.Quaternionf().rotateY((float) Math.toRadians(rotation)));
        }
        Matrix4f mat = matrices.peek().getPositionMatrix();

        switch (mode.getValue()) {
            case "Simple" -> drawSimple(mat, r, seg, alpha);
            case "Ritual" -> drawRitual(mat, r, seg, alpha);
            case "Demon Summon" -> drawDemonSummon(mat, r, seg, alpha);
            case "Pentagram" -> drawPentagram(mat, r, seg, alpha);
            case "Runes" -> drawRunes(mat, r, seg, alpha);
        }

        matrices.pop();

        // particles render (без вращения общего круга)
        Matrix4f matStatic = matrices.peek().getPositionMatrix();
        renderParticles(matStatic, data, alpha);

        RenderSystem.depthMask(true);
        if (throughWalls.getValue()) RenderSystem.enableDepthTest();
        RenderSystem.enableCull();
        RenderSystem.disableBlend();

        matrices.pop();
    }

    // ========== SIMPLE ==========
    private void drawSimple(Matrix4f mat, float r, int seg, float alpha) {
        drawRing(mat, r, seg, getColor(primaryColor.getValue(), alpha));
    }

    // ========== RITUAL ==========
    private void drawRitual(Matrix4f mat, float r, int seg, float alpha) {
        int c1 = getColor(primaryColor.getValue(), alpha);
        int c2 = getColor(secondaryColor.getValue(), alpha);
        int c3 = getColor(accentColor.getValue(), alpha);

        // Внешнее кольцо
        drawRing(mat, r, seg, c1);
        // Среднее кольцо
        drawRing(mat, r * 0.85f, seg, c2);
        // Внутреннее
        drawRing(mat, r * 0.4f, seg, c3);

        // Соединительные линии (как спицы)
        drawSpokes(mat, r * 0.4f, r * 0.85f, 8, c2);

        // Маленькие круги по периметру
        int smallCircles = 6;
        for (int i = 0; i < smallCircles; i++) {
            float a = (float)(Math.PI * 2 * i / smallCircles);
            float cx = (float)Math.cos(a) * r * 0.7f;
            float cz = (float)Math.sin(a) * r * 0.7f;
            drawSmallCircle(mat, cx, cz, r * 0.1f, 16, c3);
        }

        // Внутренний треугольник
        drawPolygon(mat, r * 0.35f, 3, c1);
    }

    // ========== DEMON SUMMON ==========
    private void drawDemonSummon(Matrix4f mat, float r, int seg, float alpha) {
        int c1 = getColor(primaryColor.getValue(), alpha);
        int c2 = getColor(secondaryColor.getValue(), alpha);
        int c3 = getColor(accentColor.getValue(), alpha);

        // Заполненный круг (полупрозрачный)
        drawFilledCircle(mat, r, seg, withAlpha(c1, alpha * 0.15f));

        // Внешнее двойное кольцо
        drawRing(mat, r, seg, c1);
        drawRing(mat, r * 0.95f, seg, c1);

        // Пентаграмма внутри
        drawPentagramShape(mat, r * 0.75f, c2);

        // Внутренний круг вокруг пентаграммы
        drawRing(mat, r * 0.75f, seg, c2);

        // Центральный круг
        drawRing(mat, r * 0.25f, seg, c3);
        drawFilledCircle(mat, r * 0.25f, seg, withAlpha(c3, alpha * 0.3f));

        // 6 шипов наружу
        int spikes = 6;
        for (int i = 0; i < spikes; i++) {
            float a = (float)(Math.PI * 2 * i / spikes);
            float x1 = (float)Math.cos(a) * r * 0.95f;
            float z1 = (float)Math.sin(a) * r * 0.95f;
            float x2 = (float)Math.cos(a) * r * 1.15f;
            float z2 = (float)Math.sin(a) * r * 1.15f;
            drawLine(mat, x1, 0, z1, x2, 0, z2, c3);
        }

        // Руны по краю
        int runeCount = 12;
        for (int i = 0; i < runeCount; i++) {
            float a = (float)(Math.PI * 2 * i / runeCount);
            float cx = (float)Math.cos(a) * r * 0.88f;
            float cz = (float)Math.sin(a) * r * 0.88f;
            drawRuneSymbol(mat, cx, cz, 0.08f, a, c1);
        }
    }

    // ========== PENTAGRAM ==========
    private void drawPentagram(Matrix4f mat, float r, int seg, float alpha) {
        int c1 = getColor(primaryColor.getValue(), alpha);
        int c2 = getColor(secondaryColor.getValue(), alpha);

        drawRing(mat, r, seg, c1);
        drawPentagramShape(mat, r * 0.95f, c2);
    }

    // ========== RUNES ==========
    private void drawRunes(Matrix4f mat, float r, int seg, float alpha) {
        int c1 = getColor(primaryColor.getValue(), alpha);
        int c2 = getColor(secondaryColor.getValue(), alpha);
        int c3 = getColor(accentColor.getValue(), alpha);

        drawRing(mat, r, seg, c1);
        drawRing(mat, r * 0.6f, seg, c2);

        // Руны между кольцами
        int runes = 8;
        for (int i = 0; i < runes; i++) {
            float a = (float)(Math.PI * 2 * i / runes);
            float cx = (float)Math.cos(a) * r * 0.8f;
            float cz = (float)Math.sin(a) * r * 0.8f;
            drawRuneSymbol(mat, cx, cz, 0.12f, a, c3);
        }

        // Центральная звезда (6 лучей)
        for (int i = 0; i < 6; i++) {
            float a = (float)(Math.PI * 2 * i / 6);
            float x = (float)Math.cos(a) * r * 0.55f;
            float z = (float)Math.sin(a) * r * 0.55f;
            drawLine(mat, 0, 0, 0, x, 0, z, c2);
        }
    }

    // ========== HELPERS ==========

    private void drawRing(Matrix4f mat, float r, int seg, int col) {
        float[] c = unpack(col);
        RenderSystem.lineWidth(lineWidth.getValue().floatValue());
        Tessellator tess = Tessellator.getInstance();
        BufferBuilder buf = tess.begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);
        for (int i = 0; i < seg; i++) {
            float a1 = (float)(Math.PI * 2 * i / seg);
            float a2 = (float)(Math.PI * 2 * (i + 1) / seg);
            buf.vertex(mat, (float)Math.cos(a1) * r, 0f, (float)Math.sin(a1) * r).color(c[0], c[1], c[2], c[3]);
            buf.vertex(mat, (float)Math.cos(a2) * r, 0f, (float)Math.sin(a2) * r).color(c[0], c[1], c[2], c[3]);
        }
        BufferRenderer.drawWithGlobalProgram(buf.end());
        RenderSystem.lineWidth(1f);
    }

    private void drawFilledCircle(Matrix4f mat, float r, int seg, int col) {
        float[] c = unpack(col);
        Tessellator tess = Tessellator.getInstance();
        BufferBuilder buf = tess.begin(VertexFormat.DrawMode.TRIANGLE_FAN, VertexFormats.POSITION_COLOR);
        buf.vertex(mat, 0f, 0f, 0f).color(c[0], c[1], c[2], c[3]);
        for (int i = 0; i <= seg; i++) {
            float a = (float)(Math.PI * 2 * i / seg);
            buf.vertex(mat, (float)Math.cos(a) * r, 0f, (float)Math.sin(a) * r).color(c[0], c[1], c[2], c[3] * 0.5f);
        }
        BufferRenderer.drawWithGlobalProgram(buf.end());
    }

    private void drawSmallCircle(Matrix4f mat, float cx, float cz, float r, int seg, int col) {
        float[] c = unpack(col);
        RenderSystem.lineWidth(lineWidth.getValue().floatValue());
        Tessellator tess = Tessellator.getInstance();
        BufferBuilder buf = tess.begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);
        for (int i = 0; i < seg; i++) {
            float a1 = (float)(Math.PI * 2 * i / seg);
            float a2 = (float)(Math.PI * 2 * (i + 1) / seg);
            buf.vertex(mat, cx + (float)Math.cos(a1) * r, 0f, cz + (float)Math.sin(a1) * r).color(c[0], c[1], c[2], c[3]);
            buf.vertex(mat, cx + (float)Math.cos(a2) * r, 0f, cz + (float)Math.sin(a2) * r).color(c[0], c[1], c[2], c[3]);
        }
        BufferRenderer.drawWithGlobalProgram(buf.end());
        RenderSystem.lineWidth(1f);
    }

    private void drawPolygon(Matrix4f mat, float r, int sides, int col) {
        float[] c = unpack(col);
        RenderSystem.lineWidth(lineWidth.getValue().floatValue());
        Tessellator tess = Tessellator.getInstance();
        BufferBuilder buf = tess.begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);
        for (int i = 0; i < sides; i++) {
            float a1 = (float)(Math.PI * 2 * i / sides - Math.PI / 2);
            float a2 = (float)(Math.PI * 2 * (i + 1) / sides - Math.PI / 2);
            buf.vertex(mat, (float)Math.cos(a1) * r, 0f, (float)Math.sin(a1) * r).color(c[0], c[1], c[2], c[3]);
            buf.vertex(mat, (float)Math.cos(a2) * r, 0f, (float)Math.sin(a2) * r).color(c[0], c[1], c[2], c[3]);
        }
        BufferRenderer.drawWithGlobalProgram(buf.end());
        RenderSystem.lineWidth(1f);
    }

    private void drawPentagramShape(Matrix4f mat, float r, int col) {
        float[] c = unpack(col);
        RenderSystem.lineWidth(lineWidth.getValue().floatValue());
        Tessellator tess = Tessellator.getInstance();
        BufferBuilder buf = tess.begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);

        float[][] points = new float[5][2];
        for (int i = 0; i < 5; i++) {
            float a = (float)(Math.PI * 2 * i / 5 - Math.PI / 2);
            points[i][0] = (float)Math.cos(a) * r;
            points[i][1] = (float)Math.sin(a) * r;
        }

        // Соединяем через одну (0->2->4->1->3->0) - звезда
        int[] order = {0, 2, 4, 1, 3, 0};
        for (int i = 0; i < 5; i++) {
            int a = order[i];
            int b = order[i + 1];
            buf.vertex(mat, points[a][0], 0f, points[a][1]).color(c[0], c[1], c[2], c[3]);
            buf.vertex(mat, points[b][0], 0f, points[b][1]).color(c[0], c[1], c[2], c[3]);
        }
        BufferRenderer.drawWithGlobalProgram(buf.end());
        RenderSystem.lineWidth(1f);
    }

    private void drawSpokes(Matrix4f mat, float rInner, float rOuter, int count, int col) {
        float[] c = unpack(col);
        RenderSystem.lineWidth(lineWidth.getValue().floatValue());
        Tessellator tess = Tessellator.getInstance();
        BufferBuilder buf = tess.begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);
        for (int i = 0; i < count; i++) {
            float a = (float)(Math.PI * 2 * i / count);
            float cosA = (float)Math.cos(a);
            float sinA = (float)Math.sin(a);
            buf.vertex(mat, cosA * rInner, 0f, sinA * rInner).color(c[0], c[1], c[2], c[3]);
            buf.vertex(mat, cosA * rOuter, 0f, sinA * rOuter).color(c[0], c[1], c[2], c[3]);
        }
        BufferRenderer.drawWithGlobalProgram(buf.end());
        RenderSystem.lineWidth(1f);
    }

    private void drawLine(Matrix4f mat, float x1, float y1, float z1, float x2, float y2, float z2, int col) {
        float[] c = unpack(col);
        RenderSystem.lineWidth(lineWidth.getValue().floatValue());
        Tessellator tess = Tessellator.getInstance();
        BufferBuilder buf = tess.begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);
        buf.vertex(mat, x1, y1, z1).color(c[0], c[1], c[2], c[3]);
        buf.vertex(mat, x2, y2, z2).color(c[0], c[1], c[2], c[3]);
        BufferRenderer.drawWithGlobalProgram(buf.end());
        RenderSystem.lineWidth(1f);
    }

    // Стилизованная "руна" - крестик с засечками
    private void drawRuneSymbol(Matrix4f mat, float cx, float cz, float size, float angle, int col) {
        float[] c = unpack(col);
        RenderSystem.lineWidth(lineWidth.getValue().floatValue());
        Tessellator tess = Tessellator.getInstance();
        BufferBuilder buf = tess.begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);

        float cosA = (float)Math.cos(angle);
        float sinA = (float)Math.sin(angle);

        // Вертикальная линия (вдоль радиуса)
        float vx1 = cx - sinA * size, vz1 = cz + cosA * size;
        float vx2 = cx + sinA * size, vz2 = cz - cosA * size;
        buf.vertex(mat, vx1, 0f, vz1).color(c[0], c[1], c[2], c[3]);
        buf.vertex(mat, vx2, 0f, vz2).color(c[0], c[1], c[2], c[3]);

        // Горизонтальная (перпендикуляр)
        float hx1 = cx - cosA * size * 0.5f, hz1 = cz - sinA * size * 0.5f;
        float hx2 = cx + cosA * size * 0.5f, hz2 = cz + sinA * size * 0.5f;
        buf.vertex(mat, hx1, 0f, hz1).color(c[0], c[1], c[2], c[3]);
        buf.vertex(mat, hx2, 0f, hz2).color(c[0], c[1], c[2], c[3]);

        BufferRenderer.drawWithGlobalProgram(buf.end());
        RenderSystem.lineWidth(1f);
    }

    private void renderParticles(Matrix4f mat, PlayerData data, float alpha) {
        if (!particles.getValue() || data.particles.isEmpty()) return;

        int col = getColor(accentColor.getValue(), alpha);
        float[] c = unpack(col);

        Tessellator tess = Tessellator.getInstance();
        BufferBuilder buf = tess.begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);
        RenderSystem.lineWidth(2f);

        for (Particle p : data.particles) {
            float lifeRatio = p.life / p.maxLife;
            float px = (float)Math.cos(p.angle) * p.distance;
            float pz = (float)Math.sin(p.angle) * p.distance;
            float a = c[3] * lifeRatio;

            buf.vertex(mat, px, p.height, pz).color(c[0], c[1], c[2], a);
            buf.vertex(mat, px, p.height + 0.1f, pz).color(c[0], c[1], c[2], 0f);
        }

        BufferRenderer.drawWithGlobalProgram(buf.end());
        RenderSystem.lineWidth(1f);
    }

    private float[] unpack(int col) {
        return new float[] {
                ((col >> 16) & 0xFF) / 255f,
                ((col >> 8) & 0xFF) / 255f,
                (col & 0xFF) / 255f,
                ((col >> 24) & 0xFF) / 255f
        };
    }

    private int withAlpha(int col, float alphaMul) {
        int a = (int)(((col >> 24) & 0xFF) * alphaMul);
        return (Math.max(0, Math.min(255, a)) << 24) | (col & 0x00FFFFFF);
    }

    private int getColor(Color base, float alphaMul) {
        int rgb;
        int baseAlpha;
        if (rainbow.getValue()) {
            long time = System.currentTimeMillis();
            float speed = rainbowSpeed.getValue().floatValue();
            float hue = (time % (long)(3000 / speed)) / (3000f / speed);
            rgb = Color.HSBtoRGB(hue, 0.8f, 1f) & 0x00FFFFFF;
            baseAlpha = base.getAlpha();
        } else {
            rgb = base.getRGB() & 0x00FFFFFF;
            baseAlpha = base.getAlpha();
        }
        int finalAlpha = (int)(baseAlpha * alphaMul);
        finalAlpha = Math.max(0, Math.min(255, finalAlpha));
        return (finalAlpha << 24) | rgb;
    }
}
