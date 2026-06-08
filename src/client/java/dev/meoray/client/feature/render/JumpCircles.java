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
            "Simple", "Ritual", "Demon Summon", "Pentagram", "Runes", "Arcane", "Void"));

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
    public final BooleanSetting glow = appearanceSection.add(new BooleanSetting("Glow", true));
    public final NumberSetting glowIntensity = appearanceSection.add(new NumberSetting("Glow Intensity", 3, 1, 6, 1));
    public final NumberSetting glowSpread = appearanceSection.add(new NumberSetting("Glow Spread", 2.5, 1.0, 5.0, 0.1));
    public final BooleanSetting innerGlow = appearanceSection.add(new BooleanSetting("Inner Glow", true));

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
        // АДДИТИВНЫЙ блендинг = свечение
        RenderSystem.blendFunc(
            com.mojang.blaze3d.platform.GlStateManager.SrcFactor.SRC_ALPHA,
            com.mojang.blaze3d.platform.GlStateManager.DstFactor.ONE
        );
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
            case "Arcane" -> drawArcane(mat, r, seg, alpha);
            case "Void" -> drawVoid(mat, r, seg, alpha);
        }

        matrices.pop();

        // particles render (без вращения общего круга)
        Matrix4f matStatic = matrices.peek().getPositionMatrix();
        renderParticles(matStatic, data, alpha);

        RenderSystem.depthMask(true);
        if (throughWalls.getValue()) RenderSystem.enableDepthTest();
        RenderSystem.enableCull();
        RenderSystem.defaultBlendFunc(); // вернуть стандартный
        RenderSystem.disableBlend();

        matrices.pop();
    }

    // ========== SIMPLE (улучшенный) ==========
    private void drawSimple(Matrix4f mat, float r, int seg, float alpha) {
        int c1 = getColor(primaryColor.getValue(), alpha);
        int c2 = getColor(secondaryColor.getValue(), alpha);

        // Главное толстое кольцо
        drawRing(mat, r, seg, c1);
        // Внутреннее тонкое
        drawRing(mat, r * 0.92f, seg, withAlpha(c1, alpha * 0.5f));
        // Прерывистое наружное
        drawDashedRing(mat, r * 1.08f, seg, c2, 8);
        // Маленькие точки-узелки
        int knots = 12;
        for (int i = 0; i < knots; i++) {
            float a = (float)(Math.PI * 2 * i / knots);
            float cx = (float)Math.cos(a) * r;
            float cz = (float)Math.sin(a) * r;
            drawSmallCircle(mat, cx, cz, r * 0.04f, 12, c2);
        }
    }

    // ========== RITUAL (улучшенный) ==========
    private void drawRitual(Matrix4f mat, float r, int seg, float alpha) {
        int c1 = getColor(primaryColor.getValue(), alpha);
        int c2 = getColor(secondaryColor.getValue(), alpha);
        int c3 = getColor(accentColor.getValue(), alpha);

        // Большой заполненный градиент в центре
        drawFilledCircle(mat, r * 0.95f, seg, withAlpha(c1, alpha * 0.08f));

        // Внешнее двойное кольцо
        drawRing(mat, r, seg, c1);
        drawRing(mat, r * 0.97f, seg, withAlpha(c1, alpha * 0.4f));

        // Среднее с обратным вращением (визуально)
        drawDashedRing(mat, r * 0.85f, seg, c2, 16);
        drawRing(mat, r * 0.82f, seg, withAlpha(c2, alpha * 0.6f));

        // Спицы (8 штук)
        drawSpokes(mat, r * 0.4f, r * 0.85f, 8, c2);

        // Между спицами - короткие линии
        drawOffsetSpokes(mat, r * 0.5f, r * 0.7f, 8, withAlpha(c3, alpha * 0.7f), (float)(Math.PI / 8));

        // Маленькие круги по периметру (с точками внутри)
        int smallCircles = 6;
        for (int i = 0; i < smallCircles; i++) {
            float a = (float)(Math.PI * 2 * i / smallCircles);
            float cx = (float)Math.cos(a) * r * 0.7f;
            float cz = (float)Math.sin(a) * r * 0.7f;
            drawSmallCircle(mat, cx, cz, r * 0.1f, 16, c3);
            drawFilledCircle(mat, cx, cz, r * 0.04f, 12, c3);
        }

        // Внутренний треугольник
        drawPolygon(mat, r * 0.35f, 3, c1);
        // Перевёрнутый треугольник (звезда Давида)
        drawPolygonRotated(mat, r * 0.35f, 3, c2, (float)Math.PI);

        // Центральная точка
        drawFilledCircle(mat, r * 0.06f, 12, c3);
        drawRing(mat, r * 0.1f, 24, c3);
    }

    // ========== DEMON SUMMON (намного эпичнее) ==========
    private void drawDemonSummon(Matrix4f mat, float r, int seg, float alpha) {
        int c1 = getColor(primaryColor.getValue(), alpha);
        int c2 = getColor(secondaryColor.getValue(), alpha);
        int c3 = getColor(accentColor.getValue(), alpha);

        // Тёмная "лужа" в основе
        drawFilledCircle(mat, r, seg, withAlpha(c1, alpha * 0.18f));
        drawFilledCircle(mat, r * 0.7f, seg, withAlpha(c1, alpha * 0.15f));

        // Внешнее тройное кольцо
        drawRing(mat, r * 1.05f, seg, withAlpha(c1, alpha * 0.4f));
        drawRing(mat, r, seg, c1);
        drawRing(mat, r * 0.95f, seg, c1);

        // Прерывистое кольцо между
        drawDashedRing(mat, r * 0.88f, seg, c3, 24);

        // Пентаграмма внутри
        drawPentagramShape(mat, r * 0.75f, c2);
        drawPentagramShape(mat, r * 0.73f, withAlpha(c2, alpha * 0.5f)); // двойная для "толщины"

        // Кольцо вокруг пентаграммы
        drawRing(mat, r * 0.75f, seg, c2);
        drawRing(mat, r * 0.78f, seg, withAlpha(c2, alpha * 0.5f));

        // Центральный круг
        drawFilledCircle(mat, r * 0.28f, seg, withAlpha(c3, alpha * 0.4f));
        drawRing(mat, r * 0.25f, seg, c3);
        drawRing(mat, r * 0.3f, seg, withAlpha(c3, alpha * 0.6f));

        // Глаз в центре (мини-круг + точка)
        drawRing(mat, r * 0.12f, 24, c3);
        drawFilledCircle(mat, r * 0.05f, 12, c3);

        // 6 шипов наружу (длинные, треугольные)
        int spikes = 6;
        for (int i = 0; i < spikes; i++) {
            float a = (float)(Math.PI * 2 * i / spikes);
            drawSpike(mat, a, r * 1.05f, r * 1.25f, r * 0.04f, c3);
        }

        // 6 малых шипов между большими
        for (int i = 0; i < spikes; i++) {
            float a = (float)(Math.PI * 2 * i / spikes) + (float)(Math.PI / spikes);
            drawSpike(mat, a, r * 1.05f, r * 1.15f, r * 0.025f, withAlpha(c1, alpha * 0.8f));
        }

        // Руны по краю (большие)
        int runeCount = 12;
        for (int i = 0; i < runeCount; i++) {
            float a = (float)(Math.PI * 2 * i / runeCount);
            float cx = (float)Math.cos(a) * r * 0.88f;
            float cz = (float)Math.sin(a) * r * 0.88f;
            drawRuneSymbol(mat, cx, cz, 0.09f, a, c1);
        }

        // Маленькие точки между рунами
        for (int i = 0; i < runeCount; i++) {
            float a = (float)(Math.PI * 2 * i / runeCount) + (float)(Math.PI / runeCount);
            float cx = (float)Math.cos(a) * r * 0.88f;
            float cz = (float)Math.sin(a) * r * 0.88f;
            drawFilledCircle(mat, cx, cz, 0.02f, 8, c3);
        }
    }

    // ========== PENTAGRAM (улучшенный) ==========
    private void drawPentagram(Matrix4f mat, float r, int seg, float alpha) {
        int c1 = getColor(primaryColor.getValue(), alpha);
        int c2 = getColor(secondaryColor.getValue(), alpha);
        int c3 = getColor(accentColor.getValue(), alpha);

        // Лёгкое свечение
        drawFilledCircle(mat, r, seg, withAlpha(c1, alpha * 0.1f));

        // Двойное внешнее кольцо
        drawRing(mat, r, seg, c1);
        drawRing(mat, r * 0.97f, seg, withAlpha(c1, alpha * 0.5f));

        // Пентаграмма (двойная для эффекта толщины)
        drawPentagramShape(mat, r * 0.95f, c2);
        drawPentagramShape(mat, r * 0.92f, withAlpha(c2, alpha * 0.5f));

        // Точки на углах пентаграммы
        for (int i = 0; i < 5; i++) {
            float a = (float)(Math.PI * 2 * i / 5 - Math.PI / 2);
            float x = (float)Math.cos(a) * r * 0.95f;
            float z = (float)Math.sin(a) * r * 0.95f;
            drawFilledCircle(mat, x, z, r * 0.06f, 16, c3);
            drawRing(mat, r * 0.08f, 16, withAlpha(c3, alpha * 0.7f));
        }

        // Внутренний 5-угольник (где пересекаются линии)
        drawPolygon(mat, r * 0.36f, 5, c3);

        // Центральная точка
        drawFilledCircle(mat, r * 0.05f, 12, c3);
    }

    // ========== RUNES (улучшенный) ==========
    private void drawRunes(Matrix4f mat, float r, int seg, float alpha) {
        int c1 = getColor(primaryColor.getValue(), alpha);
        int c2 = getColor(secondaryColor.getValue(), alpha);
        int c3 = getColor(accentColor.getValue(), alpha);

        // Внешнее кольцо двойное
        drawRing(mat, r, seg, c1);
        drawRing(mat, r * 0.98f, seg, withAlpha(c1, alpha * 0.5f));

        // Среднее кольцо
        drawRing(mat, r * 0.6f, seg, c2);
        drawRing(mat, r * 0.58f, seg, withAlpha(c2, alpha * 0.5f));

        // Прерывистое кольцо между
        drawDashedRing(mat, r * 0.8f, seg, c3, 20);

        // Большие руны между кольцами (8 шт)
        int runes = 8;
        for (int i = 0; i < runes; i++) {
            float a = (float)(Math.PI * 2 * i / runes);
            float cx = (float)Math.cos(a) * r * 0.8f;
            float cz = (float)Math.sin(a) * r * 0.8f;
            drawRuneSymbol(mat, cx, cz, 0.13f, a, c3);
        }

        // Маленькие руны (16 шт, между большими)
        for (int i = 0; i < 16; i++) {
            if (i % 2 == 0) continue;
            float a = (float)(Math.PI * 2 * i / 16);
            float cx = (float)Math.cos(a) * r * 0.7f;
            float cz = (float)Math.sin(a) * r * 0.7f;
            drawRuneSymbol(mat, cx, cz, 0.06f, a, withAlpha(c1, alpha * 0.7f));
        }

        // Центральная гексаграмма (6-лучевая звезда)
        drawHexagram(mat, r * 0.5f, c2);

        // Внутреннее кольцо
        drawRing(mat, r * 0.2f, seg, c3);
        drawFilledCircle(mat, r * 0.2f, seg, withAlpha(c3, alpha * 0.3f));

        // Центральная точка
        drawFilledCircle(mat, r * 0.06f, 12, c3);
    }

    // ========== ARCANE (новый - магический) ==========
    private void drawArcane(Matrix4f mat, float r, int seg, float alpha) {
        int c1 = getColor(primaryColor.getValue(), alpha);
        int c2 = getColor(secondaryColor.getValue(), alpha);
        int c3 = getColor(accentColor.getValue(), alpha);

        // Магическое свечение в центре
        drawFilledCircle(mat, r * 0.8f, seg, withAlpha(c2, alpha * 0.1f));
        drawFilledCircle(mat, r * 0.5f, seg, withAlpha(c3, alpha * 0.15f));

        // Основное кольцо
        drawRing(mat, r, seg, c1);

        // Внутренние концентрические круги (магические уровни)
        for (int i = 1; i <= 4; i++) {
            float ratio = 1f - (i * 0.18f);
            int col = withAlpha(c1, alpha * (1f - i * 0.15f));
            drawRing(mat, r * ratio, seg, col);
        }

        // Многоугольники наслаиваются с разными углами
        drawPolygonRotated(mat, r * 0.9f, 8, c2, 0f);
        drawPolygonRotated(mat, r * 0.9f, 8, withAlpha(c2, alpha * 0.5f), (float)(Math.PI / 8));

        drawPolygonRotated(mat, r * 0.6f, 6, c3, 0f);
        drawPolygonRotated(mat, r * 0.6f, 6, withAlpha(c3, alpha * 0.6f), (float)(Math.PI / 6));

        // Лучи света наружу
        int rays = 16;
        for (int i = 0; i < rays; i++) {
            float a = (float)(Math.PI * 2 * i / rays);
            float len = (i % 2 == 0) ? r * 1.2f : r * 1.1f;
            float cosA = (float)Math.cos(a);
            float sinA = (float)Math.sin(a);
            int col = (i % 2 == 0) ? c1 : withAlpha(c2, alpha * 0.7f);
            drawLine(mat, cosA * r, 0, sinA * r, cosA * len, 0, sinA * len, col);
        }

        // Центральная звезда (мини-пентаграмма)
        drawPentagramShape(mat, r * 0.25f, c3);
        drawFilledCircle(mat, r * 0.06f, 12, c3);
    }

    // ========== VOID (новый - тёмный/звёздный) ==========
    private void drawVoid(Matrix4f mat, float r, int seg, float alpha) {
        int c1 = getColor(primaryColor.getValue(), alpha);
        int c2 = getColor(secondaryColor.getValue(), alpha);
        int c3 = getColor(accentColor.getValue(), alpha);

        // Глубокая "дыра" в центре - градиент
        drawFilledCircle(mat, r, seg, withAlpha(c1, alpha * 0.05f));
        drawFilledCircle(mat, r * 0.7f, seg, withAlpha(c1, alpha * 0.15f));
        drawFilledCircle(mat, r * 0.4f, seg, withAlpha(c1, alpha * 0.3f));

        // 3 концентрических кольца с разной "толщиной"
        drawRing(mat, r, seg, c1);
        drawRing(mat, r * 0.99f, seg, withAlpha(c1, alpha * 0.6f));

        drawRing(mat, r * 0.75f, seg, c2);
        drawRing(mat, r * 0.74f, seg, withAlpha(c2, alpha * 0.5f));

        drawRing(mat, r * 0.5f, seg, c3);
        drawRing(mat, r * 0.49f, seg, withAlpha(c3, alpha * 0.5f));

        // Хаотичные точки вокруг (звёзды)
        long seed = 12345L; // фиксированный паттерн
        java.util.Random rand = new java.util.Random(seed);
        for (int i = 0; i < 40; i++) {
            float a = rand.nextFloat() * (float)Math.PI * 2;
            float dist = r * (0.3f + rand.nextFloat() * 0.8f);
            float cx = (float)Math.cos(a) * dist;
            float cz = (float)Math.sin(a) * dist;
            float starSize = 0.015f + rand.nextFloat() * 0.025f;
            int starCol = rand.nextBoolean() ? c3 : withAlpha(c2, alpha * 0.8f);
            drawFilledCircle(mat, cx, cz, starSize, 6, starCol);
        }

        // Хаотичные линии (трещины пустоты)
        for (int i = 0; i < 8; i++) {
            float a1 = (float)(Math.PI * 2 * i / 8);
            float a2 = a1 + (float)(Math.PI / 6);
            float r1 = r * 0.4f;
            float r2 = r * 0.95f;
            drawLine(mat, (float)Math.cos(a1) * r1, 0, (float)Math.sin(a1) * r1,
                    (float)Math.cos(a2) * r2, 0, (float)Math.sin(a2) * r2,
                    withAlpha(c2, alpha * 0.7f));
        }

        // Гексаграмма в центре
        drawHexagram(mat, r * 0.3f, c3);

        // Точка-сингулярность
        drawFilledCircle(mat, r * 0.05f, 16, c3);
        drawRing(mat, r * 0.08f, 16, withAlpha(c3, alpha * 0.6f));
    }

    // ========== HELPERS ==========

    private void drawRing(Matrix4f mat, float r, int seg, int col) {
        float[] c = unpack(col);

        if (glow.getValue()) {
            int layers = glowIntensity.getValue().intValue();
            float spread = glowSpread.getValue().floatValue();
            float baseWidth = lineWidth.getValue().floatValue();

            // Рисуем от самого толстого и прозрачного к тонкому и яркому
            for (int layer = layers; layer >= 0; layer--) {
                float t = (float) layer / layers; // 0..1
                float width = baseWidth + spread * 2f * t;
                float a = c[3] * (1f - t * 0.85f); // прозрачнее наружу
                if (layer == 0) {
                    a = Math.min(1f, c[3] * 1.5f); // ядро ярче
                    width = baseWidth * 0.8f;
                }

                RenderSystem.lineWidth(width);
                Tessellator tess = Tessellator.getInstance();
                BufferBuilder buf = tess.begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);
                for (int i = 0; i < seg; i++) {
                    float a1 = (float)(Math.PI * 2 * i / seg);
                    float a2 = (float)(Math.PI * 2 * (i + 1) / seg);
                    buf.vertex(mat, (float)Math.cos(a1) * r, 0f, (float)Math.sin(a1) * r).color(c[0], c[1], c[2], a);
                    buf.vertex(mat, (float)Math.cos(a2) * r, 0f, (float)Math.sin(a2) * r).color(c[0], c[1], c[2], a);
                }
                BufferRenderer.drawWithGlobalProgram(buf.end());
            }
            RenderSystem.lineWidth(1f);
        } else {
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
    }

    private void drawFilledCircle(Matrix4f mat, float r, int seg, int col) {
        drawFilledCircle(mat, 0f, 0f, r, seg, col);
    }

    private void drawFilledCircle(Matrix4f mat, float cx, float cz, float r, int seg, int col) {
        float[] c = unpack(col);

        if (innerGlow.getValue()) {
            // Несколько слоёв заполнения от центра наружу = свечение
            int glowLayers = 3;
            for (int g = 0; g < glowLayers; g++) {
                float layerR = r * (1f - g * 0.2f);
                float layerA = c[3] * (0.4f + g * 0.3f);

                Tessellator tess = Tessellator.getInstance();
                BufferBuilder buf = tess.begin(VertexFormat.DrawMode.TRIANGLE_FAN, VertexFormats.POSITION_COLOR);
                buf.vertex(mat, cx, 0f, cz).color(c[0], c[1], c[2], layerA);
                for (int i = 0; i <= seg; i++) {
                    float a = (float)(Math.PI * 2 * i / seg);
                    buf.vertex(mat, cx + (float)Math.cos(a) * layerR, 0f, cz + (float)Math.sin(a) * layerR)
                       .color(c[0], c[1], c[2], 0f);
                }
                BufferRenderer.drawWithGlobalProgram(buf.end());
            }
        } else {
            Tessellator tess = Tessellator.getInstance();
            BufferBuilder buf = tess.begin(VertexFormat.DrawMode.TRIANGLE_FAN, VertexFormats.POSITION_COLOR);
            buf.vertex(mat, cx, 0f, cz).color(c[0], c[1], c[2], c[3]);
            for (int i = 0; i <= seg; i++) {
                float a = (float)(Math.PI * 2 * i / seg);
                buf.vertex(mat, cx + (float)Math.cos(a) * r, 0f, cz + (float)Math.sin(a) * r)
                   .color(c[0], c[1], c[2], c[3] * 0.3f);
            }
            BufferRenderer.drawWithGlobalProgram(buf.end());
        }
    }

    private void drawSmallCircle(Matrix4f mat, float cx, float cz, float r, int seg, int col) {
        float[] c = unpack(col);

        if (glow.getValue()) {
            int layers = glowIntensity.getValue().intValue();
            float spread = glowSpread.getValue().floatValue();
            float baseWidth = lineWidth.getValue().floatValue();

            for (int layer = layers; layer >= 0; layer--) {
                float t = (float) layer / layers;
                float width = baseWidth + spread * 1.5f * t;
                float a = c[3] * (1f - t * 0.85f);
                if (layer == 0) {
                    a = Math.min(1f, c[3] * 1.5f);
                    width = baseWidth * 0.8f;
                }

                RenderSystem.lineWidth(width);
                Tessellator tess = Tessellator.getInstance();
                BufferBuilder buf = tess.begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);
                for (int i = 0; i < seg; i++) {
                    float a1 = (float)(Math.PI * 2 * i / seg);
                    float a2 = (float)(Math.PI * 2 * (i + 1) / seg);
                    buf.vertex(mat, cx + (float)Math.cos(a1) * r, 0f, cz + (float)Math.sin(a1) * r).color(c[0], c[1], c[2], a);
                    buf.vertex(mat, cx + (float)Math.cos(a2) * r, 0f, cz + (float)Math.sin(a2) * r).color(c[0], c[1], c[2], a);
                }
                BufferRenderer.drawWithGlobalProgram(buf.end());
            }
            RenderSystem.lineWidth(1f);
        } else {
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
    }

    private void drawPolygon(Matrix4f mat, float r, int sides, int col) {
        drawPolygonRotated(mat, r, sides, col, 0f);
    }

    private void drawPentagramShape(Matrix4f mat, float r, int col) {
        float[] c = unpack(col);

        float[][] points = new float[5][2];
        for (int i = 0; i < 5; i++) {
            float a = (float)(Math.PI * 2 * i / 5 - Math.PI / 2);
            points[i][0] = (float)Math.cos(a) * r;
            points[i][1] = (float)Math.sin(a) * r;
        }
        int[] order = {0, 2, 4, 1, 3, 0};

        if (glow.getValue()) {
            int layers = glowIntensity.getValue().intValue();
            float spread = glowSpread.getValue().floatValue();
            float baseWidth = lineWidth.getValue().floatValue();

            for (int layer = layers; layer >= 0; layer--) {
                float t = (float) layer / layers;
                float width = baseWidth + spread * 2f * t;
                float a = c[3] * (1f - t * 0.85f);
                if (layer == 0) {
                    a = Math.min(1f, c[3] * 1.5f);
                    width = baseWidth * 0.8f;
                }

                RenderSystem.lineWidth(width);
                Tessellator tess = Tessellator.getInstance();
                BufferBuilder buf = tess.begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);
                for (int i = 0; i < 5; i++) {
                    int p1 = order[i];
                    int p2 = order[i + 1];
                    buf.vertex(mat, points[p1][0], 0f, points[p1][1]).color(c[0], c[1], c[2], a);
                    buf.vertex(mat, points[p2][0], 0f, points[p2][1]).color(c[0], c[1], c[2], a);
                }
                BufferRenderer.drawWithGlobalProgram(buf.end());
            }
            RenderSystem.lineWidth(1f);
        } else {
            RenderSystem.lineWidth(lineWidth.getValue().floatValue());
            Tessellator tess = Tessellator.getInstance();
            BufferBuilder buf = tess.begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);
            for (int i = 0; i < 5; i++) {
                int p1 = order[i];
                int p2 = order[i + 1];
                buf.vertex(mat, points[p1][0], 0f, points[p1][1]).color(c[0], c[1], c[2], c[3]);
                buf.vertex(mat, points[p2][0], 0f, points[p2][1]).color(c[0], c[1], c[2], c[3]);
            }
            BufferRenderer.drawWithGlobalProgram(buf.end());
            RenderSystem.lineWidth(1f);
        }
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

        if (glow.getValue()) {
            int layers = glowIntensity.getValue().intValue();
            float spread = glowSpread.getValue().floatValue();
            float baseWidth = lineWidth.getValue().floatValue();

            for (int layer = layers; layer >= 0; layer--) {
                float t = (float) layer / layers;
                float width = baseWidth + spread * 2f * t;
                float a = c[3] * (1f - t * 0.85f);
                if (layer == 0) {
                    a = Math.min(1f, c[3] * 1.5f);
                    width = baseWidth * 0.8f;
                }

                RenderSystem.lineWidth(width);
                Tessellator tess = Tessellator.getInstance();
                BufferBuilder buf = tess.begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);
                buf.vertex(mat, x1, y1, z1).color(c[0], c[1], c[2], a);
                buf.vertex(mat, x2, y2, z2).color(c[0], c[1], c[2], a);
                BufferRenderer.drawWithGlobalProgram(buf.end());
            }
            RenderSystem.lineWidth(1f);
        } else {
            RenderSystem.lineWidth(lineWidth.getValue().floatValue());
            Tessellator tess = Tessellator.getInstance();
            BufferBuilder buf = tess.begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);
            buf.vertex(mat, x1, y1, z1).color(c[0], c[1], c[2], c[3]);
            buf.vertex(mat, x2, y2, z2).color(c[0], c[1], c[2], c[3]);
            BufferRenderer.drawWithGlobalProgram(buf.end());
            RenderSystem.lineWidth(1f);
        }
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

    // Прерывистое кольцо (точки)
    private void drawDashedRing(Matrix4f mat, float r, int seg, int col, int dashes) {
        float[] c = unpack(col);
        RenderSystem.lineWidth(lineWidth.getValue().floatValue());
        Tessellator tess = Tessellator.getInstance();
        BufferBuilder buf = tess.begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);

        int segPerDash = Math.max(1, seg / (dashes * 2));
        for (int d = 0; d < dashes; d++) {
            int start = d * segPerDash * 2;
            for (int i = 0; i < segPerDash; i++) {
                int idx = start + i;
                if (idx + 1 > seg) break;
                float a1 = (float)(Math.PI * 2 * idx / seg);
                float a2 = (float)(Math.PI * 2 * (idx + 1) / seg);
                buf.vertex(mat, (float)Math.cos(a1) * r, 0f, (float)Math.sin(a1) * r).color(c[0], c[1], c[2], c[3]);
                buf.vertex(mat, (float)Math.cos(a2) * r, 0f, (float)Math.sin(a2) * r).color(c[0], c[1], c[2], c[3]);
            }
        }
        BufferRenderer.drawWithGlobalProgram(buf.end());
        RenderSystem.lineWidth(1f);
    }

    // Спицы со смещением по углу
    private void drawOffsetSpokes(Matrix4f mat, float rInner, float rOuter, int count, int col, float offset) {
        float[] c = unpack(col);
        RenderSystem.lineWidth(lineWidth.getValue().floatValue());
        Tessellator tess = Tessellator.getInstance();
        BufferBuilder buf = tess.begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);
        for (int i = 0; i < count; i++) {
            float a = (float)(Math.PI * 2 * i / count) + offset;
            float cosA = (float)Math.cos(a);
            float sinA = (float)Math.sin(a);
            buf.vertex(mat, cosA * rInner, 0f, sinA * rInner).color(c[0], c[1], c[2], c[3]);
            buf.vertex(mat, cosA * rOuter, 0f, sinA * rOuter).color(c[0], c[1], c[2], c[3]);
        }
        BufferRenderer.drawWithGlobalProgram(buf.end());
        RenderSystem.lineWidth(1f);
    }

    // Многоугольник с поворотом
    private void drawPolygonRotated(Matrix4f mat, float r, int sides, int col, float rotOffset) {
        float[] c = unpack(col);

        if (glow.getValue()) {
            int layers = glowIntensity.getValue().intValue();
            float spread = glowSpread.getValue().floatValue();
            float baseWidth = lineWidth.getValue().floatValue();

            for (int layer = layers; layer >= 0; layer--) {
                float t = (float) layer / layers;
                float width = baseWidth + spread * 2f * t;
                float a = c[3] * (1f - t * 0.85f);
                if (layer == 0) {
                    a = Math.min(1f, c[3] * 1.5f);
                    width = baseWidth * 0.8f;
                }

                RenderSystem.lineWidth(width);
                Tessellator tess = Tessellator.getInstance();
                BufferBuilder buf = tess.begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);
                for (int i = 0; i < sides; i++) {
                    float a1 = (float)(Math.PI * 2 * i / sides - Math.PI / 2) + rotOffset;
                    float a2 = (float)(Math.PI * 2 * (i + 1) / sides - Math.PI / 2) + rotOffset;
                    buf.vertex(mat, (float)Math.cos(a1) * r, 0f, (float)Math.sin(a1) * r).color(c[0], c[1], c[2], a);
                    buf.vertex(mat, (float)Math.cos(a2) * r, 0f, (float)Math.sin(a2) * r).color(c[0], c[1], c[2], a);
                }
                BufferRenderer.drawWithGlobalProgram(buf.end());
            }
            RenderSystem.lineWidth(1f);
        } else {
            RenderSystem.lineWidth(lineWidth.getValue().floatValue());
            Tessellator tess = Tessellator.getInstance();
            BufferBuilder buf = tess.begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);
            for (int i = 0; i < sides; i++) {
                float a1 = (float)(Math.PI * 2 * i / sides - Math.PI / 2) + rotOffset;
                float a2 = (float)(Math.PI * 2 * (i + 1) / sides - Math.PI / 2) + rotOffset;
                buf.vertex(mat, (float)Math.cos(a1) * r, 0f, (float)Math.sin(a1) * r).color(c[0], c[1], c[2], c[3]);
                buf.vertex(mat, (float)Math.cos(a2) * r, 0f, (float)Math.sin(a2) * r).color(c[0], c[1], c[2], c[3]);
            }
            BufferRenderer.drawWithGlobalProgram(buf.end());
            RenderSystem.lineWidth(1f);
        }
    }

    // Гексаграмма (звезда Давида)
    private void drawHexagram(Matrix4f mat, float r, int col) {
        drawPolygonRotated(mat, r, 3, col, 0f);
        drawPolygonRotated(mat, r, 3, col, (float)Math.PI);
    }

    // Шип (треугольный выступ наружу)
    private void drawSpike(Matrix4f mat, float angle, float rBase, float rTip, float width, int col) {
        float[] c = unpack(col);
        float cosA = (float)Math.cos(angle);
        float sinA = (float)Math.sin(angle);
        // перпендикуляр
        float perpX = -sinA;
        float perpZ = cosA;

        float x1 = cosA * rBase + perpX * width;
        float z1 = sinA * rBase + perpZ * width;
        float x2 = cosA * rBase - perpX * width;
        float z2 = sinA * rBase - perpZ * width;
        float xTip = cosA * rTip;
        float zTip = sinA * rTip;

        // Заполненный треугольник
        Tessellator tess = Tessellator.getInstance();
        BufferBuilder buf = tess.begin(VertexFormat.DrawMode.TRIANGLES, VertexFormats.POSITION_COLOR);
        buf.vertex(mat, xTip, 0f, zTip).color(c[0], c[1], c[2], c[3]);
        buf.vertex(mat, x1, 0f, z1).color(c[0], c[1], c[2], c[3] * 0.4f);
        buf.vertex(mat, x2, 0f, z2).color(c[0], c[1], c[2], c[3] * 0.4f);
        BufferRenderer.drawWithGlobalProgram(buf.end());

        // Контур
        RenderSystem.lineWidth(lineWidth.getValue().floatValue());
        buf = tess.begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);
        buf.vertex(mat, x1, 0f, z1).color(c[0], c[1], c[2], c[3]);
        buf.vertex(mat, xTip, 0f, zTip).color(c[0], c[1], c[2], c[3]);
        buf.vertex(mat, x2, 0f, z2).color(c[0], c[1], c[2], c[3]);
        buf.vertex(mat, xTip, 0f, zTip).color(c[0], c[1], c[2], c[3]);
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
