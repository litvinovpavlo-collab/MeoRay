package dev.meoray.client.rotation.impl;

import dev.meoray.client.rotation.Rotation;
import dev.meoray.client.rotation.RotationBase;
import dev.meoray.client.rotation.RotationComponent;
import dev.meoray.client.util.combat.RaytracingUtil;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.MathHelper;

public class Sloth1Rotation extends RotationBase {
    private float legacyYawSpeed = 0.0F;
    private float legacyPitchSpeed = 0.0F;
    private float aimFatigue = 0.0F;
    private int legacyIdleTicks = 0;
    private int reactionDelayTicks = 0;

    @Override
    public void update(Rotation targetAngle, boolean elytraVisual) {
        update(null, targetAngle, elytraVisual);
    }

    public void update(LivingEntity target, Rotation targetAngle, boolean elytraVisual) {
        float deltaYaw = MathHelper.wrapDegrees(targetAngle.getYaw() - this.lastYaw);
        float deltaPitch = targetAngle.getPitch() - this.lastPitch;

        boolean hasTrace = false;
        if (target != null) {
            hasTrace = RaytracingUtil.rayTrace(mc.player.getRotationVector(), 999.0, target.getBoundingBox());
        }
        boolean isElytra = mc.player.isGliding();
        float angularDist = (float) Math.sqrt(deltaYaw * deltaYaw + deltaPitch * deltaPitch);

        if (!hasTrace) {
            int reactionThreshold = 4 + Math.min(this.legacyIdleTicks / 3, 8) + (int)(Math.random() * 3);
            if (this.reactionDelayTicks < reactionThreshold) {
                this.reactionDelayTicks++;
                if (Math.random() < 0.2) {
                    deltaYaw += (float)(Math.random() - 0.5) * 1.5F;
                    deltaPitch += (float)(Math.random() - 0.5) * 0.7F;
                }
            } else {
                float targetSpeed = angularDist > 12.0F ? 0.75F : 0.3F;
                this.legacyYawSpeed = MathHelper.lerp(0.03F, this.legacyYawSpeed, targetSpeed);
                this.legacyPitchSpeed = MathHelper.lerp(0.03F, this.legacyPitchSpeed, targetSpeed);
            }
            this.legacyIdleTicks = 0;
        } else {
            this.reactionDelayTicks = 0;
            this.legacyYawSpeed = MathHelper.lerp(0.55F, this.legacyYawSpeed, 0.0F);
            this.legacyPitchSpeed = MathHelper.lerp(0.55F, this.legacyPitchSpeed, 0.0F);
            this.legacyIdleTicks++;
        }

        this.aimFatigue = MathHelper.clamp(this.aimFatigue + 0.00012F, 0.0F, 0.35F);

        float baseYawSpeed = (float)(Math.random() * 6.0 + 10.0) / (isElytra ? 2.5F : 1.0F);
        float basePitchSpeed = (float)(Math.random() * 3.0 + 5.0) / (isElytra ? 2.5F : 1.0F);

        float yawS = this.legacyYawSpeed * this.legacyYawSpeed * (3.0F - 2.0F * this.legacyYawSpeed);
        float pitchS = this.legacyPitchSpeed * this.legacyPitchSpeed * (3.0F - 2.0F * this.legacyPitchSpeed);

        float yawSpeed = baseYawSpeed * yawS * (1.0F - this.aimFatigue);
        float pitchSpeed = basePitchSpeed * pitchS * (1.0F - this.aimFatigue);

        if (!mc.player.isOnGround()) {
            deltaPitch *= 0.08F;
        }

        if (hasTrace && this.legacyIdleTicks > 2 && Math.random() < 0.18) {
            deltaYaw += (float)(Math.random() - 0.5) * 2.2F;
            deltaPitch += (float)(Math.random() - 0.5) * 1.1F;
        }

        float clampedYaw = MathHelper.clamp(deltaYaw, -yawSpeed, yawSpeed);
        float clampedPitch = MathHelper.clamp(deltaPitch, -pitchSpeed, pitchSpeed);

        if (hasTrace && Math.abs(clampedYaw) < 0.5F) {
            clampedYaw = 0.0F;
        }
        if (hasTrace && Math.abs(clampedPitch) < 0.3F) {
            clampedPitch = 0.0F;
        }

        this.lastYaw = this.lastYaw + clampedYaw;
        this.lastPitch = this.lastPitch + clampedPitch;

        Rotation rot = new Rotation(this.lastYaw, this.lastPitch);
        RotationComponent.update(rot, 360.0F, 360.0F, 360.0F, 360.0F, 0, 1, elytraVisual);
    }
}
