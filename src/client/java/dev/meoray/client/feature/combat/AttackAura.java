package dev.meoray.client.feature.combat;

import dev.meoray.client.MeoRayClient;
import dev.meoray.client.core.Category;
import dev.meoray.client.core.Module;
import dev.meoray.client.core.setting.BooleanSetting;
import dev.meoray.client.core.setting.GroupSetting;
import dev.meoray.client.core.setting.ModeSetting;
import dev.meoray.client.core.setting.NumberSetting;
import dev.meoray.client.rotation.Rotation;
import dev.meoray.client.rotation.RotationBase;
import dev.meoray.client.rotation.RotationComponent;
import dev.meoray.client.rotation.impl.FunTimeRotation;
import dev.meoray.client.rotation.impl.HVHRotation;
import dev.meoray.client.rotation.impl.ReallyWorldRotation;
import dev.meoray.client.rotation.impl.Sloth1Rotation;
import dev.meoray.client.rotation.impl.Sloth2Rotation;
import dev.meoray.client.rotation.impl.UniversalRotation;
import dev.meoray.client.rotation.impl.VanillaRotation;
import dev.meoray.client.util.MovingUtil;
import dev.meoray.client.util.combat.MultipointUtils;
import dev.meoray.client.util.combat.RaytracingUtil;
import dev.meoray.client.util.predict.PredictUtils;
import dev.meoray.client.utils.render.Render3DUtils;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.mob.AmbientEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.passive.FishEntity;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public final class AttackAura extends Module {
    public static AttackAura INSTANCE;

    private final GroupSetting targetTypeSetting;
    public final ModeSetting rotationMode;
    private final GroupSetting targetPriority;
    private final BooleanSetting predictOnElytra;
    private final ModeSetting predictionType;
    private final NumberSetting predict;
    private final ModeSetting correction;
    private final NumberSetting distance;
    private final NumberSetting distanceRotation;
    private final BooleanSetting shieldBreak;
    private final BooleanSetting legitSwap;
    private final BooleanSetting raycastCheck;
    private final BooleanSetting doubleAttack;
    public final BooleanSetting critsOnlyWithSpace;
    private final BooleanSetting visualElytraRotation;
    private final BooleanSetting keepTarget;
    private final BooleanSetting sprintReset;
    private final BooleanSetting visualizePrediction;

    private final VanillaRotation rotVanilla = new VanillaRotation();
    private final FunTimeRotation rotFunTime = new FunTimeRotation();
    private final UniversalRotation rotUniversal = new UniversalRotation();
    private final Sloth1Rotation rotSloth1 = new Sloth1Rotation();
    private final Sloth2Rotation rotSloth2 = new Sloth2Rotation();
    private final ReallyWorldRotation rotReallyWorld = new ReallyWorldRotation();
    private final HVHRotation rotHVH = new HVHRotation();

    private LivingEntity target;
    private Vec3d lastPredictedPoint;
    private Vec3d smoothedAimPoint;
    private Vec3d smoothedTargetVelocity = Vec3d.ZERO;
    private final Timer hurtTimer = new Timer();
    private int lastSlot = -1;
    public float lastYaw;
    public float lastPitch;
    private int postAttackTicks;
    private boolean needSprintReset;
    private boolean sprintResetDone;
    private int sprintResetTicks;
    private int targetLostTicks;

    public AttackAura() {
        super("AttackAura", "Wyvern DLC combat aura", Category.COMBAT);
        INSTANCE = this;

        targetTypeSetting = add(new GroupSetting("TargetType", "Target types", "Players")
            .add("Players", true).add("Mobs", false).add("Animals", false));

        rotationMode = add(new ModeSetting("Rotation", "Vanilla",
            "Vanilla", "FunTime", "Universal (OLD)", "Sloth1", "Sloth2", "ReallyWorld", "HVH"));

        targetPriority = add(new GroupSetting("Priority", "Sort priority", "Health", "Distance")
            .add("Health", true).add("Distance", true).add("Vision", false));

        predictOnElytra = add(new BooleanSetting("PredictOnElytra", true));

        predictionType = add(new ModeSetting("PredictionType", "Ticks", "Ticks", "HitboxOffset")
            .visibleWhen(() -> predictOnElytra.getValue()));

        predict = add(new NumberSetting("Predict", 2.0, 1.0, 8.0, 0.1)
            .visibleWhen(() -> predictOnElytra.getValue()));

        correction = add(new ModeSetting("Correction", "Free", "Focused", "Free", "None"));

        distance = add(new NumberSetting("Distance", 3.0, 0.5, 6.0, 0.1));
        distanceRotation = add(new NumberSetting("PreAim", 0.1, 0.0, 6.0, 0.1));

        shieldBreak = add(new BooleanSetting("ShieldBreak", true));
        legitSwap = add(new BooleanSetting("LegitSwap", true)
            .visibleWhen(() -> shieldBreak.getValue()));
        raycastCheck = add(new BooleanSetting("RaycastCheck", false));
        doubleAttack = add(new BooleanSetting("DoubleAttack", true));
        critsOnlyWithSpace = add(new BooleanSetting("OnlyCrit", false));
        visualElytraRotation = add(new BooleanSetting("VisualElytraRotation", true));
        keepTarget = add(new BooleanSetting("KeepTarget", true));
        sprintReset = add(new BooleanSetting("SprintReset", true));
        visualizePrediction = add(new BooleanSetting("VisualizePrediction", true));
        target = null;
        lastSlot = -1;
    }

    // ===== ON TICK =====

    @Override
    public void onTick() {
        if (mc.player == null || mc.world == null) return;

        if (sprintResetDone) {
            sprintResetTicks++;
        }

        LivingEntity newTarget = updateTarget();
        if (keepTarget.getValue() && target != null && isValid(target)) {
            // keep current
        } else if (newTarget != null) {
            target = newTarget;
            targetLostTicks = 0;
        } else if (target != null && !isValid(target)) {
            targetLostTicks++;
            if (targetLostTicks > 5) {
                target = null;
            }
        }

        if (target != null) {
            if (isCanAttack() && hurtTimer.finished(458L) && !target.isBlocking()) {
                if (shouldPrepareSprintReset()) return;

                mc.interactionManager.attackEntity(mc.player, target);
                mc.player.swingHand(Hand.MAIN_HAND);
                postAttackTicks = 7;
                resetSprintResetState();
                hurtTimer.reset();
            }
        }

        if (!isElytraPredictActive()) {
            resetElytraPredictState();
        }
    }

    // ===== ON TICK MOVEMENT =====

    @Override
    public void onTickMovement() {
        if (mc.player == null || mc.world == null) return;
        if (target != null && target.isBlocking() && hurtTimer.finished(200L)) {
            breakShieldAndAttack();
            hurtTimer.reset();
        }
    }

    // ===== EVENT ROTATE =====

    @Override
    public void eventRotate() {
        if (target == null) {
            RotationComponent.setAiming(false);
            resetElytraPredictState();
            return;
        }
        RotationComponent.setAiming(true);

        Vec3d eyes = mc.player.getEyePos();
        boolean elytraPredictActive = isElytraPredictActive();
        boolean hitboxOffsetMode = predictionType.getValue().equals("HitboxOffset");
        boolean elytraVisual = elytraPredictActive && visualElytraRotation.getValue();

        Vec3d point = MultipointUtils.getMultipoint(target, distance.getValue());

        if (elytraPredictActive) {
            double distToTarget = mc.player.getEyePos().distanceTo(target.getBoundingBox().getCenter());
            float pingFactor = getPlayerPing() / 1000.0F;
            float basePrediction = distToTarget > 8.0D ? 8.0F : predict.getValue().floatValue();
            float adjustedPrediction = basePrediction + pingFactor * 2.0F;

            Vec3d targetVel = target.getVelocity();
            Vec3d playerVel = mc.player.getVelocity();
            double relativeSpeed = playerVel.subtract(targetVel).horizontalLength();
            if (relativeSpeed > 1.5) {
                adjustedPrediction += (float)(relativeSpeed * 0.3);
            }

            if (predictionType.getValue().equals("Ticks")) {
                point = PredictUtils.predict(target, target.getPos(), adjustedPrediction);
                point = point.add(targetVel.x * pingFactor * 3, targetVel.y * pingFactor, targetVel.z * pingFactor * 3);
                smoothedAimPoint = point;
            } else {
                point = calculateHitboxOffsetPoint(eyes, pingFactor, adjustedPrediction, distToTarget);
            }
        }

        lastPredictedPoint = point;

        Rotation angle = Rotation.lookingAt(point, eyes);

        RotationBase currentRot = null;
        String mode = rotationMode.getValue();
        if (mode.equals("Vanilla")) currentRot = rotVanilla;
        else if (mode.equals("ReallyWorld")) currentRot = rotReallyWorld;
        else if (mode.equals("FunTime")) currentRot = rotFunTime;
        else if (mode.equals("Universal (OLD)")) currentRot = rotUniversal;
        else if (mode.equals("Sloth1")) currentRot = rotSloth1;
        else if (mode.equals("Sloth2")) currentRot = rotSloth2;
        else if (mode.equals("HVH")) currentRot = rotHVH;

        if (currentRot != null) {
            currentRot.setYaw(lastYaw);
            currentRot.setPitch(lastPitch);

            if (currentRot instanceof FunTimeRotation fr) {
                fr.update(target, angle, elytraVisual);
            } else if (currentRot instanceof ReallyWorldRotation rr) {
                rr.update(target, angle, elytraVisual);
            } else if (currentRot instanceof UniversalRotation ur) {
                ur.update(target, angle, elytraVisual);
            } else if (currentRot instanceof Sloth1Rotation sr) {
                sr.update(target, angle, elytraVisual);
            } else if (currentRot instanceof Sloth2Rotation sr) {
                sr.update(target, angle, elytraVisual);
            } else {
                currentRot.update(angle, elytraVisual);
            }

            lastYaw = currentRot.getYaw();
            lastPitch = currentRot.getPitch();
        }
    }

    // ===== ON MOVE INPUT =====

    @Override
    public void onMoveInput() {
        if (needSprintReset) {
            mc.player.input.movementForward = 0.0F;
            mc.player.input.movementSideways = 0.0F;
            needSprintReset = false;
            sprintResetDone = true;
            sprintResetTicks = 0;
            mc.player.setSprinting(false);
            return;
        }

        if (!correction.getValue().equals("None") && target != null) {
            if (correction.getValue().equals("Focused")) {
                MovingUtil.fixMovementFocus(mc.player.getYaw());
            } else {
                MovingUtil.fixMovementFree();
            }
        }
    }

    // ===== ELYTRA PREDICTION =====

    private boolean isElytraPredictContext() {
        return mc.player != null
                && mc.player.isGliding()
                && target != null
                && target instanceof PlayerEntity
                && target.isGliding();
    }

    private boolean isElytraPredictActive() {
        return isEnabled()
                && predictOnElytra.getValue()
                && isElytraPredictContext();
    }

    private void resetElytraPredictState() {
        lastPredictedPoint = null;
        smoothedAimPoint = null;
        smoothedTargetVelocity = Vec3d.ZERO;
    }

    private Vec3d calculateHitboxOffsetPoint(Vec3d eyes, float pingFactor, float adjustedPrediction, double distToTarget) {
        Vec3d velocity = target.getVelocity();
        Vec3d playerVel = mc.player.getVelocity();
        Vec3d relativeVel = velocity.subtract(playerVel);
        smoothedTargetVelocity = smoothedTargetVelocity.multiply(0.55).add(velocity.multiply(0.45));

        double speed = smoothedTargetVelocity.horizontalLength();
        double relativeSpeed = relativeVel.horizontalLength();

        Vec3d targetDir;
        if (speed >= 0.05) {
            targetDir = new Vec3d(
                smoothedTargetVelocity.x,
                smoothedTargetVelocity.y * 0.42,
                smoothedTargetVelocity.z
            ).normalize();
        } else {
            targetDir = Vec3d.fromPolar(target.getPitch() * 0.35F, target.getYaw()).normalize();
        }

        double interceptFactor = MathHelper.clamp(distToTarget / Math.max(relativeSpeed + speed, 0.75), 0.35, 2.5);
        double pingStrength = pingFactor * Math.min(speed * 4.5 + relativeSpeed * 1.15, 5.5);
        double baseStrength = predict.getValue() * (0.92 + Math.min(distToTarget / 14.0, 0.65));
        double totalStrength = MathHelper.clamp(
            baseStrength + pingStrength + relativeSpeed * 0.42 + adjustedPrediction * 0.18,
            1.25, 10.0);

        Vec3d offset = targetDir.multiply(totalStrength);
        offset = offset.add(relativeVel.multiply(interceptFactor * (0.85 + pingFactor * 1.35)));

        double maxOffset = Math.max(distToTarget * 0.58, 2.0);
        if (offset.length() > maxOffset) {
            offset = offset.normalize().multiply(maxOffset);
        }

        Vec3d rawPoint = target.getBoundingBox().getCenter().add(offset);
        if (smoothedAimPoint == null) {
            smoothedAimPoint = rawPoint;
        } else {
            double smooth = MathHelper.clamp(0.18 + speed * 0.14 + relativeSpeed * 0.06, 0.18, 0.55);
            smoothedAimPoint = new Vec3d(
                MathHelper.lerp(smooth, smoothedAimPoint.x, rawPoint.x),
                MathHelper.lerp(smooth, smoothedAimPoint.y, rawPoint.y),
                MathHelper.lerp(smooth, smoothedAimPoint.z, rawPoint.z));
        }
        return smoothedAimPoint;
    }

    // ===== SHIELD BREAK =====

    private void breakShieldAndAttack() {
        boolean wasSwapped = false;
        boolean wasSwappedInventory = false;

        int slotHotbar = findItemInHotbar(Items.WOODEN_AXE, Items.STONE_AXE, Items.IRON_AXE,
            Items.GOLDEN_AXE, Items.DIAMOND_AXE, Items.NETHERITE_AXE);
        int slotInventory = findItemInInventory(Items.WOODEN_AXE, Items.STONE_AXE, Items.IRON_AXE,
            Items.GOLDEN_AXE, Items.DIAMOND_AXE, Items.NETHERITE_AXE);

        if (shouldPrepareSprintReset()) return;

        if (slotHotbar != -1 && shieldBreak.getValue() && target.isBlocking()) {
            if (legitSwap.getValue()) {
                lastSlot = mc.player.getInventory().selectedSlot;
                mc.player.getInventory().selectedSlot = slotHotbar;
            } else {
                mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(slotHotbar));
            }
            wasSwapped = true;
        }

        if (slotHotbar == -1 && slotInventory != -1 && shieldBreak.getValue() && target.isBlocking()) {
            if (legitSwap.getValue()) {
                mc.interactionManager.clickSlot(0, slotInventory, 8, SlotActionType.SWAP, mc.player);
                mc.getNetworkHandler().sendPacket(new CloseHandledScreenC2SPacket(0));
                lastSlot = mc.player.getInventory().selectedSlot;
                mc.player.getInventory().selectedSlot = 8;
            } else {
                mc.interactionManager.clickSlot(0, slotInventory, 8, SlotActionType.SWAP, mc.player);
                mc.getNetworkHandler().sendPacket(new CloseHandledScreenC2SPacket(0));
                mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(slotHotbar));
            }
            wasSwappedInventory = true;
        }

        mc.interactionManager.attackEntity(mc.player, target);
        if (doubleAttack.getValue()) {
            mc.interactionManager.attackEntity(mc.player, target);
        }
        mc.player.swingHand(Hand.MAIN_HAND);
        postAttackTicks = 7;
        resetSprintResetState();

        if (wasSwapped) {
            if (legitSwap.getValue()) {
                mc.player.getInventory().selectedSlot = lastSlot;
                lastSlot = -1;
            } else {
                mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(mc.player.getInventory().selectedSlot));
            }
        }

        if (wasSwappedInventory) {
            if (legitSwap.getValue()) {
                mc.player.getInventory().selectedSlot = lastSlot;
                mc.interactionManager.clickSlot(0, slotInventory, 8, SlotActionType.SWAP, mc.player);
                mc.getNetworkHandler().sendPacket(new CloseHandledScreenC2SPacket(0));
                lastSlot = -1;
            } else {
                mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(mc.player.getInventory().selectedSlot));
                mc.interactionManager.clickSlot(0, slotInventory, 8, SlotActionType.SWAP, mc.player);
                mc.getNetworkHandler().sendPacket(new CloseHandledScreenC2SPacket(0));
            }
        }
    }

    // ===== IS CAN ATTACK =====

    private boolean isCanAttack() {
        if (mc.player.getAttackCooldownProgress(0.5F) < 0.9F) return false;

        if (critsOnlyWithSpace.getValue()) {
            if (!mc.player.isOnGround() && mc.player.getVelocity().y > 0.0) {
                return false;
            }
        }

        if (target instanceof PlayerEntity && predictOnElytra.getValue() && mc.player.isGliding() && target.isGliding()) {
            float pingFactor = getPlayerPing() / 1000.0F;
            double extraReach = Math.min(pingFactor * mc.player.getVelocity().length() * 2.5, 2.0);
            double maxDist = distance.getValue() + extraReach;
            double distToPredicted = mc.player.getEyePos().distanceTo(
                lastPredictedPoint != null ? lastPredictedPoint : target.getBoundingBox().getCenter());
            double distToActual = mc.player.getEyePos().distanceTo(target.getBoundingBox().getCenter());
            if (distToPredicted > maxDist && distToActual > maxDist) return false;
        } else if ((!mc.player.isGliding() || !target.isGliding()) &&
            mc.player.getEyePos().distanceTo(MultipointUtils.getNearestPoint(target, distance.getValue())) > distance.getValue()) {
            return false;
        }

        if (raycastCheck.getValue()) {
            return RaytracingUtil.rayTrace(mc.player.getRotationVector(), distance.getValue(), target.getBoundingBox())
                || mc.targetedEntity != null;
        }
        return true;
    }

    // ===== TARGET UPDATE =====

    private LivingEntity updateTarget() {
        List<LivingEntity> targets = new ArrayList<>();

        for (PlayerEntity player : mc.world.getPlayers()) {
            if (isValid(player)) {
                targets.add(player);
            }
        }

        try {
            for (Entity entity : mc.world.getEntities()) {
                if (entity instanceof LivingEntity living && !(entity instanceof PlayerEntity)) {
                    if (isValid(living)) {
                        targets.add(living);
                    }
                }
            }
        } catch (Exception ignored) {}

        if (targets.isEmpty()) {
            MultipointUtils.reset();
            return null;
        }

        targets.sort(Comparator.comparingDouble(e -> {
            double score = 0;
            if (targetPriority.get("Health")) score += e.getHealth();
            if (targetPriority.get("Distance")) score += mc.player.squaredDistanceTo(e) * 0.1;
            if (targetPriority.get("Vision")) {
                Rotation vec = Rotation.lookingAt(e.getBoundingBox().getCenter(), mc.player.getEyePos());
                double dy = Math.abs(MathHelper.wrapDegrees(vec.getYaw() - mc.player.getYaw()));
                double dp = Math.abs(MathHelper.wrapDegrees(vec.getPitch() - mc.player.getPitch()));
                score += (dy + dp) * 0.5;
            }
            return score;
        }));

        LivingEntity best = targets.get(0);
        if (best != target) {
            MultipointUtils.reset();
        }
        return best;
    }

    public boolean isValid(LivingEntity entity) {
        if (entity == mc.player) return false;
        if (!entity.isAlive() || entity.getHealth() <= 0.0F) return false;
        if (!mc.player.isAlive() || mc.player.getHealth() <= 0.0F) return false;

        if (entity instanceof PlayerEntity player) {
            if (!targetTypeSetting.get("Players")) return false;
            if (MeoRayClient.INSTANCE.getFriendManager().isFriend(entity.getName().getString())) return false;
        }

        if ((entity instanceof PassiveEntity || entity instanceof FishEntity)
            && !targetTypeSetting.get("Animals")) return false;

        if ((entity instanceof HostileEntity || entity instanceof AmbientEntity)
            && !targetTypeSetting.get("Mobs")) return false;

        double maxDist = mc.player.isGliding() ? 20.0F : distance.getValue() + distanceRotation.getValue();
        if (mc.player.getEyePos().distanceTo(MultipointUtils.getNearestPoint(entity, maxDist)) > maxDist) return false;

        return !(entity instanceof ArmorStandEntity);
    }

    // ===== SPRINT RESET =====

    private boolean shouldPrepareSprintReset() {
        if (!sprintReset.getValue() || !mc.player.isSprinting() || shouldSkipSprintResetInWater()) return false;
        if (sprintResetDone) return sprintResetTicks < 1;
        needSprintReset = true;
        return true;
    }

    private boolean shouldSkipSprintResetInWater() {
        return mc.player != null && (mc.player.isTouchingWater() || mc.player.isSubmergedInWater());
    }

    private void resetSprintResetState() {
        needSprintReset = false;
        sprintResetDone = false;
        sprintResetTicks = 0;
    }

    // ===== UTILITY =====

    private int findItemInHotbar(Item... items) {
        for (int i = 0; i < 9; i++) {
            var stack = mc.player.getInventory().getStack(i);
            for (var item : items) {
                if (stack.getItem() == item) return i;
            }
        }
        return -1;
    }

    private int findItemInInventory(Item... items) {
        for (int i = 9; i < 36; i++) {
            var stack = mc.player.getInventory().getStack(i);
            for (var item : items) {
                if (stack.getItem() == item) return i;
            }
        }
        return -1;
    }

    private int getPlayerPing() {
        if (mc.getNetworkHandler() != null && mc.player != null) {
            PlayerListEntry entry = mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid());
            if (entry != null) return entry.getLatency();
        }
        return 50;
    }

    // ===== RENDER =====

    public void onWorldRender(MatrixStack matrices, float tickDelta) {
        if (mc.player == null || mc.world == null || target == null) return;

        int color = 0xFF00FF00;
        int predColor = 0xFFFFFF00;
        int mpColor = 0xFF00FFFF;

        Render3DUtils.drawBoxOutline(matrices, target.getBoundingBox(), color, 1.5F);

        if (visualizePrediction.getValue() && isElytraPredictActive() && lastPredictedPoint != null) {
            Box predBox = new Box(
                lastPredictedPoint.x - 0.3, lastPredictedPoint.y - 0.3, lastPredictedPoint.z - 0.3,
                lastPredictedPoint.x + 0.3, lastPredictedPoint.y + 0.3, lastPredictedPoint.z + 0.3);
            Render3DUtils.drawBoxOutline(matrices, predBox, predColor, 1.5F);
        }

        if (lastPredictedPoint != null) {
            double s = 0.08;
            Box mpBox = new Box(
                lastPredictedPoint.x - s, lastPredictedPoint.y - s, lastPredictedPoint.z - s,
                lastPredictedPoint.x + s, lastPredictedPoint.y + s, lastPredictedPoint.z + s);
            Render3DUtils.drawBoxOutline(matrices, mpBox, mpColor, 2.0F);
        }
    }

    // ===== MODULE LIFECYCLE =====

    @Override
    public void onEnable() {
        target = null;
        lastPredictedPoint = null;
        smoothedAimPoint = null;
        smoothedTargetVelocity = Vec3d.ZERO;
        resetSprintResetState();
        RotationComponent.setAiming(false);
        if (mc.player == null) return;

        rotationMode.setValue("Vanilla");
        correction.setValue("Free");

        for (var rot : new RotationBase[]{rotVanilla, rotFunTime, rotUniversal, rotSloth1, rotSloth2, rotReallyWorld, rotHVH}) {
            rot.setYaw(mc.player.getYaw());
            rot.setPitch(mc.player.getPitch());
        }
        lastYaw = mc.player.getYaw();
        lastPitch = mc.player.getPitch();
    }

    @Override
    public void onDisable() {
        resetElytraPredictState();
        resetSprintResetState();
        RotationComponent.setAiming(false);
        target = null;
        MultipointUtils.reset();
    }

    public LivingEntity getTarget() {
        return isEnabled() ? target : null;
    }

    // ===== TIMER =====

    private static final class Timer {
        private long millis;

        Timer() {
            reset();
        }

        boolean finished(long delay) {
            return System.currentTimeMillis() - delay >= millis;
        }

        void reset() {
            millis = System.currentTimeMillis();
        }
    }
}
