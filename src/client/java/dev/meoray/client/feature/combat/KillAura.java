package dev.meoray.client.feature.combat;

import dev.meoray.client.core.Category;
import dev.meoray.client.core.Module;
import dev.meoray.client.core.setting.BooleanSetting;
import dev.meoray.client.core.setting.ModeSetting;
import dev.meoray.client.core.setting.NumberSetting;
import dev.meoray.client.rotation.NeuroRotation;
import dev.meoray.client.rotation.Rotation;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Hand;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class KillAura extends Module {

    private final NeuroRotation neuro = new NeuroRotation();
    private final MinecraftClient mc = MinecraftClient.getInstance();

    public final ModeSetting attackMode = add(new ModeSetting("Attack Mode", "Single", "Single", "Switch"));
    public final NumberSetting range = add(new NumberSetting("Range", 4.4, 3.0, 6.0, 0.05));
    public final ModeSetting targetPriority = add(new ModeSetting("Priority", "Distance", "Distance", "Health", "Armor"));

    public final BooleanSetting playersOnly = add(new BooleanSetting("Players Only", true));
    public final BooleanSetting mobs = add(new BooleanSetting("Mobs", false));
    public final BooleanSetting invisible = add(new BooleanSetting("Invisible", false));
    public final BooleanSetting naked = add(new BooleanSetting("Ignore Naked", true));

    public final ModeSetting rotationMode = add(new ModeSetting("Rotation Mode", "Neuro", "Neuro", "Matrix", "Vulcan"));
    public final NumberSetting smoothness = add(new NumberSetting("Smoothness", 78, 40, 140, 5));
    public final NumberSetting humanization = add(new NumberSetting("Randomization", 1.65, 0.5, 4.0, 0.1));
    public final NumberSetting prediction = add(new NumberSetting("Prediction", 1.45, 0.0, 3.5, 0.05));
    public final NumberSetting gcd = add(new NumberSetting("GCD", 0.5, 0.0, 1.0, 0.05));
    public final BooleanSetting silent = add(new BooleanSetting("Silent", true));

    public final NumberSetting hitChance = add(new NumberSetting("Hit Chance", 93, 60, 100, 1));
    public final BooleanSetting raytrace = add(new BooleanSetting("Raytrace", true));
    public final BooleanSetting throughWalls = add(new BooleanSetting("Through Walls", false));

    public final BooleanSetting criticals = add(new BooleanSetting("Criticals", true));
    public final ModeSetting aimPoint = add(new ModeSetting("Aim Point", "Head", "Head", "Body", "Legs", "Dynamic"));

    private Entity target = null;
    private long lastAttackTime = 0;

    public KillAura() {
        super("KillAura", "Funtime / Spokytime KillAura", Category.COMBAT);
    }

    @Override
    public void onTick() {
        if (mc.player == null || mc.world == null) return;

        neuro.setRotationMode(rotationMode.getValue());
        neuro.setSmoothness(smoothness.getValue().floatValue());
        neuro.setHumanization(humanization.getValue().floatValue());
        neuro.setPrediction(prediction.getValue().floatValue());
        neuro.setGCD(gcd.getValue().floatValue());

        target = findBestTarget();

        if (target == null) {
            neuro.reset();
            return;
        }

        Rotation rot = neuro.update(target, aimPoint.getValue());
        applyRotation(rot);

        if (canAttack() && Math.random() * 100 < hitChance.getValue()) {
            doAttack();
            lastAttackTime = System.currentTimeMillis();
        }
    }

    private Entity findBestTarget() {
        List<Entity> valid = new ArrayList<>();

        for (Entity e : mc.world.getEntities()) {
            if (e instanceof LivingEntity living && isValidTarget(living)) {
                valid.add(e);
            }
        }

        if (valid.isEmpty()) return null;

        Comparator<Entity> comp = switch (targetPriority.getValue()) {
            case "Health" -> Comparator.comparingDouble(en -> ((LivingEntity) en).getHealth());
            case "Armor" -> Comparator.comparingInt(en -> ((LivingEntity) en).getArmor());
            default -> Comparator.comparingDouble(mc.player::distanceTo);
        };

        return valid.stream()
                .min(comp.thenComparingDouble(this::getAngleTo))
                .orElse(null);
    }

    private boolean isValidTarget(LivingEntity e) {
        if (e == mc.player || e.isDead() || mc.player.distanceTo(e) > range.getValue()) return false;

        if (e.isInvisible() && !invisible.getValue()) return false;
        if (playersOnly.getValue() && !(e instanceof PlayerEntity)) return false;
        if (!mobs.getValue() && !(e instanceof PlayerEntity)) return false;

        if (naked.getValue() && e instanceof PlayerEntity p && p.getArmor() <= 4) return false;

        if (!throughWalls.getValue() && raytrace.getValue() && !mc.player.canSee(e)) return false;

        return getAngleTo(e) <= 165;
    }

    private double getAngleTo(Entity e) {
        double dx = e.getX() - mc.player.getX();
        double dz = e.getZ() - mc.player.getZ();
        double yaw = Math.toDegrees(Math.atan2(dz, dx)) - 90;
        return Math.abs(Math.IEEEremainder(yaw - mc.player.getYaw(), 360));
    }

    private void applyRotation(Rotation rot) {
        if (silent.getValue()) {
            mc.player.setYaw(rot.yaw);
            mc.player.setPitch(rot.pitch);
        } else {
            mc.player.setYaw(rot.yaw);
            mc.player.setPitch(rot.pitch);
        }
    }

    private boolean canAttack() {
        return System.currentTimeMillis() - lastAttackTime >= 85;
    }

    private void doAttack() {
        if (mc.interactionManager == null || target == null) return;

        if (criticals.getValue()) {
            if (mc.player.isOnGround()) {
                mc.player.setVelocity(mc.player.getVelocity().x, -0.0785, mc.player.getVelocity().z);
                mc.player.setOnGround(false);
            }
        }

        mc.interactionManager.attackEntity(mc.player, target);
        mc.player.swingHand(Hand.MAIN_HAND);
    }

    @Override
    public void onEnable() {
        neuro.reset();
        target = null;
        lastAttackTime = 0;
    }

    @Override
    public void onDisable() {
        neuro.reset();
        target = null;
    }
}
