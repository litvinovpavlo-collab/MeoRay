package dev.meoray.client.feature.combat;

import dev.meoray.client.core.Category;
import dev.meoray.client.core.Module;
import dev.meoray.client.core.setting.BooleanSetting;
import dev.meoray.client.core.setting.ModeSetting;
import dev.meoray.client.core.setting.NumberSetting;
import dev.meoray.client.rotation.NeuroRotation;
import dev.meoray.client.rotation.Rotation;
import dev.meoray.client.hud.TargetHUD;
import dev.meoray.client.util.CombatUtil;
import dev.meoray.client.util.RotationUtil;
import dev.meoray.client.util.TPSUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Hand;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

public class AttackAura extends Module {

    private final NeuroRotation neuro = new NeuroRotation();
    private final MinecraftClient mc = MinecraftClient.getInstance();
    private final Random random = new Random();

    // === SERVER PRESET (САМАЯ ВЕРХНЯЯ НАСТРОЙКА) ===
    public final ModeSetting serverPreset = add(new ModeSetting("Server Preset", "Custom",
            "Custom", "Funtime", "Spookytime", "ReallyWorld", "Holyworld"));

    // === DISTANCE ===
    public final NumberSetting distance = add(new NumberSetting("Distance", 4.0, 1.0, 6.0, 0.1));
    public final NumberSetting additionalDistance = add(new NumberSetting("Additional Distance", 2.0, 0.0, 6.0, 0.1));

    // === TARGETS ===
    public final BooleanSetting targetPlayers = add(new BooleanSetting("Target Players", true));
    public final BooleanSetting targetMobs = add(new BooleanSetting("Target Mobs", false));
    public final BooleanSetting targetAnimals = add(new BooleanSetting("Target Animals", false));
    public final BooleanSetting targetInvisible = add(new BooleanSetting("Target Invisible", false));

    // === SORTING ===
    public final ModeSetting sorting = add(new ModeSetting("Sorting", "Distance", "Distance", "Health", "Armor", "Angle"));

    // === MODE ===
    public final ModeSetting mode = add(new ModeSetting("Mode", "Silent", "Rage", "Silent"));
    public final ModeSetting attackMode = add(new ModeSetting("Attack Mode", "1.9+", "1.9+", "Legacy"));

    // === ACCURACY ===
    public final NumberSetting accuracy = add(new NumberSetting("Accuracy", 100, 1, 100, 1));

    // === CORRECTION ===
    public final ModeSetting correctionMode = add(new ModeSetting("Correction Mode", "Focused", "Focused", "Smooth", "None"));

    // === SPRINT RESET ===
    public final ModeSetting sprintReset = add(new ModeSetting("Sprint Reset", "Packet", "Packet", "Vanilla", "None"));

    // === ROTATION TUNING ===
    public final NumberSetting smoothness = add(new NumberSetting("Smoothness", 78, 40, 140, 5));
    public final NumberSetting humanization = add(new NumberSetting("Randomization", 1.65, 0.5, 4.0, 0.1));
    public final NumberSetting prediction = add(new NumberSetting("Prediction", 1.2, 0.0, 3.5, 0.05));
    public final NumberSetting gcd = add(new NumberSetting("GCD", 0.5, 0.0, 1.0, 0.05));

    // === BOOLEANS ===
    public final BooleanSetting ignoreWhileUsing = add(new BooleanSetting("Ignore While Using", true));
    public final BooleanSetting throughWalls = add(new BooleanSetting("Through Walls", false));
    public final BooleanSetting onlyCriticals = add(new BooleanSetting("Only Criticals", false));
    public final BooleanSetting smartCriticals = add(new BooleanSetting("Smart Criticals", true));
    public final BooleanSetting jumpOnly = add(new BooleanSetting("Jump Only", false));
    public final BooleanSetting swordOnly = add(new BooleanSetting("Sword Only", false));
    public final BooleanSetting tpsSync = add(new BooleanSetting("TPS Sync", true));
    public final BooleanSetting breakShield = add(new BooleanSetting("Break Shield", true));
    public final BooleanSetting desyncShield = add(new BooleanSetting("Desync Shield", false));
    public final BooleanSetting criticalEffect = add(new BooleanSetting("Critical Effect", false));

