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
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.item.*;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import org.joml.Matrix4f;


import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

public class ProjectileTrails extends Module {

    private final MinecraftClient mc = MinecraftClient.getInstance();

    // ═══════════════════════════════════════════════════════════════════════════
    //                                SETTINGS
    // ═══════════════════════════════════════════════════════════════════════════

    // === SIMULATION ===
    public final SectionSetting simSection = add(new SectionSetting("Simulation"));
    public final NumberSetting maxIterations = simSection.add(
            new NumberSetting("Max Iterations", 400, 100, 800, 10));
    public final NumberSetting subSteps = simSection.add(
            new NumberSetting("Sub-Steps (Smoothness)", 5, 1, 10, 1));
    public final BooleanSetting showWhenNotUsing = simSection.add(
            new BooleanSetting("Show When Not Using", true));

    // === LINE STYLE ===
    public final SectionSetting lineSection = add(new SectionSetting("Line Style"));
    public final NumberSetting lineWidth = lineSection.add(
            new NumberSetting("Line Width", 2.0, 0.5, 4.0, 0.1));
    public final NumberSetting glowLayers = lineSection.add(
            new NumberSetting("Glow Layers", 3, 0, 5, 1));
    public final NumberSetting glowExpand = lineSection.add(
            new NumberSetting("Glow Expand", 1.8, 1.0, 3.0, 0.1));
    public final BooleanSetting taperEnd = lineSection.add(
            new BooleanSetting("Taper End", true));
    public final BooleanSetting fadeAlpha = lineSection.add(
            new BooleanSetting("Fade Alpha", true));

    // === COLORS ===
    public final SectionSetting colorSection = add(new SectionSetting("Colors"));
    public final ModeSetting colorMode = colorSection.add(
            new ModeSetting("Color Mode", "Gradient", "Gradient", "Rainbow", "Chroma", "Static"));
    public final ColorSetting startColor = colorSection.add(
            new ColorSetting("Start Color", new Color(0, 255, 200, 255)));
    public final ColorSetting endColor = colorSection.add(
            new ColorSetting("End Color", new Color(255, 50, 100, 255)));
    public final ColorSetting staticColor = colorSection.add(
            new ColorSetting("Static Color", new Color(100, 200, 255, 255)));
    public final NumberSetting rainbowSpeed = colorSection.add(
            new NumberSetting("Rainbow/Chroma Speed", 2.0, 0.1, 10.0, 0.1));
    public final NumberSetting chromaSpread = colorSection.add(
            new NumberSetting("Chroma Spread", 0.4, 0.1, 1.0, 0.05));

    // === IMPACT MARKER ===
    public final SectionSetting markerSection = add(new SectionSetting("Impact Marker"));
    public final BooleanSetting showMarker = markerSection.add(
            new BooleanSetting("Show Marker", true));
    public final NumberSetting markerSize = markerSection.add(
            new NumberSetting("Marker Size", 0.5, 0.2, 1.5, 0.05));
    public final NumberSetting markerRings = markerSection.add(
            new NumberSetting("Marker Rings", 3, 1, 5, 1));
    public final BooleanSetting markerPulse = markerSection.add(
            new BooleanSetting("Pulse Animation", true));
    public final BooleanSetting markerGlow = markerSection.add(
            new BooleanSetting("Marker Glow", true));

    // === PROJECTILES ===
    public final SectionSetting projSection = add(new SectionSetting("Projectiles"));
    public final BooleanSetting bowEnabled = projSection.add(new BooleanSetting("Bow/Crossbow", true));
    public final BooleanSetting tridentEnabled = projSection.add(new BooleanSetting("Trident", true));
    public final BooleanSetting pearlEnabled = projSection.add(new BooleanSetting("Ender Pearl", true));
    public final BooleanSetting snowballEnabled = projSection.add(new BooleanSetting("Snowball", true));
    public final BooleanSetting eggEnabled = projSection.add(new BooleanSetting("Egg", true));
    public final BooleanSetting potionEnabled = projSection.add(new BooleanSetting("Splash Potion", true));
    public final BooleanSetting expBottleEnabled = projSection.add(new BooleanSetting("Exp Bottle", true));
    public final BooleanSetting windChargeEnabled = projSection.add(new BooleanSetting("Wind Charge", true));

