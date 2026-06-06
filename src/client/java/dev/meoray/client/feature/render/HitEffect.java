package dev.meoray.client.feature.render;

import dev.meoray.client.core.Category;
import dev.meoray.client.core.Module;
import dev.meoray.client.core.setting.BooleanSetting;
import dev.meoray.client.core.setting.ColorSetting;
import dev.meoray.client.core.setting.ModeSetting;
import dev.meoray.client.core.setting.NumberSetting;
import dev.meoray.client.utils.render.Render3DUtils;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Vec3d;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class HitEffect extends Module {

    private final ModeSetting mode = new ModeSetting(
            "Mode", "Lightning",
            "Lightning", "Sparks", "Crystals", "Shockwave", "Crit Stars"
    );

    private final BooleanSetting onlyOnCrit = new BooleanSetting("Only On Crit", false);
    private final NumberSetting size = new NumberSetting("Size", 1.0, 0.3, 3.0, 0.1);
    private final NumberSetting lifetime = new NumberSetting("Lifetime (ms)", 600.0, 100.0, 2000.0, 50.0);
    private final NumberSetting particleCount = new NumberSetting("Particle Count", 12.0, 3.0, 40.0, 1.0);
    private final ColorSetting color = new ColorSetting("Color", new Color(180, 80, 255));
    private final BooleanSetting rainbow = new BooleanSetting("Rainbow", false);

    private final List<Effect> effects = new ArrayList<>();
    private final Map<Integer, Integer> lastHurtTime = new HashMap<>();
    private final Random rand = new Random();

    public HitEffect() {
        super("HitEffect", "Эффекты при ударе по сущности", Category.RENDER);

        particleCount.visibleWhen(() -> {
            String m = mode.getValue();
            return m.equals("Sparks") || m.equals("Crit Stars") || m.equals("Crystals");
        });
        color.visibleWhen(() -> !rainbow.getValue());

        addSettings(mode, onlyOnCrit, size, lifetime, particleCount, color, rainbow);
    }

    public void tick() {
        if (mc.world == null || mc.player == null) return;

        for (Entity entity : mc.world.getEntities()) {
            if (!(entity instanceof LivingEntity living)) continue;
            if (entity == mc.player) continue;

            int prevHurt = lastHurtTime.getOrDefault(entity.getId(), 0);
            int curHurt = living.hurtTime;

            if (prevHurt == 0 && curHurt > 0) {
                if (!onlyOnCrit.getValue() || isCrit()) {
                    spawnEffect(entity);
                }
            }
            lastHurtTime.put(entity.getId(), curHurt);
        }

        lastHurtTime.entrySet().removeIf(e -> mc.world.getEntityById(e.getKey()) == null);

        long now = System.currentTimeMillis();
        Iterator<Effect> it = effects.iterator();
        while (it.hasNext()) {
            Effect e = it.next();
            if (now - e.startTime > e.lifetime) {
                it.remove();
            }
        }
    }

    private boolean isCrit() {
        return mc.player.fallDistance > 0
                && !mc.player.isOnGround()
                && !mc.player.isClimbing()
                && !mc.player.isTouchingWater()
                && !mc.player.hasStatusEffect(net.minecraft.entity.effect.StatusEffects.BLINDNESS)
                && !mc.player.hasVehicle();
    }

    private void spawnEffect(Entity entity) {
        Vec3d center = entity.getPos().add(0, entity.getHeight() / 2.0, 0);
        Effect e = new Effect();
        e.center = center;
        e.startTime = System.currentTimeMillis();
        e.lifetime = lifetime.getValue().longValue();
        e.mode = mode.getValue();
        e.size = size.getValue().floatValue();
        e.baseColor = rainbow.getValue() ? null : color.getValue();
        e.particles = new ArrayList<>();

        int count = particleCount.getValue().intValue();

        switch (e.mode) {
            case "Lightning" -> spawnLightning(e);
            case "Sparks" -> spawnSparks(e, count);
            case "Crystals" -> spawnCrystals(e, count);
            case "Shockwave" -> spawnShockwave(e);
            case "Crit Stars" -> spawnCritStars(e, count);
        }

        effects.add(e);
    }

    private void spawnLightning(Effect e) {
        int bolts = 4;
        for (int b = 0; b < bolts; b++) {
            Particle p = new Particle();
            p.type = "lightning";
            p.start = e.center;

            double angle = rand.nextDouble() * Math.PI * 2;
            double dx = Math.cos(angle) * 0.3;
            double dz = Math.sin(angle) * 0.3;
            p.end = e.center.add(dx * e.size, 1.5 * e.size, dz * e.size);

            p.zigzag = new ArrayList<>();
            p.zigzag.add(p.start);
            int segments = 5;
            for (int i = 1; i < segments; i++) {
                double t = (double) i / segments;
                Vec3d straight = p.start.lerp(p.end, t);
                Vec3d offset = new Vec3d(
                        (rand.nextDouble() - 0.5) * 0.3 * e.size,
                        (rand.nextDouble() - 0.5) * 0.2 * e.size,
                        (rand.nextDouble() - 0.5) * 0.3 * e.size
                );
                p.zigzag.add(straight.add(offset));
            }
            p.zigzag.add(p.end);

            e.particles.add(p);
        }
    }

    private void spawnSparks(Effect e, int count) {
        for (int i = 0; i < count; i++) {
            Particle p = new Particle();
            p.type = "spark";
            p.start = e.center;
            double theta = rand.nextDouble() * Math.PI * 2;
            double phi = Math.acos(2 * rand.nextDouble() - 1);
            double speed = 0.15 + rand.nextDouble() * 0.15;
            p.velocity = new Vec3d(
                    Math.sin(phi) * Math.cos(theta) * speed,
                    Math.cos(phi) * speed,
                    Math.sin(phi) * Math.sin(theta) * speed
            ).multiply(e.size);
            e.particles.add(p);
        }
    }

    private void spawnCrystals(Effect e, int count) {
        for (int i = 0; i < count; i++) {
            Particle p = new Particle();
            p.type = "crystal";
            double angle = (Math.PI * 2 * i) / count;
            double r = 0.4 + rand.nextDouble() * 0.3;
            p.start = e.center.add(Math.cos(angle) * r * e.size, 0, Math.sin(angle) * r * e.size);
            p.velocity = new Vec3d(Math.cos(angle) * 0.05, 0.08, Math.sin(angle) * 0.05).multiply(e.size);
            p.rotation = rand.nextFloat() * (float) Math.PI;
            e.particles.add(p);
        }
    }

    private void spawnShockwave(Effect e) {
        Particle p = new Particle();
        p.type = "shockwave";
        p.start = e.center;
        e.particles.add(p);
    }

    private void spawnCritStars(Effect e, int count) {
        for (int i = 0; i < count; i++) {
            Particle p = new Particle();
            p.type = "star";
            p.start = e.center;
            double theta = rand.nextDouble() * Math.PI * 2;
            double phi = Math.acos(2 * rand.nextDouble() - 1);
            double speed = 0.1 + rand.nextDouble() * 0.12;
            p.velocity = new Vec3d(
                    Math.sin(phi) * Math.cos(theta) * speed,
                    Math.abs(Math.cos(phi)) * speed,
                    Math.sin(phi) * Math.sin(theta) * speed
            ).multiply(e.size);
            p.rotation = rand.nextFloat() * (float) Math.PI * 2;
            e.particles.add(p);
        }
    }

    public void onWorldRender(MatrixStack matrices, float tickDelta) {
        if (effects.isEmpty()) return;
        long now = System.currentTimeMillis();

        for (Effect e : effects) {
            long elapsed = now - e.startTime;
            float life = 1f - Math.min(1f, (float) elapsed / e.lifetime);
            if (life <= 0) continue;

            int color = computeColor(e, life);

            for (Particle p : e.particles) {
                switch (p.type) {
                    case "lightning" -> renderLightning(matrices, p, color, life);
                    case "spark" -> renderSpark(matrices, p, color, life, elapsed, e.size);
                    case "crystal" -> renderCrystal(matrices, p, color, life, elapsed, e.size);
                    case "shockwave" -> renderShockwave(matrices, e.center, color, life, elapsed, e.size);
                    case "star" -> renderStar(matrices, p, color, life, elapsed, e.size);
                }
            }
        }
    }

    private int computeColor(Effect e, float life) {
        Color c;
        if (e.baseColor == null) {
            float hue = (System.currentTimeMillis() % 2000) / 2000f;
            c = Color.getHSBColor(hue, 1f, 1f);
        } else {
            c = e.baseColor;
        }
        int alpha = (int) (255 * life);
        return (alpha << 24) | (c.getRed() << 16) | (c.getGreen() << 8) | c.getBlue();
    }

    private void renderLightning(MatrixStack matrices, Particle p, int color, float life) {
        for (int i = 0; i < p.zigzag.size() - 1; i++) {
            Vec3d a = p.zigzag.get(i);
            Vec3d b = p.zigzag.get(i + 1);
            Render3DUtils.drawLine(matrices, a, b, color, 2.5f * life);
        }
    }

    private void renderSpark(MatrixStack matrices, Particle p, int color, float life, long elapsed, float size) {
        float t = elapsed / 1000f;
        Vec3d pos = p.start.add(p.velocity.multiply(t * 20)).add(0, -0.5 * t * t * 9.8 * 0.1, 0);
        double s = 0.05 * size * life;
        Render3DUtils.drawBoxFilled(matrices,
                new net.minecraft.util.math.Box(
                        pos.x - s, pos.y - s, pos.z - s,
                        pos.x + s, pos.y + s, pos.z + s
                ),
                color);
    }

    private void renderCrystal(MatrixStack matrices, Particle p, int color, float life, long elapsed, float size) {
        float t = elapsed / 1000f;
        Vec3d pos = p.start.add(p.velocity.multiply(t * 20));
        double s = 0.15 * size * life;
        var box = new net.minecraft.util.math.Box(
                pos.x - s, pos.y - s, pos.z - s,
                pos.x + s, pos.y + s, pos.z + s
        );
        Render3DUtils.drawBoxOutline(matrices, box, color, 2f);
    }

    private void renderShockwave(MatrixStack matrices, Vec3d center, int color, float life, long elapsed, float size) {
        float t = elapsed / 1000f;
        double r = t * 3.0 * size;
        int segments = 32;
        for (int i = 0; i < segments; i++) {
            double a1 = (Math.PI * 2 * i) / segments;
            double a2 = (Math.PI * 2 * (i + 1)) / segments;
            Vec3d p1 = center.add(Math.cos(a1) * r, 0.05, Math.sin(a1) * r);
            Vec3d p2 = center.add(Math.cos(a2) * r, 0.05, Math.sin(a2) * r);
            Render3DUtils.drawLine(matrices, p1, p2, color, 3f * life);
        }
    }

    private void renderStar(MatrixStack matrices, Particle p, int color, float life, long elapsed, float size) {
        float t = elapsed / 1000f;
        Vec3d pos = p.start.add(p.velocity.multiply(t * 20)).add(0, -0.5 * t * t * 9.8 * 0.05, 0);
        double s = 0.08 * size * life;

        Render3DUtils.drawLine(matrices,
                pos.add(-s, 0, 0), pos.add(s, 0, 0), color, 2f * life);
        Render3DUtils.drawLine(matrices,
                pos.add(0, -s, 0), pos.add(0, s, 0), color, 2f * life);
        Render3DUtils.drawLine(matrices,
                pos.add(0, 0, -s), pos.add(0, 0, s), color, 2f * life);
    }

    private static class Effect {
        Vec3d center;
        long startTime;
        long lifetime;
        String mode;
        float size;
        Color baseColor;
        List<Particle> particles;
    }

    private static class Particle {
        String type;
        Vec3d start;
        Vec3d end;
        Vec3d velocity;
        List<Vec3d> zigzag;
        float rotation;
    }
}