    // === STATE ===
    private Entity target = null;
    private long lastAttackTime = 0;
    private boolean wasSprinting = false;
    private String lastPreset = "Custom";

    public AttackAura() {
        super("AttackAura", "Автоматически бьёт игроков", Category.COMBAT);
    }

    /**
     * Применяет настройки из выбранного пресета сервера
     */
    private void applyPresetIfChanged() {
        String currentPreset = serverPreset.getValue();
        if (currentPreset.equals(lastPreset)) return;
        lastPreset = currentPreset;

        if (currentPreset.equals("Custom")) return;

        AuraPresets.Preset p = AuraPresets.getByName(currentPreset);
        if (p == null) return;

        distance.setValue(Double.valueOf(p.distance));
        additionalDistance.setValue(Double.valueOf(p.additionalDistance));
        mode.setValue(p.mode);
        attackMode.setValue(p.attackMode);
        accuracy.setValue(Double.valueOf(p.accuracy));
        sprintReset.setValue(p.sprintReset);
        smoothness.setValue(Double.valueOf(p.smoothness));
        humanization.setValue(Double.valueOf(p.humanization));
        prediction.setValue(Double.valueOf(p.prediction));
        gcd.setValue(Double.valueOf(p.gcd));
        smartCriticals.setValue(p.smartCriticals);
        onlyCriticals.setValue(p.onlyCriticals);
        tpsSync.setValue(p.tpsSync);
        breakShield.setValue(p.breakShield);
        desyncShield.setValue(p.desyncShield);

        System.out.println("[AttackAura] Applied preset: " + currentPreset);
    }

    @Override
    public void onTick() {
        if (mc.player == null || mc.world == null) return;

        // Проверяем не сменился ли пресет
        applyPresetIfChanged();

        if (swordOnly.getValue() && !CombatUtil.isHoldingSword()) {
            cleanup();
            return;
        }

        if (ignoreWhileUsing.getValue() && CombatUtil.isUsingItem()) {
            cleanup();
            return;
        }

        neuro.setRotationMode(mode.getValue().equals("Rage") ? "Matrix" : "Neuro");
        neuro.setSmoothness(smoothness.getValue().floatValue());
        neuro.setHumanization(humanization.getValue().floatValue());
        neuro.setPrediction(prediction.getValue().floatValue());
        neuro.setGCD(gcd.getValue().floatValue());

        target = findBestTarget();
        if (target instanceof LivingEntity living) TargetHUD.setTarget(living);
        if (target == null) {
            cleanup();
            return;
        }

        Rotation rot = neuro.update(target, "Body");
        applyRotation(rot);

        handleAttack();
    }

    private void applyRotation(Rotation rot) {
        if (mode.getValue().equals("Silent")) {
            RotationUtil.setRotation(rot);
        } else {
            mc.player.setYaw(rot.yaw);
            mc.player.setPitch(rot.pitch);
            RotationUtil.setRotation(rot);
        }
    }

    private void handleAttack() {
        if (mc.player == null || target == null) return;

        if (mc.player.distanceTo(target) > distance.getValue()) return;

        if (attackMode.getValue().equals("1.9+")) {
            float cooldown = mc.player.getAttackCooldownProgress(0.5f);
            if (cooldown < 0.92f) return;
        }

        long minInterval = 100;
        if (tpsSync.getValue()) {
            float mult = TPSUtil.getTPSMultiplier();
            minInterval = (long)(minInterval / Math.max(0.1f, mult));
        }
        if (System.currentTimeMillis() - lastAttackTime < minInterval) return;

        // === КРИТЫ ===
        if (onlyCriticals.getValue()) {
            if (jumpOnly.getValue() && !mc.options.jumpKey.isPressed()) return;

            if (mc.player.isOnGround()) {
                if (!jumpOnly.getValue()) mc.player.jump();
                return;
            }

            if (!canCrit()) return;
        }
        else if (smartCriticals.getValue()) {
            if (!mc.player.isOnGround() && mc.player.getVelocity().y > 0) {
                return; // летим вверх — ждём
            }
        }

        if (random.nextInt(100) >= accuracy.getValue().intValue()) return;

        if (target instanceof PlayerEntity p && CombatUtil.targetHoldingShield(p)) {
            if (desyncShield.getValue()) CombatUtil.desyncShield();
        }

        doSprintReset();
        doAttack();
    }