    // === ENTITY DETECTION ===
    public final SectionSetting entitySection = add(new SectionSetting("Entity Detection"));
    public final BooleanSetting detectEntities = entitySection.add(
            new BooleanSetting("Detect Entities", true));
    public final ColorSetting entityHitColor = entitySection.add(
            new ColorSetting("Entity Hit Color", new Color(255, 0, 60, 255)));

    // === RENDER OPTIONS ===
    public final SectionSetting renderSection = add(new SectionSetting("Render"));
    public final BooleanSetting throughWalls = renderSection.add(
            new BooleanSetting("Through Walls", false));

    // ═══════════════════════════════════════════════════════════════════════════
    //                                 STATE
    // ═══════════════════════════════════════════════════════════════════════════

    private final List<TrailPoint> trajectoryPoints = new ArrayList<>();
    private Vec3d impactPoint = null;
    private Vec3d impactNormal = null;
    private boolean hitEntity = false;

    private static class TrailPoint {
        final Vec3d pos;
        final float progress;

        TrailPoint(Vec3d pos, float progress) {
            this.pos = pos;
            this.progress = progress;
        }
    }

    private enum ProjectileType {
        BOW(3.0f, 0.05f, 0.99f, 0.3f),
        CROSSBOW(3.15f, 0.05f, 0.99f, 0.3f),
        TRIDENT(2.5f, 0.05f, 0.99f, 0.05f),
        ENDER_PEARL(1.5f, 0.03f, 0.99f, 0.25f),
        SNOWBALL(1.5f, 0.03f, 0.99f, 0.25f),
        EGG(1.5f, 0.03f, 0.99f, 0.25f),
        POTION(0.5f, 0.05f, 0.99f, 0.25f),
        EXP_BOTTLE(0.7f, 0.07f, 0.99f, 0.25f),
        WIND_CHARGE(1.5f, 0.0f, 1.0f, 0.3125f);

        final float baseVelocity;
        final float gravity;
        final float drag;
        final float size;

        ProjectileType(float baseVelocity, float gravity, float drag, float size) {
            this.baseVelocity = baseVelocity;
            this.gravity = gravity;
            this.drag = drag;
            this.size = size;
        }
    }

