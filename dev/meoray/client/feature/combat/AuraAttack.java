package dev.meoray.client.feature.combat;

import dev.meoray.client.core.Category;
import dev.meoray.client.core.Module;
import dev.meoray.client.core.setting.BooleanSetting;
import dev.meoray.client.core.setting.ModeSetting;
import dev.meoray.client.core.setting.NumberSetting;
import dev.meoray.client.rotation.NeuroRotation;
import dev.meoray.client.rotation.Rotation;
import dev.meoray.client.util.CriticalUtil;
import dev.meoray.client.util.RotationUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Hand;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

public class AuraAttack extends Module {

    private final NeuroRotation neuro      = new NeuroRotation();
    private final MinecraftClient mc       = MinecraftClient.getInstance();
    private final Random random            = new Random();

    // === Target ===
    public final ModeSetting targetPriority = add(new ModeSetting("Priority", "Distance", "Distance", "Health", "Armor"));
    public final NumberSetting range        = add(new NumberSetting("Range", 4.2, 2.0, 6.0, 0.05));
    public final BooleanSetting players     = add(new BooleanSetting("Players", true));
    public final BooleanSetting mobs        = add(new BooleanSetting("Mobs", false));
    public final BooleanSetting invisible   = add(new BooleanSetting("Invisible", false));
    public final BooleanSetting ignoreNaked = add(new BooleanSetting("Ignore Naked", false));
    public final BooleanSetting throughWalls= add(new BooleanSetting("Through Walls", false));

    // === Rotation ===
    public final ModeSetting rotationMode   = add(new ModeSetting("Rotation", "Neuro", "Neuro", "Matrix", "Vulcan"));
    public final BooleanSetting silent      = add(new BooleanSetting("Silent", true));
    public final NumberSetting smoothness   = add(new NumberSetting("Smoothness", 78, 40, 140, 5));
    public final NumberSetting humanization = add(new NumberSetting("Randomization", 1.65, 0.5, 4.0, 0.1));
    public final NumberSetting prediction   = add(new NumberSetting("Prediction", 1.2, 0.0, 3.5, 0.05));
    public final NumberSetting gcd          = add(new NumberSetting("GCD", 0.5, 0.0, 1.0, 0.05));
    public final ModeSetting aimPoint       = add(new ModeSetting("Aim Point", "Body", "Head", "Body", "Legs", "Dynamic"));

    // === Attack ===
    public final NumberSetting hitChance    = add(new NumberSetting("Hit Chance", 95, 60, 100, 1));
    // Cooldown threshold: 1.0 = ждать 100% cooldown, 0.9 = бить на 90%
    public final NumberSetting cooldownThr  = add(new NumberSetting("Cooldown", 0.9, 0.5, 1.0, 0.05));

    // === Criticals ===
    public final BooleanSetting criticals   = add(new BooleanSetting("Criticals", true));
    public final ModeSetting critMode       = add(new ModeSetting("Crit Mode", "Packet", "Packet", "Jump", "None"));

    // === State ===
    private Entity target       = null;
    private long lastAttackTime = 0;

    public AuraAttack() {
        super("AuraAttack", "KillAura для 1.21.4 анархии", Category.COMBAT);
    }

    @Override
    public void onTick() {
        if (mc.player == null || mc.world == null) return;

        // Применяем настройки в NeuroRotation
        neuro.setRotationMode(rotationMode.getValue());
        neuro.setSmoothness(smoothness.getValue().floatValue());
        neuro.setHumanization(humanization.getValue().floatValue());
        neuro.setPrediction(prediction.getValue().floatValue());
        neuro.setGCD(gcd.getValue().floatValue());

        // Ищем цель
        target = findBestTarget();

        if (target == null) {
            neuro.reset();
            RotationUtil.reset();
            return;
        }

        // Считаем и применяем rotation
        Rotation rot = neuro.update(target, aimPoint.getValue());

        if (silent.getValue()) {
            // Silent: только серверные повороты
            RotationUtil.setRotation(rot);
            mc.player.setYaw(rot.yaw);
            mc.player.setPitch(rot.pitch);
        } else {
            // Обычный: двигаем камеру
            mc.player.setYaw(rot.yaw);
            mc.player.setPitch(rot.pitch);
        }

        // Атакуем если можно
        if (canAttack()) {
            // Проверяем hit chance
            if (random.nextInt(100) < hitChance.getValue().intValue()) {
                doAttack();
            }
        }
    }