    private boolean canCrit() {
        if (mc.player == null) return false;
        return !mc.player.isOnGround()
                && mc.player.getVelocity().y < 0
                && !mc.player.isTouchingWater()
                && !mc.player.isInLava()
                && !mc.player.isClimbing()
                && !mc.player.isRiding();
    }

    private void doSprintReset() {
        if (mc.player == null) return;
        switch (sprintReset.getValue()) {
            case "Packet" -> {
                if (mc.player.isSprinting()) {
                    CombatUtil.sprintResetPacket();
                    wasSprinting = true;
                }
            }
            case "Vanilla" -> {
                if (mc.player.isSprinting()) {
                    CombatUtil.sprintResetVanilla();
                    wasSprinting = true;
                }
            }
        }
    }

    private void doAttack() {
        if (mc.interactionManager == null || target == null) return;

        mc.interactionManager.attackEntity(mc.player, target);
        mc.player.swingHand(Hand.MAIN_HAND);
        lastAttackTime = System.currentTimeMillis();

        if (wasSprinting && mc.options.sprintKey.isPressed()) {
            mc.player.setSprinting(true);
            wasSprinting = false;
        }
    }

    private Entity findBestTarget() {
        List<LivingEntity> valid = new ArrayList<>();
        double searchRange = distance.getValue() + additionalDistance.getValue();

        for (Entity e : mc.world.getEntities()) {
            if (!(e instanceof LivingEntity living)) continue;
            if (!isValidTarget(living, searchRange)) continue;
            valid.add(living);
        }

        if (valid.isEmpty()) return null;

        Comparator<LivingEntity> comp = switch (sorting.getValue()) {
            case "Health" -> Comparator.comparingDouble(LivingEntity::getHealth);
            case "Armor"  -> Comparator.comparingInt(LivingEntity::getArmor);
            case "Angle"  -> Comparator.comparingDouble(e -> RotationUtil.getAngleTo(e));
            default       -> Comparator.comparingDouble(e -> mc.player.distanceTo(e));
        };

        return valid.stream().min(comp).orElse(null);
    }

    private boolean isValidTarget(LivingEntity e, double maxDist) {
        if (mc.player == null) return false;
        if (e == mc.player) return false;
        if (e.isDead() || e.getHealth() <= 0) return false;
        if (mc.player.distanceTo(e) > maxDist) return false;

        boolean isPlayer = e instanceof PlayerEntity;
        boolean isAnimal = e instanceof AnimalEntity;
        boolean isMob = e instanceof MobEntity && !isAnimal;

        if (isPlayer && !targetPlayers.getValue()) return false;
        if (isMob && !targetMobs.getValue()) return false;
        if (isAnimal && !targetAnimals.getValue()) return false;
        if (!isPlayer && !isMob && !isAnimal) return false;

        if (e.isInvisible() && !targetInvisible.getValue()) return false;
        if (!throughWalls.getValue() && !mc.player.canSee(e)) return false;

        return true;
    }

    private void cleanup() {
        neuro.reset();
        RotationUtil.reset();
        target = null;
    }

    @Override
    public void onEnable() {
        cleanup();
        lastAttackTime = 0;
        wasSprinting = false;
        lastPreset = "Custom"; // сброс чтобы пресет применился заново
        TPSUtil.reset();
    }

    @Override
    public void onDisable() {
        cleanup();
    }
}