    public ProjectileTrails() {
        super("ProjectileTrails", "Неоновая траектория полёта снарядов", Category.RENDER);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //                              MAIN RENDER
    // ═══════════════════════════════════════════════════════════════════════════

    public void onWorldRender(MatrixStack matrices, float tickDelta) {
        if (!isEnabled()) return;
        if (mc.player == null || mc.world == null) return;

        ProjectileType type = getProjectileType();
        if (type == null) return;

        if (!shouldShowTrajectory(type)) return;

        calculateTrajectory(type, tickDelta);

        if (trajectoryPoints.size() < 2) return;

        Camera cam = mc.gameRenderer.getCamera();
        Vec3d camPos = cam.getPos();

        setupRenderState();

        renderTrailMultiLine(matrices);

        if (showMarker.getValue() && impactPoint != null) {
            renderImpactMarker(matrices, camPos);
        }

        cleanupRenderState();
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //                         TRAJECTORY CALCULATION
    // ═══════════════════════════════════════════════════════════════════════════

    private void calculateTrajectory(ProjectileType type, float tickDelta) {
        trajectoryPoints.clear();
        impactPoint = null;
        impactNormal = null;
        hitEntity = false;

        if (mc.player == null || mc.world == null) return;

        Camera cam = mc.gameRenderer.getCamera();
        Vec3d camPos = cam.getPos();

        float yaw = mc.player.getYaw(tickDelta);
        float pitch = mc.player.getPitch(tickDelta);

        float yawRad = yaw * 0.017453292F;
        float pitchRad = pitch * 0.017453292F;

        double vx = -MathHelper.sin(yawRad) * MathHelper.cos(pitchRad);
        double vy = -MathHelper.sin(pitchRad);
        double vz =  MathHelper.cos(yawRad) * MathHelper.cos(pitchRad);

        Vec3d direction = new Vec3d(vx, vy, vz).normalize();

        Vec3d right = new Vec3d(Math.cos(yawRad), 0.0, Math.sin(yawRad)).normalize();

        Vec3d handOffset = right.multiply(0.18)
                .add(0.0, -0.12, 0.0)
                .add(direction.multiply(0.16));

        float speed = getInitialVelocity(type);
        if (speed <= 0.001f) return;

        Vec3d velocity = direction.multiply(speed);
        Vec3d worldPos = camPos.add(handOffset);

        int iterations = maxIterations.getValue().intValue();
        int sub = subSteps.getValue().intValue();

        double subDrag = Math.pow(type.drag, 1.0D / sub);
        double subGravity = type.gravity / sub;

        int totalSteps = Math.max(1, iterations * sub);
        trajectoryPoints.add(new TrailPoint(worldPos.subtract(camPos), 0.0f));

        for (int i = 0; i < iterations; i++) {
            for (int s = 0; s < sub; s++) {
                int stepIndex = i * sub + s + 1;

                Vec3d stepVelocity = velocity.multiply(1.0D / sub);
                Vec3d nextWorldPos = worldPos.add(stepVelocity);

                BlockHitResult blockHit = mc.world.raycast(new RaycastContext(
                        worldPos,
                        nextWorldPos,
                        RaycastContext.ShapeType.COLLIDER,
                        RaycastContext.FluidHandling.NONE,
                        mc.player
                ));

                EntityHitResult entityHit = null;
                if (detectEntities.getValue()) {
                    Box searchBox = new Box(worldPos, nextWorldPos).expand(type.size);
                    entityHit = ProjectileUtil.raycast(
                            mc.player,
                            worldPos,
                            nextWorldPos,
                            searchBox,
                            entity -> entity != mc.player && !entity.isSpectator() && entity.canHit(),
                            worldPos.squaredDistanceTo(nextWorldPos)
                    );
                }

                boolean hitBlock = blockHit.getType() != HitResult.Type.MISS;
                boolean hitEnt = entityHit != null;

                if (hitBlock || hitEnt) {
                    Vec3d hitPos;

                    if (hitBlock && hitEnt) {
                        double blockDist = worldPos.squaredDistanceTo(blockHit.getPos());
                        double entityDist = worldPos.squaredDistanceTo(entityHit.getPos());

                        if (entityDist < blockDist) {
                            hitPos = entityHit.getPos();
                            hitEntity = true;
                        } else {
                            hitPos = blockHit.getPos();
                            hitEntity = false;
                            impactNormal = Vec3d.of(blockHit.getSide().getVector());
                        }
                    } else if (hitEnt) {
                        hitPos = entityHit.getPos();
                        hitEntity = true;
                    } else {
                        hitPos = blockHit.getPos();
                        hitEntity = false;
                        impactNormal = Vec3d.of(blockHit.getSide().getVector());
                    }

                    float progress = Math.min(1.0f, stepIndex / (float) totalSteps);
                    Vec3d localHit = hitPos.subtract(camPos);
                    trajectoryPoints.add(new TrailPoint(localHit, progress));
                    impactPoint = localHit;
                    return;
                }

                worldPos = nextWorldPos;

                float progress = Math.min(1.0f, stepIndex / (float) totalSteps);
                trajectoryPoints.add(new TrailPoint(worldPos.subtract(camPos), progress));

                velocity = velocity.multiply(subDrag).subtract(0.0, subGravity, 0.0);

                if (worldPos.y < mc.world.getBottomY() - 64 || worldPos.y > mc.world.getTopYInclusive() + 64) {
                    impactPoint = worldPos.subtract(camPos);
                    return;
                }
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //                      RIBBON RENDERING (MULTI-PASS LINES)
    // ═══════════════════════════════════════════════════════════════════════════

    private void renderTrailMultiLine(MatrixStack matrices) {
        if (trajectoryPoints.size() < 2) return;

        Matrix4f mat = matrices.peek().getPositionMatrix();
        Tessellator tessellator = Tessellator.getInstance();

        float baseLineWidth = lineWidth.getValue().floatValue();
        int glowPassCount = glowLayers.getValue().intValue();

        if (glowPassCount > 0) {
            float glowExp = glowExpand.getValue().floatValue();

            for (int glowLayer = glowPassCount; glowLayer >= 1; glowLayer--) {
                float t = (float) glowLayer / glowPassCount;

                float glowLineWidth = baseLineWidth * (1.0f + t * (glowExp - 1.0f));
                float glowAlphaMult = 0.12f / glowLayer;

                double spread = 0.005 * glowLayer;
                int glowPasses = 5;

                RenderSystem.lineWidth(glowLineWidth);

                for (int pass = 0; pass < glowPasses; pass++) {
                    double angle = Math.PI * 2.0 * pass / glowPasses;
                    double offX = Math.cos(angle) * spread;
                    double offY = Math.sin(angle) * spread;

                    BufferBuilder buffer = tessellator.begin(
                            VertexFormat.DrawMode.DEBUG_LINE_STRIP,
                            VertexFormats.POSITION_COLOR
                    );

                    for (int i = 0; i < trajectoryPoints.size(); i++) {
                        TrailPoint tp = trajectoryPoints.get(i);
                        Vec3d p = tp.pos;
                        float progress = tp.progress;

                        float[] color = getTrailColor(progress);
                        float fade = getTrailFade(progress);
                        float alpha = color[3] * glowAlphaMult * fade;

                        buffer.vertex(mat,
                                (float) (p.x + offX),
                                (float) (p.y + offY),
                                (float) p.z
                        ).color(color[0], color[1], color[2], alpha);
                    }

                    BufferRenderer.drawWithGlobalProgram(buffer.end());
                }
            }
        }

        RenderSystem.lineWidth(baseLineWidth);

        double maxOffset = 0.018;
        double step = maxOffset / 3.0;

        for (double offX = -maxOffset; offX <= maxOffset + 0.001; offX += step) {
            for (double offY = -maxOffset; offY <= maxOffset + 0.001; offY += step) {
                boolean isCross = Math.abs(offX) < 0.001 || Math.abs(offY) < 0.001;
                if (!isCross) continue;

                float centerAlpha = (Math.abs(offX) < 0.001 && Math.abs(offY) < 0.001)
                        ? 1.0f
                        : 0.6f;

                BufferBuilder buffer = tessellator.begin(
                        VertexFormat.DrawMode.DEBUG_LINE_STRIP,
                        VertexFormats.POSITION_COLOR
                );

                for (int i = 0; i < trajectoryPoints.size(); i++) {
                    TrailPoint tp = trajectoryPoints.get(i);
                    Vec3d p = tp.pos;
                    float progress = tp.progress;

                    float[] color = getTrailColor(progress);
                    float fade = getTrailFade(progress);
                    float alpha = color[3] * centerAlpha * fade;

                    buffer.vertex(mat,
                            (float) (p.x + offX),
                            (float) (p.y + offY),
                            (float) p.z
                    ).color(color[0], color[1], color[2], alpha);
                }

                BufferRenderer.drawWithGlobalProgram(buffer.end());
            }
        }

        RenderSystem.lineWidth(1.0f);
    }

    private float getTrailFade(float progress) {
        if (!fadeAlpha.getValue()) {
            return 1.0f;
        }

        float fade = 1.0f - progress;
        fade = fade * fade;

        return MathHelper.clamp(fade, 0.0f, 1.0f);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //                            IMPACT MARKER
    // ═══════════════════════════════════════════════════════════════════════════

    private void renderImpactMarker(MatrixStack matrices, Vec3d camPos) {
        if (impactPoint == null) return;

        float baseSize = markerSize.getValue().floatValue();
        int rings = markerRings.getValue().intValue();

        float pulse = 1.0f;
        if (markerPulse.getValue()) {
            double time = System.currentTimeMillis() / 120.0;
            pulse = 0.85f + (float) Math.sin(time) * 0.15f;
        }

        float[] color;
        if (hitEntity) {
            Color c = entityHitColor.getValue();
            color = new float[]{
                    c.getRed() / 255f, c.getGreen() / 255f,
                    c.getBlue() / 255f, c.getAlpha() / 255f
            };
        } else {
            color = getTrailColor(1.0f);
        }

        matrices.push();
        matrices.translate(impactPoint.x, impactPoint.y + 0.02, impactPoint.z);

        Matrix4f mat = matrices.peek().getPositionMatrix();
        Tessellator tess = Tessellator.getInstance();

        if (markerGlow.getValue()) {
            for (int g = 3; g >= 1; g--) {
                float glowSize = baseSize * pulse * (1.0f + g * 0.4f);
                float glowAlpha = color[3] * 0.08f / g;
                renderMarkerRing(mat, tess, glowSize, color, glowAlpha, 32);
            }
        }

        for (int r = 0; r < rings; r++) {
            float ringProgress = (float) r / rings;
            float ringSize = baseSize * pulse * (0.3f + ringProgress * 0.7f);
            float ringAlpha = color[3] * (1.0f - ringProgress * 0.5f);

            renderMarkerRing(mat, tess, ringSize, color, ringAlpha, 48);
        }

        renderMarkerCore(mat, tess, baseSize * 0.15f * pulse, color);

        renderMarkerCross(mat, tess, baseSize * pulse * 0.8f, color);

        matrices.pop();
    }

    private void renderMarkerRing(Matrix4f mat, Tessellator tess, float radius,
                                  float[] color, float alpha, int segments) {
        BufferBuilder fillBuf = tess.begin(VertexFormat.DrawMode.TRIANGLE_FAN, VertexFormats.POSITION_COLOR);
        fillBuf.vertex(mat, 0, 0.02f, 0).color(color[0], color[1], color[2], alpha * 0.5f);

        for (int i = 0; i <= segments; i++) {
            float angle = (float) (Math.PI * 2 * i / segments);
            float x = (float) Math.cos(angle) * radius;
            float z = (float) Math.sin(angle) * radius;
            fillBuf.vertex(mat, x, 0.02f, z).color(color[0], color[1], color[2], 0f);
        }
        BufferRenderer.drawWithGlobalProgram(fillBuf.end());

        float ringWidth = radius * 0.08f;
        BufferBuilder ringBuf = tess.begin(VertexFormat.DrawMode.TRIANGLE_STRIP, VertexFormats.POSITION_COLOR);

        for (int i = 0; i <= segments; i++) {
            float angle = (float) (Math.PI * 2 * i / segments);
            float cos = (float) Math.cos(angle);
            float sin = (float) Math.sin(angle);

            float xInner = cos * (radius - ringWidth);
            float zInner = sin * (radius - ringWidth);
            float xOuter = cos * (radius + ringWidth);
            float zOuter = sin * (radius + ringWidth);

            ringBuf.vertex(mat, xInner, 0.02f, zInner).color(color[0], color[1], color[2], alpha);
            ringBuf.vertex(mat, xOuter, 0.02f, zOuter).color(color[0], color[1], color[2], alpha * 0.3f);
        }
        BufferRenderer.drawWithGlobalProgram(ringBuf.end());
    }

    private void renderMarkerCore(Matrix4f mat, Tessellator tess, float size, float[] color) {
        int segments = 16;

        BufferBuilder buf = tess.begin(VertexFormat.DrawMode.TRIANGLE_FAN, VertexFormats.POSITION_COLOR);
        buf.vertex(mat, 0, 0.03f, 0).color(color[0], color[1], color[2], Math.min(1f, color[3] * 1.5f));

        for (int i = 0; i <= segments; i++) {
            float angle = (float) (Math.PI * 2 * i / segments);
            float x = (float) Math.cos(angle) * size;
            float z = (float) Math.sin(angle) * size;
            buf.vertex(mat, x, 0.03f, z).color(color[0], color[1], color[2], 0f);
        }
        BufferRenderer.drawWithGlobalProgram(buf.end());
    }

    private void renderMarkerCross(Matrix4f mat, Tessellator tess, float size, float[] color) {
        float width = size * 0.04f;
        float alpha = color[3] * 0.8f;

        BufferBuilder buf = tess.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);

        buf.vertex(mat, -size, 0.025f, -width).color(color[0], color[1], color[2], 0f);
        buf.vertex(mat, -size, 0.025f, width).color(color[0], color[1], color[2], 0f);
        buf.vertex(mat, 0, 0.025f, width).color(color[0], color[1], color[2], alpha);
        buf.vertex(mat, 0, 0.025f, -width).color(color[0], color[1], color[2], alpha);

        buf.vertex(mat, 0, 0.025f, -width).color(color[0], color[1], color[2], alpha);
        buf.vertex(mat, 0, 0.025f, width).color(color[0], color[1], color[2], alpha);
        buf.vertex(mat, size, 0.025f, width).color(color[0], color[1], color[2], 0f);
        buf.vertex(mat, size, 0.025f, -width).color(color[0], color[1], color[2], 0f);

        buf.vertex(mat, -width, 0.025f, -size).color(color[0], color[1], color[2], 0f);
        buf.vertex(mat, width, 0.025f, -size).color(color[0], color[1], color[2], 0f);
        buf.vertex(mat, width, 0.025f, 0).color(color[0], color[1], color[2], alpha);
        buf.vertex(mat, -width, 0.025f, 0).color(color[0], color[1], color[2], alpha);

        buf.vertex(mat, -width, 0.025f, 0).color(color[0], color[1], color[2], alpha);
        buf.vertex(mat, width, 0.025f, 0).color(color[0], color[1], color[2], alpha);
        buf.vertex(mat, width, 0.025f, size).color(color[0], color[1], color[2], 0f);
        buf.vertex(mat, -width, 0.025f, size).color(color[0], color[1], color[2], 0f);

        BufferRenderer.drawWithGlobalProgram(buf.end());
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //                               COLORS
    // ═══════════════════════════════════════════════════════════════════════════

    private float[] getTrailColor(float progress) {
        if (hitEntity && progress > 0.9f) {
            Color c = entityHitColor.getValue();
            return new float[]{c.getRed() / 255f, c.getGreen() / 255f, c.getBlue() / 255f, c.getAlpha() / 255f};
        }

        switch (colorMode.getValue()) {
            case "Gradient" -> {
                Color start = startColor.getValue();
                Color end = endColor.getValue();

                float r = MathHelper.lerp(progress, start.getRed() / 255f, end.getRed() / 255f);
                float g = MathHelper.lerp(progress, start.getGreen() / 255f, end.getGreen() / 255f);
                float b = MathHelper.lerp(progress, start.getBlue() / 255f, end.getBlue() / 255f);
                float a = MathHelper.lerp(progress, start.getAlpha() / 255f, end.getAlpha() / 255f);

                return new float[]{r, g, b, a};
            }

            case "Rainbow" -> {
                float speed = rainbowSpeed.getValue().floatValue();
                float time = (System.currentTimeMillis() % 10000L) / 10000f;
                float hue = (time * speed + progress * 0.5f) % 1f;

                int rgb = Color.HSBtoRGB(hue, 0.9f, 1f);
                return new float[]{
                        ((rgb >> 16) & 0xFF) / 255f,
                        ((rgb >> 8) & 0xFF) / 255f,
                        (rgb & 0xFF) / 255f,
                        1f
                };
            }

            case "Chroma" -> {
                float speed = rainbowSpeed.getValue().floatValue();
                float spread = chromaSpread.getValue().floatValue();
                float time = (System.currentTimeMillis() % 10000L) / 10000f;
                float hue = (time * speed + progress * spread) % 1f;

                int rgb = Color.HSBtoRGB(hue, 0.85f, 1f);
                return new float[]{
                        ((rgb >> 16) & 0xFF) / 255f,
                        ((rgb >> 8) & 0xFF) / 255f,
                        (rgb & 0xFF) / 255f,
                        1f
                };
            }

            case "Static" -> {
                Color c = staticColor.getValue();
                return new float[]{c.getRed() / 255f, c.getGreen() / 255f, c.getBlue() / 255f, c.getAlpha() / 255f};
            }
        }

        return new float[]{1f, 1f, 1f, 1f};
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //                          PROJECTILE DETECTION
    // ═══════════════════════════════════════════════════════════════════════════

    private ProjectileType getProjectileType() {
        ItemStack mainHand = mc.player.getMainHandStack();
        ItemStack offHand = mc.player.getOffHandStack();

        ProjectileType type = getTypeFromStack(mainHand);
        if (type != null) return type;

        return getTypeFromStack(offHand);
    }

    private ProjectileType getTypeFromStack(ItemStack stack) {
        if (stack.isEmpty()) return null;
        Item item = stack.getItem();

        if (item instanceof BowItem && bowEnabled.getValue()) return ProjectileType.BOW;
        if (item instanceof CrossbowItem && bowEnabled.getValue()) {
            return CrossbowItem.isCharged(stack) ? ProjectileType.CROSSBOW : null;
        }
        if (item instanceof TridentItem && tridentEnabled.getValue()) return ProjectileType.TRIDENT;
        if (item instanceof EnderPearlItem && pearlEnabled.getValue()) return ProjectileType.ENDER_PEARL;
        if (item instanceof SnowballItem && snowballEnabled.getValue()) return ProjectileType.SNOWBALL;
        if (item instanceof EggItem && eggEnabled.getValue()) return ProjectileType.EGG;
        if ((item instanceof SplashPotionItem || item instanceof LingeringPotionItem)
                && potionEnabled.getValue()) return ProjectileType.POTION;
        if (item instanceof ExperienceBottleItem && expBottleEnabled.getValue()) return ProjectileType.EXP_BOTTLE;
        if (item instanceof WindChargeItem && windChargeEnabled.getValue()) return ProjectileType.WIND_CHARGE;

        return null;
    }

    private boolean shouldShowTrajectory(ProjectileType type) {
        if (showWhenNotUsing.getValue()) return true;

        if (type == ProjectileType.BOW) {
            return mc.player.isUsingItem() && mc.player.getActiveItem().getItem() instanceof BowItem;
        }
        if (type == ProjectileType.TRIDENT) {
            return mc.player.isUsingItem() && mc.player.getActiveItem().getItem() instanceof TridentItem;
        }
        if (type == ProjectileType.CROSSBOW) {
            ItemStack stack = mc.player.getMainHandStack();
            if (!(stack.getItem() instanceof CrossbowItem)) stack = mc.player.getOffHandStack();
            return stack.getItem() instanceof CrossbowItem && CrossbowItem.isCharged(stack);
        }

        return true;
    }

    private float getInitialVelocity(ProjectileType type) {
        if (type == ProjectileType.BOW) {
            if (mc.player.isUsingItem() && mc.player.getActiveItem().getItem() instanceof BowItem) {
                int useTicks = mc.player.getItemUseTime();
                float pull = BowItem.getPullProgress(useTicks);
                return Math.max(pull * 3.0f, 0.1f);
            }
            return showWhenNotUsing.getValue() ? 3.0f : 0f;
        }

        if (type == ProjectileType.TRIDENT) {
            if (mc.player.isUsingItem() && mc.player.getActiveItem().getItem() instanceof TridentItem) {
                int useTicks = mc.player.getItemUseTime();
                float charge = Math.min(useTicks / 10.0f, 1.0f);
                return type.baseVelocity * Math.max(charge, 0.3f);
            }
            return showWhenNotUsing.getValue() ? type.baseVelocity : 0f;
        }

        return type.baseVelocity;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //                            RENDER STATE
    // ═══════════════════════════════════════════════════════════════════════════

    private void setupRenderState() {
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(
                com.mojang.blaze3d.platform.GlStateManager.SrcFactor.SRC_ALPHA,
                com.mojang.blaze3d.platform.GlStateManager.DstFactor.ONE
        );
        RenderSystem.disableCull();
        if (throughWalls.getValue()) {
            RenderSystem.disableDepthTest();
        }
        RenderSystem.depthMask(false);
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
    }

    private void cleanupRenderState() {
        RenderSystem.depthMask(true);
        if (throughWalls.getValue()) {
            RenderSystem.enableDepthTest();
        }
        RenderSystem.enableCull();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
    }
}