    /**
     * Можно ли атаковать прямо сейчас?
     * Используем реальный cooldown из 1.21.4
     */
    private boolean canAttack() {
        if (mc.player == null || target == null) return false;

        // getAttackCooldownProgress(0) возвращает 0.0 (не готов) до 1.0 (готов)
        float cooldown = mc.player.getAttackCooldownProgress(0.5f);

        // Ждём пока cooldown не достигнет нашего порога
        if (cooldown < cooldownThr.getValue().floatValue()) return false;

        // Дополнительная защита от слишком быстрых атак
        if (System.currentTimeMillis() - lastAttackTime < 100) return false;

        return true;
    }

    private void doAttack() {
        if (mc.interactionManager == null || target == null) return;

        // Крит логика
        if (criticals.getValue()) {
            switch (critMode.getValue()) {
                case "Packet" -> {
                    // Пакетный крит — работает на большинстве анархия-серверов
                    // Отправляем пакеты ДО удара
                    CriticalUtil.doCritPacket();
                }
                case "Jump" -> {
                    // Прыжковый крит — легитимнее но медленнее
                    CriticalUtil.doCritJump();
                    // Бьём только если уже в воздухе и падаем
                    if (!CriticalUtil.canCrit()) return;
                }
                // case "None" — просто бьём
            }
        }

        // Сам удар
        mc.interactionManager.attackEntity(mc.player, target);
        mc.player.swingHand(Hand.MAIN_HAND);
        lastAttackTime = System.currentTimeMillis();
    }

    // =========================================================
    // Target selection
    // =========================================================

    private Entity findBestTarget() {
        List<LivingEntity> valid = new ArrayList<>();

        for (Entity e : mc.world.getEntities()) {
            if (e instanceof LivingEntity living && isValidTarget(living)) {
                valid.add(living);
            }
        }

        if (valid.isEmpty()) return null;

        // Сортируем по приоритету
        Comparator<LivingEntity> comp = switch (targetPriority.getValue()) {
            case "Health"   -> Comparator.comparingDouble(LivingEntity::getHealth);
            case "Armor"    -> Comparator.comparingInt(LivingEntity::getArmor);
            default         -> Comparator.comparingDouble(e -> mc.player.distanceTo(e));
        };

        // Вторичный критерий — угол к цели (чтобы не прыгать между целями)
        comp = comp.thenComparingDouble(e -> RotationUtil.getAngleTo(e));

        return valid.stream().min(comp).orElse(null);
    }

    private boolean isValidTarget(LivingEntity e) {
        if (mc.player == null) return false;

        // Базовые проверки
        if (e == mc.player)  return false;
        if (e.isDead())      return false;
        if (e.getHealth() <= 0) return false;

        // Дистанция
        if (mc.player.distanceTo(e) > range.getValue()) return false;

        // Тип цели
        boolean isPlayer = e instanceof PlayerEntity;
        if (!players.getValue() && isPlayer)    return false;
        if (!mobs.getValue() && !isPlayer)      return false;

        // Invisible
        if (e.isInvisible() && !invisible.getValue()) return false;

        // Ignore naked (без брони)
        if (ignoreNaked.getValue() && e instanceof PlayerEntity p) {
            if (p.getArmor() == 0) return false;
        }

        // Through walls / raytrace
        if (!throughWalls.getValue()) {
            if (!mc.player.canSee(e)) return false;
        }

        // FOV фильтр — не атакуем если цель прямо за спиной (> 180°)
        return RotationUtil.getAngleTo(e) <= 180f;
    }

    @Override
    public void onEnable() {
        neuro.reset();
        RotationUtil.reset();
        target        = null;
        lastAttackTime = 0;
    }

    @Override
    public void onDisable() {
        neuro.reset();
        RotationUtil.reset();
        target = null;
    }
}
