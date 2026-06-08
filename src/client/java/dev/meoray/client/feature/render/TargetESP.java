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
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

public class TargetESP extends Module {

    private final MinecraftClient mc = MinecraftClient.getInstance();

    // === MODE ===
    public final SectionSetting modeSection = add(new SectionSetting("Mode"));
    public final ModeSetting mode = modeSection.add(new ModeSetting("Mode", "Jello",
            "Ghosts", "Swords", "Jello", "Vortex"));

    // === COMMON ===
    public final SectionSetting commonSection = add(new SectionSetting("Common"));
    public final NumberSetting lineWidth = commonSection.add(new NumberSetting("Line Width", 1.5, 0.5, 4.0, 0.1));
    public final BooleanSetting throughWalls = commonSection.add(new BooleanSetting("Through Walls", true));
    public final BooleanSetting glow = commonSection.add(new BooleanSetting("Glow", true));
    public final NumberSetting glowIntensity = commonSection.add(new NumberSetting("Glow Intensity", 3, 1, 5, 1));

    // === COLOR ===
    public final SectionSetting colorSection = add(new SectionSetting("Color"));
    public final ColorSetting primaryColor = colorSection.add(new ColorSetting("Primary", new Color(220, 50, 255, 230)));
    public final ColorSetting secondaryColor = colorSection.add(new ColorSetting("Secondary", new Color(80, 220, 255, 230)));
    public final BooleanSetting rainbow = colorSection.add(new BooleanSetting("Rainbow", false));
    public final NumberSetting rainbowSpeed = colorSection.add(new NumberSetting("Rainbow Speed", 2.0, 0.1, 10.0, 0.1));

    // === GHOSTS ===
    public final SectionSetting ghostsSection = add(new SectionSetting("Ghosts"));
    public final NumberSetting ghostOrbCount = ghostsSection.add(new NumberSetting("Orb Count", 4, 2, 6, 1));
    public final NumberSetting ghostOrbitRadius = ghostsSection.add(new NumberSetting("Orbit Radius", 1.0, 0.5, 2.0, 0.05));
    public final NumberSetting ghostOrbitSpeed = ghostsSection.add(new NumberSetting("Orbit Speed", 2.0, 0.5, 6.0, 0.1));
    public final NumberSetting ghostOrbSize = ghostsSection.add(new NumberSetting("Orb Size", 0.12, 0.04, 0.3, 0.01));
    public final NumberSetting ghostWaveAmplitude = ghostsSection.add(new NumberSetting("Wave Amplitude", 0.4, 0.1, 1.0, 0.05));
    public final NumberSetting ghostWaveSpeed = ghostsSection.add(new NumberSetting("Wave Speed", 3.0, 0.5, 8.0, 0.1));
    public final NumberSetting ghostTrailLength = ghostsSection.add(new NumberSetting("Trail Length", 18, 5, 40, 1));
    public final NumberSetting ghostTrailSpacing = ghostsSection.add(new NumberSetting("Trail Spacing", 0.03, 0.01, 0.1, 0.005));
    public final BooleanSetting ghostElliptical = ghostsSection.add(new BooleanSetting("Elliptical Orbit", false));
    public final NumberSetting ghostEllipseRatio = ghostsSection.add(new NumberSetting("Ellipse Ratio", 0.7, 0.3, 1.0, 0.05));

    // === SWORDS ===
    public final SectionSetting swordsSection = add(new SectionSetting("Swords"));
    public final NumberSetting swordCount = swordsSection.add(new NumberSetting("Sword Count", 4, 2, 6, 1));
    public final NumberSetting swordSize = swordsSection.add(new NumberSetting("Sword Size", 0.7, 0.3, 1.5, 0.05));
    public final NumberSetting swordOrbitRadius = swordsSection.add(new NumberSetting("Orbit Radius", 1.2, 0.5, 2.5, 0.05));
    public final NumberSetting swordRotSpeed = swordsSection.add(new NumberSetting("Rotation Speed", 120, 10, 360, 5));
    public final NumberSetting swordTiltAngle = swordsSection.add(new NumberSetting("Tilt Angle", 15, 0, 45, 1));
    public final NumberSetting swordBobSpeed = swordsSection.add(new NumberSetting("Bob Speed", 2.5, 0.5, 6.0, 0.1));
    public final NumberSetting swordBobAmount = swordsSection.add(new NumberSetting("Bob Amount", 0.08, 0.0, 0.3, 0.01));
    public final BooleanSetting swordBob = swordsSection.add(new BooleanSetting("Bob Up/Down", true));
    public final NumberSetting swordBladeThickness = swordsSection.add(new NumberSetting("Blade Thickness", 0.025, 0.01, 0.06, 0.005));
    public final BooleanSetting swordGlowTrail = swordsSection.add(new BooleanSetting("Glow Aura", true));

    // === JELLO ===
    public final SectionSetting jelloSection = add(new SectionSetting("Jello"));
    public final NumberSetting jelloRadius = jelloSection.add(new NumberSetting("Radius", 1.0, 0.3, 3.0, 0.05));
    public final NumberSetting jelloPulse = jelloSection.add(new NumberSetting("Pulse Amount", 0.15, 0.0, 0.5, 0.01));
    public final NumberSetting jelloSpeed = jelloSection.add(new NumberSetting("Pulse Speed", 2.5, 0.5, 6.0, 0.1));
    public final BooleanSetting jelloFilled = jelloSection.add(new BooleanSetting("Filled", true));

