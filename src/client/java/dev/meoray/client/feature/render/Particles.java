package dev.meoray.client.feature.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.platform.GlStateManager;
import dev.meoray.client.MeoRayClient;
import dev.meoray.client.core.Category;
import dev.meoray.client.core.Module;
import dev.meoray.client.core.setting.BooleanSetting;
import dev.meoray.client.core.setting.ModeSetting;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.option.Perspective;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Particles extends Module {

    private static final int TYPE_COUNT = 9;
    private static final float FADE_IN_SPEED = 3f;
    private static final float FADE_OUT_SPEED = 1.5f;
    private static final int MAX_SKY = 100;
    private static final int MAX_MOVE = 64;
    private static final int MAX_HIT = 96;
    private static final int MAX_TOTAL = 260;
    private static final long SKY_SPAWN_INTERVAL = 5L;
    private static final long MOVE_LIFETIME = 500L;
    private static final long HIT_LIFETIME = 5000L;
    private static final int VISIBILITY_RECHECK = 4;
    private static final double LOG_035 = Math.log(0.35);
    private static final byte KIND_SKY = 0;
    private static final byte KIND_MOVE = 1;
    private static final byte KIND_HIT = 2;

    private static final Identifier[] PARTICLE_TEX = new Identifier[TYPE_COUNT];
    static {
        for (int i = 0; i < TYPE_COUNT; i++) {
            PARTICLE_TEX[i] = Identifier.of("meoray", "images/particles/type" + (i + 1) + ".png");
        }
    }

    private final BooleanSetting type1 = add(new BooleanSetting("Type 1", false));
    private final BooleanSetting type2 = add(new BooleanSetting("Type 2", false));
    private final BooleanSetting type3 = add(new BooleanSetting("Type 3", false));
    private final BooleanSetting type4 = add(new BooleanSetting("Type 4", false));
    private final BooleanSetting type5 = add(new BooleanSetting("Type 5", false));
    private final BooleanSetting type6 = add(new BooleanSetting("Type 6", false));
    private final BooleanSetting type7 = add(new BooleanSetting("Type 7", false));
    private final BooleanSetting type8 = add(new BooleanSetting("Type 8", false));
    private final BooleanSetting type9 = add(new BooleanSetting("Type 9", false));
    private final ModeSetting moveMode = add(new ModeSetting("Move Mode", "Down", "Up", "Down"));
    private final BooleanSetting triggerSky = add(new BooleanSetting("Sky", true));
    private final BooleanSetting triggerMove = add(new BooleanSetting("Move", true));
    private final BooleanSetting triggerAttack = add(new BooleanSetting("Attack", true));

    private final List<Particle> particles = new ArrayList<>(256);
    private final List<Particle>[] batches = new List[TYPE_COUNT];
    private final Random random = new Random();
    private final Quaternionf cameraRot = new Quaternionf();
    private final Vector3f rightAxis = new Vector3f();
    private final Vector3f upAxis = new Vector3f();
    private final int[] enabledTypeIndices = new int[TYPE_COUNT];

    private long lastRenderNano = System.nanoTime();
    private long frameTimeMs;
    private int tickCounter;
    private int skyCount, moveCount, hitCount;
    private int enabledTypeCount;
    private long lastSkySpawn;
    private double eyeX, eyeY, eyeZ;
    private boolean moveDown;

    public Particles() {
        super("Particles", "Adds ambient particles to the world", Category.RENDER);
        for (int i = 0; i < TYPE_COUNT; i++) {
            batches[i] = new ArrayList<>(48);
        }
        WorldRenderEvents.AFTER_ENTITIES.register(this::onWorldRender);
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (isEnabled() && triggerAttack.getValue() && entity != null) {
                if (particles.size() < MAX_TOTAL && hitCount < MAX_HIT) {
                    spawnHitParticle(entity);
                    spawnHitParticle(entity);
                    spawnHitParticle(entity);
                }
            }
            return ActionResult.PASS;
        });
    }

    @Override
    protected void onDisable() {
        particles.clear();
        skyCount = 0;
        moveCount = 0;
        hitCount = 0;
        frameTimeMs = 0L;
        tickCounter = 0;
        lastRenderNano = System.nanoTime();
        for (int i = 0; i < TYPE_COUNT; i++) batches[i].clear();
    }

    @Override
    public void onTick() {
        var mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.world == null) return;

        tickCounter++;
        moveDown = moveMode.getValue().equals("Down");
        eyeX = mc.player.getX();
        eyeY = mc.player.getEyeY();
        eyeZ = mc.player.getZ();

        rebuildEnabledTypes();

        if (skyCount < MAX_SKY && triggerSky.getValue()) {
            long now = System.currentTimeMillis();
            if (now - lastSkySpawn >= SKY_SPAWN_INTERVAL) {
                if (particles.size() < MAX_TOTAL) {
                    double px = mc.player.getX();
                    double py = mc.player.getY();
                    double pz = mc.player.getZ();
                    double sx = px + 30.0 - random.nextInt(60);
                    double sy = moveDown ? py + 10.0 + random.nextInt(25) : py + 1.0 + random.nextInt(10);
                    double sz = pz + 30.0 - random.nextInt(60);
                    if (canBeSeen(sx, sy, sz)) {
                        addParticle(new SkyParticle(sx, sy, sz, nextTex(), tickCounter + random.nextInt(4)));
                        lastSkySpawn = now;
                    }
                }
            }
        }

        if (moveCount < MAX_MOVE && triggerMove.getValue()) {
            var vel = mc.player.getVelocity();
            if (vel.x != 0 || vel.z != 0) {
                if (particles.size() < MAX_TOTAL) {
                    double sx = mc.player.getX() + 0.25 - random.nextDouble() * 0.5;
                    double sy = mc.player.getY() + 0.75 + random.nextDouble() * 0.75;
                    double sz = mc.player.getZ() + 0.25 - random.nextDouble() * 0.5;
                    addParticle(new MoveParticle(sx, sy, sz, nextTex(), frameTimeMs != 0L ? frameTimeMs : System.currentTimeMillis(), tickCounter + 1));
                }
            }
        }
    }

    private void onWorldRender(WorldRenderContext ctx) {
        if (!isEnabled()) return;
        var mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.world == null) return;

        if (particles.isEmpty()) {
            lastRenderNano = System.nanoTime();
            return;
        }

        long nowNano = System.nanoTime();
        float deltaTime = Math.min((float) (nowNano - lastRenderNano) * 1e-9f, 0.05f);
        lastRenderNano = nowNano;
        frameTimeMs = System.currentTimeMillis();

        Camera camera = mc.gameRenderer.getCamera();
        Vec3d camPos = camera.getPos();
        double camX = camPos.x, camY = camPos.y, camZ = camPos.z;

        cameraRot.set(camera.getRotation());
        rightAxis.set(0.6f, 0, 0).rotate(cameraRot);
        upAxis.set(0, 0.6f, 0).rotate(cameraRot);

        boolean firstPerson = mc.options.getPerspective() == Perspective.FIRST_PERSON;
        int themeRgb = MeoRayClient.INSTANCE.getThemeManager().getCurrentTheme().accent() & 0xFFFFFF;

        Matrix4f posMatrix = ctx.positionMatrix();

        clearBatches();
        fillBatches(deltaTime, firstPerson);

        RenderSystem.enableDepthTest();
        RenderSystem.depthFunc(515);
        RenderSystem.depthMask(true);
        RenderSystem.disableCull();
        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE, GlStateManager.SrcFactor.ONE, GlStateManager.DstFactor.ZERO);
        RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);

        for (int texIdx = 0; texIdx < TYPE_COUNT; texIdx++) {
            List<Particle> batch = batches[texIdx];
            if (batch.isEmpty()) continue;
            RenderSystem.setShaderTexture(0, PARTICLE_TEX[texIdx]);
            BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
            for (Particle p : batch) {
                appendQuad(buffer, posMatrix, p, camX, camY, camZ, themeRgb);
            }
            BufferRenderer.drawWithGlobalProgram(buffer.end());
        }

        RenderSystem.disableBlend();
        RenderSystem.enableCull();
        RenderSystem.depthMask(true);
        RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA);
        RenderSystem.setShaderColor(1, 1, 1, 1);
        pruneDead();
    }

    private void rebuildEnabledTypes() {
        BooleanSetting[] types = {type1, type2, type3, type4, type5, type6, type7, type8, type9};
        int count = 0;
        for (int i = 0; i < TYPE_COUNT; i++) {
            if (types[i].getValue()) {
                enabledTypeIndices[count++] = i;
            }
        }
        enabledTypeCount = count;
    }

    private int nextTex() {
        return enabledTypeCount == 0 ? 0 : enabledTypeIndices[random.nextInt(enabledTypeCount)];
    }

    private void clearBatches() {
        for (int i = 0; i < TYPE_COUNT; i++) batches[i].clear();
    }

    private void fillBatches(float dt, boolean firstPerson) {
        for (Particle p : particles) {
            p.update(dt, moveDown, frameTimeMs);
            if (p.kind == KIND_MOVE && firstPerson) continue;
            float alpha = p.getAlpha();
            p.renderAlpha = alpha;
            if (alpha <= 0.005f) continue;
            batches[p.textureIdx].add(p);
        }
    }

    private void appendQuad(BufferBuilder buffer, Matrix4f posMat, Particle p, double cx, double cy, double cz, int themeRgb) {
        int color = ((int) (p.renderAlpha * 255f) & 0xFF) << 24 | themeRgb;
        float qMax = p.quadSize;
        float qMin = -0.75f;
        float bx = (float) (p.x - cx);
        float by = (float) (p.y - cy);
        float bz = (float) (p.z - cz);

        float x1 = bx + rightAxis.x * qMin + upAxis.x * qMax;
        float y1 = by + rightAxis.y * qMin + upAxis.y * qMax;
        float z1 = bz + rightAxis.z * qMin + upAxis.z * qMax;

        float x2 = bx + rightAxis.x * qMax + upAxis.x * qMax;
        float y2 = by + rightAxis.y * qMax + upAxis.y * qMax;
        float z2 = bz + rightAxis.z * qMax + upAxis.z * qMax;

        float x3 = bx + rightAxis.x * qMax + upAxis.x * qMin;
        float y3 = by + rightAxis.y * qMax + upAxis.y * qMin;
        float z3 = bz + rightAxis.z * qMax + upAxis.z * qMin;

        float x4 = bx + rightAxis.x * qMin + upAxis.x * qMin;
        float y4 = by + rightAxis.y * qMin + upAxis.y * qMin;
        float z4 = bz + rightAxis.z * qMin + upAxis.z * qMin;

        buffer.vertex(posMat, x1, y1, z1).texture(0, 0).color(color);
        buffer.vertex(posMat, x2, y2, z2).texture(1, 0).color(color);
        buffer.vertex(posMat, x3, y3, z3).texture(1, 1).color(color);
        buffer.vertex(posMat, x4, y4, z4).texture(0, 1).color(color);
    }

    private boolean canBeSeen(double tx, double ty, double tz) {
        var mc = MinecraftClient.getInstance();
        double ay = ty - (moveDown ? 0.5 : 0.0);
        double dx = tx - eyeX;
        double dy = ay - eyeY;
        double dz = tz - eyeZ;
        if (dx * dx + dy * dy + dz * dz > 16384.0) return false;
        Vec3d eye = new Vec3d(eyeX, eyeY, eyeZ);
        Vec3d target = new Vec3d(tx, ay, tz);
        return mc.world.raycast(new RaycastContext(eye, target, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, mc.player)).getType() == HitResult.Type.MISS;
    }

    private void spawnHitParticle(Entity entity) {
        double ex = entity.getX(), ey = entity.getY(), ez = entity.getZ();
        double sx = ex + (random.nextDouble() - 0.5) * 0.75;
        double sy = ey + random.nextDouble();
        double sz = ez + (random.nextDouble() - 0.5) * 0.75;
        double endX = ex - 8.0 + random.nextInt(16);
        double endY = ey + random.nextInt(10);
        double endZ = ez - 8.0 + random.nextInt(16);
        addParticle(new HitParticle(sx, sy, sz, nextTex(), endX, endY, endZ,
            frameTimeMs != 0L ? frameTimeMs : System.currentTimeMillis(), tickCounter + 1));
    }

    private void addParticle(Particle p) {
        particles.add(p);
        switch (p.kind) {
            case 0 -> skyCount++;
            case 1 -> moveCount++;
            case 2 -> hitCount++;
        }
    }

    private void pruneDead() {
        for (int i = particles.size() - 1; i >= 0; i--) {
            Particle p = particles.get(i);
            if (p.fadingOut && p.fadeAlpha <= 0.005f) {
                removeParticle(i);
            }
        }
    }

    private void removeParticle(int index) {
        int last = particles.size() - 1;
        Particle removed = particles.get(index);
        if (index != last) {
            particles.set(index, particles.get(last));
        }
        particles.remove(last);
        switch (removed.kind) {
            case 0 -> skyCount--;
            case 1 -> moveCount--;
            case 2 -> hitCount--;
        }
    }

    private static class Particle {
        double x, y, z;
        final double startY;
        final int textureIdx;
        final byte kind;
        final float quadSize;
        int nextVisTick;
        float renderAlpha;
        float fadeAlpha;
        boolean fadingOut;

        Particle(double x, double y, double z, int textureIdx, byte kind, float quadSize, int nextVisTick) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.startY = y;
            this.textureIdx = textureIdx;
            this.kind = kind;
            this.quadSize = quadSize;
            this.nextVisTick = nextVisTick;
            this.fadeAlpha = 0f;
        }

        void update(float dt, boolean moveDown, long nowMs) {
            this.y += (moveDown ? -1.5f : 1.5f) * dt;
            if (fadingOut) {
                fadeAlpha = Math.max(0, fadeAlpha - dt * FADE_OUT_SPEED);
            } else {
                fadeAlpha = Math.min(1, fadeAlpha + dt * FADE_IN_SPEED);
            }
        }

        float getAlpha() { return Math.max(0, Math.min(1, fadeAlpha)); }
    }

    private static class SkyParticle extends Particle {
        SkyParticle(double x, double y, double z, int textureIdx, int nextVisTick) {
            super(x, y, z, textureIdx, KIND_SKY, 1.5f, nextVisTick);
        }
    }

    private static class MoveParticle extends Particle {
        private final long bornAtMs;
        MoveParticle(double x, double y, double z, int textureIdx, long bornAtMs, int nextVisTick) {
            super(x, y, z, textureIdx, KIND_MOVE, 0.75f, nextVisTick);
            this.bornAtMs = bornAtMs;
        }
        @Override
        void update(float dt, boolean moveDown, long nowMs) {
            super.update(dt, moveDown, nowMs);
            if (nowMs - bornAtMs >= MOVE_LIFETIME) fadingOut = true;
        }
    }

    private static class HitParticle extends Particle {
        private final double endX, endY, endZ;
        private final long bornAtMs;
        HitParticle(double x, double y, double z, int textureIdx, double endX, double endY, double endZ, long bornAtMs, int nextVisTick) {
            super(x, y, z, textureIdx, KIND_HIT, 1.5f, nextVisTick);
            this.endX = endX;
            this.endY = endY;
            this.endZ = endZ;
            this.bornAtMs = bornAtMs;
        }
        @Override
        void update(float dt, boolean moveDown, long nowMs) {
            float lerp = 1f - (float) Math.exp(LOG_035 * dt);
            x += (endX - x) * lerp;
            y += (endY - y) * lerp;
            z += (endZ - z) * lerp;
            if (nowMs - bornAtMs >= HIT_LIFETIME) fadingOut = true;
            if (!fadingOut && fadeAlpha < 1f) {
                fadeAlpha = Math.min(1, fadeAlpha + dt * 3f);
            }
            if (fadingOut) {
                fadeAlpha = Math.max(0, fadeAlpha - dt * 1.5f);
            }
        }
    }
}