    // === VORTEX ===
    public final SectionSetting vortexSection = add(new SectionSetting("Vortex"));
    public final NumberSetting vortexRadius = vortexSection.add(new NumberSetting("Radius", 0.7, 0.2, 2.0, 0.05));
    public final NumberSetting vortexTurns = vortexSection.add(new NumberSetting("Turns", 4, 1, 10, 1));
    public final NumberSetting vortexSegments = vortexSection.add(new NumberSetting("Segments", 60, 20, 200, 1));
    public final NumberSetting vortexRotSpeed = vortexSection.add(new NumberSetting("Rotation Speed", 90, 0, 360, 1));
    public final BooleanSetting vortexDouble = vortexSection.add(new BooleanSetting("Double Helix", true));

    // === ANIMATION ===
    public final SectionSetting animSection = add(new SectionSetting("Animation"));
    public final NumberSetting fadeSpeed = animSection.add(new NumberSetting("Fade Speed", 0.08, 0.01, 0.3, 0.01));

    // === STATE ===
    private LivingEntity currentTarget = null;
    private float alpha = 0f;
    private float globalRotation = 0f;

    public TargetESP() {
        super("TargetESP", "Подсветка цели AttackAura", Category.RENDER);
    }

    private LivingEntity getAuraTarget() {
        try {
            if (dev.meoray.client.feature.combat.AttackAura.INSTANCE != null) {
                return dev.meoray.client.feature.combat.AttackAura.INSTANCE.getTarget();
            }
        } catch (Throwable ignored) {}
        return null;
    }

    @Override
    public void onTick() {
        globalRotation += 1f;
        if (globalRotation > 360f) globalRotation -= 360f;

        LivingEntity target = getAuraTarget();
        if (target != null && !target.isAlive()) target = null;

        if (target != null) {
            currentTarget = target;
            alpha = Math.min(1f, alpha + fadeSpeed.getValue().floatValue());
        } else {
            alpha = Math.max(0f, alpha - fadeSpeed.getValue().floatValue());
            if (alpha <= 0.001f) currentTarget = null;
        }
    }

    public void onWorldRender(MatrixStack matrices, float tickDelta) {
        if (!isEnabled()) return;
        if (mc.world == null || mc.player == null) return;
        if (currentTarget == null || alpha <= 0.01f) return;

        Camera cam = mc.gameRenderer.getCamera();
        if (cam == null) return;
        Vec3d camPos = cam.getPos();

        double tx = MathHelper.lerp((double) tickDelta, currentTarget.lastRenderX, currentTarget.getX());
        double ty = MathHelper.lerp((double) tickDelta, currentTarget.lastRenderY, currentTarget.getY());
        double tz = MathHelper.lerp((double) tickDelta, currentTarget.lastRenderZ, currentTarget.getZ());

        double rx = tx - camPos.x;
        double ry = ty - camPos.y;
        double rz = tz - camPos.z;

        float entHeight = currentTarget.getHeight();
        float entWidth = currentTarget.getWidth();

        RenderSystem.enableBlend();
        RenderSystem.blendFunc(
                com.mojang.blaze3d.platform.GlStateManager.SrcFactor.SRC_ALPHA,
                com.mojang.blaze3d.platform.GlStateManager.DstFactor.ONE);
        RenderSystem.disableCull();
        if (throughWalls.getValue()) RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);

        switch (mode.getValue()) {
            case "Ghosts" -> renderGhosts(matrices, rx, ry, rz, entHeight, entWidth, tickDelta);
            case "Swords" -> renderSwords(matrices, rx, ry, rz, entHeight, entWidth, tickDelta);
            case "Jello" -> renderJello(matrices, rx, ry, rz, entWidth);
            case "Vortex" -> renderVortex(matrices, rx, ry, rz, entHeight, entWidth);
        }

        RenderSystem.depthMask(true);
        if (throughWalls.getValue()) RenderSystem.enableDepthTest();
        RenderSystem.enableCull();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
    }

    // ==================================================================================
    //                            GHOSTS — Орбитальные призрачные сферы
    // ==================================================================================

    private void renderGhosts(MatrixStack matrices, double rx, double ry, double rz,
                              float entHeight, float entWidth, float tickDelta) {
        int orbCount = ghostOrbCount.getValue().intValue();
        float orbitRadius = ghostOrbitRadius.getValue().floatValue();
        float orbitSpeed = ghostOrbitSpeed.getValue().floatValue();
        float orbSize = ghostOrbSize.getValue().floatValue();
        float waveAmp = ghostWaveAmplitude.getValue().floatValue();
        float waveSpeed = ghostWaveSpeed.getValue().floatValue();
        int trailLen = ghostTrailLength.getValue().intValue();
        float trailSpacing = ghostTrailSpacing.getValue().floatValue();
        boolean elliptical = ghostElliptical.getValue();
        float ellipseRatio = ghostEllipseRatio.getValue().floatValue();

        double timeSeconds = System.currentTimeMillis() / 1000.0;

        float centerY = entHeight * 0.5f;

        matrices.push();
        matrices.translate(rx, ry, rz);

        for (int orb = 0; orb < orbCount; orb++) {
            float orbOffset = (float) (Math.PI * 2.0 * orb / orbCount);
            float colorOffset = (float) orb / orbCount;

            List<float[]> trailPositions = new ArrayList<>(trailLen + 1);

            for (int t = trailLen; t >= 0; t--) {
                double trailTime = timeSeconds - t * trailSpacing;

                double angle = trailTime * orbitSpeed * Math.PI * 2.0 + orbOffset;

                float radiusX = orbitRadius;
                float radiusZ = elliptical ? orbitRadius * ellipseRatio : orbitRadius;

                float posX = (float) (Math.sin(angle) * radiusX);
                float posZ = (float) (Math.cos(angle) * radiusZ);

                float wavePhase = (float) (trailTime * waveSpeed * Math.PI * 2.0 + orbOffset * 1.5);
                float posY = centerY + (float) Math.sin(wavePhase) * waveAmp;

                trailPositions.add(new float[]{posX, posY, posZ});
            }

            renderGhostTrail(matrices, trailPositions, orbSize, colorOffset);

            float[] currentPos = trailPositions.get(trailPositions.size() - 1);
            renderGhostOrb(matrices, currentPos[0], currentPos[1], currentPos[2],
                    orbSize, colorOffset);
        }

        matrices.pop();
    }

    private void renderGhostTrail(MatrixStack matrices, List<float[]> positions,
                                  float orbSize, float colorOffset) {
        if (positions.size() < 2) return;

        Camera cam = mc.gameRenderer.getCamera();
        Vec3d camDir = new Vec3d(
                -Math.sin(Math.toRadians(cam.getYaw())) * Math.cos(Math.toRadians(cam.getPitch())),
                -Math.sin(Math.toRadians(cam.getPitch())),
                Math.cos(Math.toRadians(cam.getYaw())) * Math.cos(Math.toRadians(cam.getPitch()))
        );

        int total = positions.size();
        Matrix4f mat = matrices.peek().getPositionMatrix();

        Tessellator tess = Tessellator.getInstance();
        BufferBuilder buf = tess.begin(VertexFormat.DrawMode.TRIANGLE_STRIP, VertexFormats.POSITION_COLOR);

        for (int i = 0; i < total; i++) {
            float progress = (float) i / (total - 1);
            float[] pos = positions.get(i);

            float[] next = (i < total - 1) ? positions.get(i + 1) : positions.get(i);
            float[] prev = (i > 0) ? positions.get(i - 1) : positions.get(i);
            float dx = next[0] - prev[0];
            float dy = next[1] - prev[1];
            float dz = next[2] - prev[2];
            float len = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (len < 0.0001f) len = 1f;
            dx /= len; dy /= len; dz /= len;

            float cx = (float) camDir.x;
            float cy = (float) camDir.y;
            float cz = (float) camDir.z;

            float perpX = dy * cz - dz * cy;
            float perpY = dz * cx - dx * cz;
            float perpZ = dx * cy - dy * cx;
            float perpLen = (float) Math.sqrt(perpX * perpX + perpY * perpY + perpZ * perpZ);
            if (perpLen < 0.0001f) {
                perpX = dy * 1f - dz * 0f;
                perpY = dz * 0f - dx * 1f;
                perpZ = dx * 0f - dy * 0f;
                perpLen = (float) Math.sqrt(perpX * perpX + perpY * perpY + perpZ * perpZ);
                if (perpLen < 0.0001f) { perpX = 1f; perpY = 0f; perpZ = 0f; perpLen = 1f; }
            }
            perpX /= perpLen; perpY /= perpLen; perpZ /= perpLen;

            float width = orbSize * 0.7f * progress;

            float trailAlpha = progress * progress * alpha * 0.8f;

            int col = getColor(colorOffset + (1f - progress) * 0.3f);
            float[] c = unpack(col);

            float ax = pos[0] + perpX * width;
            float ay = pos[1] + perpY * width;
            float az = pos[2] + perpZ * width;

            float bx = pos[0] - perpX * width;
            float by = pos[1] - perpY * width;
            float bz = pos[2] - perpZ * width;

            buf.vertex(mat, ax, ay, az).color(c[0], c[1], c[2], trailAlpha);
            buf.vertex(mat, bx, by, bz).color(c[0], c[1], c[2], trailAlpha * 0.3f);
        }

        BufferRenderer.drawWithGlobalProgram(buf.end());

        if (glow.getValue()) {
            int layers = Math.min(glowIntensity.getValue().intValue(), 3);
            for (int layer = 1; layer <= layers; layer++) {
                float layerScale = 1f + layer * 0.5f;
                float layerAlphaScale = 1f / (1f + layer * 1.5f);

                BufferBuilder glowBuf = tess.begin(VertexFormat.DrawMode.TRIANGLE_STRIP, VertexFormats.POSITION_COLOR);

                for (int i = 0; i < total; i++) {
                    float progress = (float) i / (total - 1);
                    float[] pos = positions.get(i);

                    float[] next2 = (i < total - 1) ? positions.get(i + 1) : positions.get(i);
                    float[] prev2 = (i > 0) ? positions.get(i - 1) : positions.get(i);
                    float dx2 = next2[0] - prev2[0];
                    float dy2 = next2[1] - prev2[1];
                    float dz2 = next2[2] - prev2[2];
                    float len2 = (float) Math.sqrt(dx2 * dx2 + dy2 * dy2 + dz2 * dz2);
                    if (len2 < 0.0001f) len2 = 1f;
                    dx2 /= len2; dy2 /= len2; dz2 /= len2;

                    float cx2 = (float) camDir.x;
                    float cy2 = (float) camDir.y;
                    float cz2 = (float) camDir.z;

                    float pX = dy2 * cz2 - dz2 * cy2;
                    float pY = dz2 * cx2 - dx2 * cz2;
                    float pZ = dx2 * cy2 - dy2 * cx2;
                    float pL = (float) Math.sqrt(pX * pX + pY * pY + pZ * pZ);
                    if (pL < 0.0001f) { pX = 1f; pY = 0; pZ = 0; pL = 1f; }
                    pX /= pL; pY /= pL; pZ /= pL;

                    float width = orbSize * 0.7f * progress * layerScale;
                    float trailAlpha = progress * progress * alpha * 0.4f * layerAlphaScale;
                    int col2 = getColor(colorOffset + (1f - progress) * 0.3f);
                    float[] c2 = unpack(col2);

                    glowBuf.vertex(mat, pos[0] + pX * width, pos[1] + pY * width, pos[2] + pZ * width)
                            .color(c2[0], c2[1], c2[2], trailAlpha);
                    glowBuf.vertex(mat, pos[0] - pX * width, pos[1] - pY * width, pos[2] - pZ * width)
                            .color(c2[0], c2[1], c2[2], trailAlpha * 0.1f);
                }

                BufferRenderer.drawWithGlobalProgram(glowBuf.end());
            }
        }
    }

    private void renderGhostOrb(MatrixStack matrices, float x, float y, float z,
                                float size, float colorOffset) {
        int col = withAlpha(getColor(colorOffset), alpha);
        float[] c = unpack(col);

        int rings = 8;
        int segments = 12;

        matrices.push();
        matrices.translate(x, y, z);
        Matrix4f mat = matrices.peek().getPositionMatrix();

        Tessellator tess = Tessellator.getInstance();

        for (int ring = 0; ring < rings; ring++) {
            float lat0 = (float) Math.PI * (-0.5f + (float) ring / rings);
            float lat1 = (float) Math.PI * (-0.5f + (float) (ring + 1) / rings);

            float y0 = (float) Math.sin(lat0) * size;
            float y1 = (float) Math.sin(lat1) * size;
            float r0 = (float) Math.cos(lat0) * size;
            float r1 = (float) Math.cos(lat1) * size;

            float ringAlpha0 = (1f - Math.abs((float) ring / rings - 0.5f) * 2f) * 0.6f + 0.2f;
            float ringAlpha1 = (1f - Math.abs((float) (ring + 1) / rings - 0.5f) * 2f) * 0.6f + 0.2f;

            BufferBuilder buf = tess.begin(VertexFormat.DrawMode.TRIANGLE_STRIP, VertexFormats.POSITION_COLOR);

            for (int seg = 0; seg <= segments; seg++) {
                float lon = (float) (Math.PI * 2.0 * seg / segments);
                float cosLon = (float) Math.cos(lon);
                float sinLon = (float) Math.sin(lon);

                buf.vertex(mat, cosLon * r0, y0, sinLon * r0)
                        .color(c[0], c[1], c[2], c[3] * ringAlpha0);
                buf.vertex(mat, cosLon * r1, y1, sinLon * r1)
                        .color(c[0], c[1], c[2], c[3] * ringAlpha1);
            }

            BufferRenderer.drawWithGlobalProgram(buf.end());
        }

        if (glow.getValue()) {
            int glowLayers = glowIntensity.getValue().intValue();
            for (int gl = 1; gl <= glowLayers; gl++) {
                float glowSize = size * (1f + gl * 0.45f);
                float glowAlpha = c[3] * 0.15f / gl;

                for (int ring = 0; ring < 6; ring++) {
                    float lat0 = (float) Math.PI * (-0.5f + (float) ring / 6);
                    float lat1 = (float) Math.PI * (-0.5f + (float) (ring + 1) / 6);

                    float gy0 = (float) Math.sin(lat0) * glowSize;
                    float gy1 = (float) Math.sin(lat1) * glowSize;
                    float gr0 = (float) Math.cos(lat0) * glowSize;
                    float gr1 = (float) Math.cos(lat1) * glowSize;

                    BufferBuilder glowBuf = tess.begin(VertexFormat.DrawMode.TRIANGLE_STRIP, VertexFormats.POSITION_COLOR);

                    for (int seg = 0; seg <= 10; seg++) {
                        float lon = (float) (Math.PI * 2.0 * seg / 10);
                        float cosLon = (float) Math.cos(lon);
                        float sinLon = (float) Math.sin(lon);

                        glowBuf.vertex(mat, cosLon * gr0, gy0, sinLon * gr0)
                                .color(c[0], c[1], c[2], glowAlpha);
                        glowBuf.vertex(mat, cosLon * gr1, gy1, sinLon * gr1)
                                .color(c[0], c[1], c[2], glowAlpha);
                    }

                    BufferRenderer.drawWithGlobalProgram(glowBuf.end());
                }
            }
        }

        {
            BufferBuilder coreBuf = tess.begin(VertexFormat.DrawMode.TRIANGLE_FAN, VertexFormats.POSITION_COLOR);
            float coreSize = size * 0.4f;
            coreBuf.vertex(mat, 0, 0, 0).color(c[0], c[1], c[2], Math.min(1f, c[3] * 1.5f));
            for (int seg = 0; seg <= segments; seg++) {
                float lon = (float) (Math.PI * 2.0 * seg / segments);
                coreBuf.vertex(mat, (float) Math.cos(lon) * coreSize, 0, (float) Math.sin(lon) * coreSize)
                        .color(c[0], c[1], c[2], 0f);
            }
            BufferRenderer.drawWithGlobalProgram(coreBuf.end());
        }

        matrices.pop();
    }

    // ==================================================================================
    //                    SWORDS — 3D мечи на орбитах (Meoray Premium Style)
    // ==================================================================================

    private void renderSwords(MatrixStack matrices, double rx, double ry, double rz,
                              float entHeight, float entWidth, float tickDelta) {
        int count = swordCount.getValue().intValue();
        float size = swordSize.getValue().floatValue();
        float orbitRadius = swordOrbitRadius.getValue().floatValue();
        float rotSpeed = swordRotSpeed.getValue().floatValue();
        float tiltAngle = swordTiltAngle.getValue().floatValue();
        float bobSpeed = swordBobSpeed.getValue().floatValue();
        float bobAmount = swordBobAmount.getValue().floatValue();
        boolean doBob = swordBob.getValue();
        boolean doGlowAura = swordGlowTrail.getValue();

        double timeSeconds = System.currentTimeMillis() / 1000.0;

        float baseRotation = (float) (timeSeconds * rotSpeed) % 360f;

        float centerY = entHeight * 0.5f;

        matrices.push();
        matrices.translate(rx, ry + centerY, rz);

        for (int i = 0; i < count; i++) {
            float swordAngle = baseRotation + (360f / count) * i;
            float swordAngleRad = (float) Math.toRadians(swordAngle);

            float bob = 0f;
            if (doBob) {
                bob = (float) Math.sin(timeSeconds * bobSpeed * Math.PI * 2.0 + i * Math.PI * 0.5) * bobAmount;
            }

            float posX = (float) Math.sin(swordAngleRad) * orbitRadius;
            float posZ = (float) Math.cos(swordAngleRad) * orbitRadius;

            float colorOffset = (float) i / count;

            matrices.push();
            matrices.translate(posX, bob, posZ);

            matrices.multiply(new Quaternionf().rotateY(-swordAngleRad + (float) Math.PI));

            matrices.multiply(new Quaternionf().rotateX((float) Math.toRadians(180 + tiltAngle)));

            renderSword3D(matrices, size, colorOffset, doGlowAura);

            matrices.pop();
        }

        {
            matrices.push();
            Matrix4f ringMat = matrices.peek().getPositionMatrix();
            int ringCol = withAlpha(getColor(0f), alpha * 0.3f);
            drawRingGlow(ringMat, orbitRadius, 48, ringCol);
            matrices.pop();
        }

        matrices.pop();
    }

    private void renderSword3D(MatrixStack matrices, float size, float colorOffset, boolean doGlow) {
        Matrix4f mat = matrices.peek().getPositionMatrix();

        float bladeLength = size * 0.65f;
        float bladeWidth = size * 0.08f;
        float bladeThick = swordBladeThickness.getValue().floatValue() * size;
        float tipLength = size * 0.15f;

        float guardWidth = size * 0.25f;
        float guardHeight = size * 0.04f;
        float guardThick = bladeThick * 1.5f;

        float handleLength = size * 0.2f;
        float handleWidth = size * 0.035f;
        float handleThick = bladeThick * 1.2f;

        float pommelRadius = size * 0.04f;

        int colBlade = withAlpha(getColor(colorOffset), alpha * 0.9f);
        int colGuard = withAlpha(getColor(colorOffset + 0.15f), alpha * 0.85f);
        int colHandle = withAlpha(getColor(colorOffset + 0.3f), alpha * 0.7f);
        int colEdge = withAlpha(getColor(colorOffset + 0.1f), alpha);

        Tessellator tess = Tessellator.getInstance();

        // ====== BLADE ======
        renderBox(mat, tess, -bladeWidth, 0, -bladeThick, bladeWidth, bladeLength, bladeThick, colBlade);

        // ====== BLADE TIP ======
        renderBladeTip(mat, tess, bladeWidth, bladeThick, bladeLength, tipLength, colBlade, colEdge);

        // ====== GUARD ======
        renderBox(mat, tess, -guardWidth, -guardHeight, -guardThick,
                guardWidth, 0, guardThick, colGuard);

        // ====== HANDLE ======
        renderBox(mat, tess, -handleWidth, -guardHeight - handleLength, -handleThick,
                handleWidth, -guardHeight, handleThick, colHandle);

        // ====== POMMEL ======
        float pommelY = -guardHeight - handleLength;
        renderBox(mat, tess, -pommelRadius, pommelY - pommelRadius * 2, -pommelRadius,
                pommelRadius, pommelY, pommelRadius, colGuard);

        // ====== EDGE LINES ======
        float[] ce = unpack(colEdge);
        RenderSystem.lineWidth(lineWidth.getValue().floatValue());
        {
            BufferBuilder lineBuf = tess.begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);

            edgeLine(lineBuf, mat, -bladeWidth, 0, 0, -bladeWidth, bladeLength, 0, ce);
            edgeLine(lineBuf, mat, bladeWidth, 0, 0, bladeWidth, bladeLength, 0, ce);
            edgeLine(lineBuf, mat, -bladeWidth, bladeLength, 0, 0, bladeLength + tipLength, 0, ce);
            edgeLine(lineBuf, mat, bladeWidth, bladeLength, 0, 0, bladeLength + tipLength, 0, ce);

            edgeLine(lineBuf, mat, 0, bladeLength * 0.1f, bladeThick + 0.001f,
                    0, bladeLength * 0.85f, bladeThick + 0.001f, ce);
            edgeLine(lineBuf, mat, 0, bladeLength * 0.1f, -bladeThick - 0.001f,
                    0, bladeLength * 0.85f, -bladeThick - 0.001f, ce);

            edgeLine(lineBuf, mat, -guardWidth, 0, 0, guardWidth, 0, 0, ce);
            edgeLine(lineBuf, mat, -guardWidth, -guardHeight, 0, guardWidth, -guardHeight, 0, ce);

            BufferRenderer.drawWithGlobalProgram(lineBuf.end());
        }
        RenderSystem.lineWidth(1f);

        // ====== GLOW AURA ======
        if (doGlow && glow.getValue()) {
            int glowLayers = glowIntensity.getValue().intValue();
            for (int gl = 1; gl <= glowLayers; gl++) {
                float expand = gl * 0.015f * size;
                float glowAlpha = alpha * 0.12f / gl;
                int glowCol = withAlpha(getColor(colorOffset), glowAlpha);
                renderBox(mat, tess,
                        -bladeWidth - expand, -expand, -bladeThick - expand,
                        bladeWidth + expand, bladeLength + expand, bladeThick + expand,
                        glowCol);
            }
        }
    }

    private void renderBox(Matrix4f mat, Tessellator tess,
                           float x1, float y1, float z1,
                           float x2, float y2, float z2, int col) {
        float[] c = unpack(col);

        BufferBuilder buf = tess.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);

        // Front (+Z)
        buf.vertex(mat, x1, y1, z2).color(c[0], c[1], c[2], c[3]);
        buf.vertex(mat, x2, y1, z2).color(c[0], c[1], c[2], c[3]);
        buf.vertex(mat, x2, y2, z2).color(c[0], c[1], c[2], c[3] * 0.8f);
        buf.vertex(mat, x1, y2, z2).color(c[0], c[1], c[2], c[3] * 0.8f);

        // Back (-Z)
        buf.vertex(mat, x2, y1, z1).color(c[0], c[1], c[2], c[3]);
        buf.vertex(mat, x1, y1, z1).color(c[0], c[1], c[2], c[3]);
        buf.vertex(mat, x1, y2, z1).color(c[0], c[1], c[2], c[3] * 0.8f);
        buf.vertex(mat, x2, y2, z1).color(c[0], c[1], c[2], c[3] * 0.8f);

        // Right (+X)
        buf.vertex(mat, x2, y1, z2).color(c[0], c[1], c[2], c[3] * 0.9f);
        buf.vertex(mat, x2, y1, z1).color(c[0], c[1], c[2], c[3] * 0.9f);
        buf.vertex(mat, x2, y2, z1).color(c[0], c[1], c[2], c[3] * 0.7f);
        buf.vertex(mat, x2, y2, z2).color(c[0], c[1], c[2], c[3] * 0.7f);

        // Left (-X)
        buf.vertex(mat, x1, y1, z1).color(c[0], c[1], c[2], c[3] * 0.9f);
        buf.vertex(mat, x1, y1, z2).color(c[0], c[1], c[2], c[3] * 0.9f);
        buf.vertex(mat, x1, y2, z2).color(c[0], c[1], c[2], c[3] * 0.7f);
        buf.vertex(mat, x1, y2, z1).color(c[0], c[1], c[2], c[3] * 0.7f);

        // Top (+Y)
        buf.vertex(mat, x1, y2, z1).color(c[0], c[1], c[2], c[3] * 0.85f);
        buf.vertex(mat, x1, y2, z2).color(c[0], c[1], c[2], c[3] * 0.85f);
        buf.vertex(mat, x2, y2, z2).color(c[0], c[1], c[2], c[3] * 0.85f);
        buf.vertex(mat, x2, y2, z1).color(c[0], c[1], c[2], c[3] * 0.85f);

        // Bottom (-Y)
        buf.vertex(mat, x1, y1, z2).color(c[0], c[1], c[2], c[3] * 0.6f);
        buf.vertex(mat, x1, y1, z1).color(c[0], c[1], c[2], c[3] * 0.6f);
        buf.vertex(mat, x2, y1, z1).color(c[0], c[1], c[2], c[3] * 0.6f);
        buf.vertex(mat, x2, y1, z2).color(c[0], c[1], c[2], c[3] * 0.6f);

        BufferRenderer.drawWithGlobalProgram(buf.end());
    }

    private void renderBladeTip(Matrix4f mat, Tessellator tess,
                                float bladeW, float bladeT,
                                float baseY, float tipLen,
                                int colBase, int colTip) {
        float[] cb = unpack(colBase);
        float[] ct = unpack(colTip);
        float tipY = baseY + tipLen;

        BufferBuilder buf = tess.begin(VertexFormat.DrawMode.TRIANGLES, VertexFormats.POSITION_COLOR);

        // Front face
        buf.vertex(mat, -bladeW, baseY, bladeT).color(cb[0], cb[1], cb[2], cb[3]);
        buf.vertex(mat, bladeW, baseY, bladeT).color(cb[0], cb[1], cb[2], cb[3]);
        buf.vertex(mat, 0, tipY, 0).color(ct[0], ct[1], ct[2], ct[3]);

        // Back face
        buf.vertex(mat, bladeW, baseY, -bladeT).color(cb[0], cb[1], cb[2], cb[3]);
        buf.vertex(mat, -bladeW, baseY, -bladeT).color(cb[0], cb[1], cb[2], cb[3]);
        buf.vertex(mat, 0, tipY, 0).color(ct[0], ct[1], ct[2], ct[3]);

        // Right face
        buf.vertex(mat, bladeW, baseY, bladeT).color(cb[0], cb[1], cb[2], cb[3] * 0.85f);
        buf.vertex(mat, bladeW, baseY, -bladeT).color(cb[0], cb[1], cb[2], cb[3] * 0.85f);
        buf.vertex(mat, 0, tipY, 0).color(ct[0], ct[1], ct[2], ct[3]);

        // Left face
        buf.vertex(mat, -bladeW, baseY, -bladeT).color(cb[0], cb[1], cb[2], cb[3] * 0.85f);
        buf.vertex(mat, -bladeW, baseY, bladeT).color(cb[0], cb[1], cb[2], cb[3] * 0.85f);
        buf.vertex(mat, 0, tipY, 0).color(ct[0], ct[1], ct[2], ct[3]);

        BufferRenderer.drawWithGlobalProgram(buf.end());
    }

    private void edgeLine(BufferBuilder buf, Matrix4f mat,
                          float x1, float y1, float z1,
                          float x2, float y2, float z2, float[] c) {
        buf.vertex(mat, x1, y1, z1).color(c[0], c[1], c[2], c[3]);
        buf.vertex(mat, x2, y2, z2).color(c[0], c[1], c[2], c[3]);
    }

    // ==================================================================================
    //                            JELLO — Пульсирующее кольцо
    // ==================================================================================

    private void renderJello(MatrixStack matrices, double rx, double ry, double rz, float w) {
        matrices.push();
        matrices.translate(rx, ry + 0.02, rz);
        Matrix4f mat = matrices.peek().getPositionMatrix();

        float baseR = jelloRadius.getValue().floatValue();
        float pulseAmt = jelloPulse.getValue().floatValue();
        float speed = jelloSpeed.getValue().floatValue();
        float pulse = (float) Math.sin(System.currentTimeMillis() / 1000.0 * speed) * pulseAmt;
        float r = baseR + pulse;
        int seg = 64;

        int col1 = withAlpha(getColor(0f), alpha);
        int col2 = withAlpha(getColor(0.5f), alpha);

        if (jelloFilled.getValue()) {
            float[] c = unpack(col1);
            Tessellator tess = Tessellator.getInstance();
            BufferBuilder buf = tess.begin(VertexFormat.DrawMode.TRIANGLE_FAN, VertexFormats.POSITION_COLOR);
            buf.vertex(mat, 0, 0, 0).color(c[0], c[1], c[2], c[3] * 0.3f);
            for (int i = 0; i <= seg; i++) {
                float a = (float) (Math.PI * 2 * i / seg);
                buf.vertex(mat, (float) Math.cos(a) * r, 0, (float) Math.sin(a) * r)
                        .color(c[0], c[1], c[2], 0f);
            }
            BufferRenderer.drawWithGlobalProgram(buf.end());
        }

        drawRingGlow(mat, r, seg, col1);
        drawRingGlow(mat, r * 0.6f, seg, col2);

        matrices.pop();
    }

    // ==================================================================================
    //                            VORTEX — Двойная спираль
    // ==================================================================================

    private void renderVortex(MatrixStack matrices, double rx, double ry, double rz, float h, float w) {
        matrices.push();
        matrices.translate(rx, ry, rz);
        matrices.multiply(new Quaternionf().rotateY(
                (float) Math.toRadians(
                        (float) (System.currentTimeMillis() / 1000.0 * vortexRotSpeed.getValue().floatValue()) % 360f
                )));
        Matrix4f mat = matrices.peek().getPositionMatrix();

        float r = vortexRadius.getValue().floatValue();
        int seg = vortexSegments.getValue().intValue();
        float turns = vortexTurns.getValue().floatValue();

        int col1 = withAlpha(getColor(0f), alpha);
        int col2 = withAlpha(getColor(0.5f), alpha);

        drawHelix(mat, r, h, seg, turns, 0f, col1);
        if (vortexDouble.getValue()) {
            drawHelix(mat, r, h, seg, turns, (float) Math.PI, col2);
        }

        drawRingGlow(mat, r * 1.05f, 32, col1);
        matrices.push();
        matrices.translate(0, h, 0);
        Matrix4f matTop = matrices.peek().getPositionMatrix();
        drawRingGlow(matTop, r * 1.05f, 32, col1);
        matrices.pop();

        matrices.pop();
    }

    private void drawHelix(Matrix4f mat, float r, float h, int seg, float turns, float phase, int col) {
        float[] c = unpack(col);
        int layers = glow.getValue() ? glowIntensity.getValue().intValue() : 0;
        float baseW = lineWidth.getValue().floatValue();

        for (int l = layers; l >= 0; l--) {
            float t = layers > 0 ? (float) l / layers : 0;
            float w = baseW + 2f * t;
            float a = c[3] * (1f - t * 0.85f);
            if (l == 0) {
                w = baseW * 0.85f;
                a = Math.min(1f, c[3] * 1.3f);
            }
            RenderSystem.lineWidth(w);

            Tessellator tess = Tessellator.getInstance();
            BufferBuilder buf = tess.begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);
            for (int i = 0; i < seg; i++) {
                float p1 = (float) i / seg;
                float p2 = (float) (i + 1) / seg;
                float a1 = p1 * (float) Math.PI * 2 * turns + phase;
                float a2 = p2 * (float) Math.PI * 2 * turns + phase;
                float y1 = p1 * h;
                float y2 = p2 * h;
                buf.vertex(mat, (float) Math.cos(a1) * r, y1, (float) Math.sin(a1) * r).color(c[0], c[1], c[2], a);
                buf.vertex(mat, (float) Math.cos(a2) * r, y2, (float) Math.sin(a2) * r).color(c[0], c[1], c[2], a);
            }
            BufferRenderer.drawWithGlobalProgram(buf.end());
            if (layers == 0) break;
        }
        RenderSystem.lineWidth(1f);
    }

    // ==================================================================================
    //                              Общие утилиты рендеринга
    // ==================================================================================

    private void drawRingGlow(Matrix4f mat, float r, int seg, int col) {
        float[] c = unpack(col);
        if (glow.getValue()) {
            int layers = glowIntensity.getValue().intValue();
            float baseW = lineWidth.getValue().floatValue();
            for (int l = layers; l >= 0; l--) {
                float t = (float) l / layers;
                float w = baseW + 2.5f * t;
                float a = c[3] * (1f - t * 0.85f);
                if (l == 0) {
                    w = baseW * 0.85f;
                    a = Math.min(1f, c[3] * 1.3f);
                }
                RenderSystem.lineWidth(w);
                drawRingRaw(mat, r, seg, c[0], c[1], c[2], a);
            }
            RenderSystem.lineWidth(1f);
        } else {
            RenderSystem.lineWidth(lineWidth.getValue().floatValue());
            drawRingRaw(mat, r, seg, c[0], c[1], c[2], c[3]);
            RenderSystem.lineWidth(1f);
        }
    }

    private void drawRingRaw(Matrix4f mat, float r, int seg, float cr, float cg, float cb, float a) {
        Tessellator tess = Tessellator.getInstance();
        BufferBuilder buf = tess.begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);
        for (int i = 0; i < seg; i++) {
            float a1 = (float) (Math.PI * 2 * i / seg);
            float a2 = (float) (Math.PI * 2 * (i + 1) / seg);
            buf.vertex(mat, (float) Math.cos(a1) * r, 0, (float) Math.sin(a1) * r).color(cr, cg, cb, a);
            buf.vertex(mat, (float) Math.cos(a2) * r, 0, (float) Math.sin(a2) * r).color(cr, cg, cb, a);
        }
        BufferRenderer.drawWithGlobalProgram(buf.end());
    }

    // ==================================================================================
    //                                 Цвет / Утилиты
    // ==================================================================================

    private int getColor(float offset) {
        if (rainbow.getValue()) {
            long time = System.currentTimeMillis();
            float speed = rainbowSpeed.getValue().floatValue();
            float hue = ((time % (long) (3000 / speed)) / (3000f / speed) + offset) % 1f;
            int rgb = Color.HSBtoRGB(hue, 0.8f, 1f) & 0x00FFFFFF;
            return 0xFF000000 | rgb;
        }
        return offset < 0.4f ? primaryColor.getValue().getRGB() : secondaryColor.getValue().getRGB();
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
