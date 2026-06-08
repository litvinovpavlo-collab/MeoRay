package dev.meoray.client.rotation.impl;

import dev.meoray.client.rotation.Rotation;
import dev.meoray.client.rotation.RotationBase;
import dev.meoray.client.rotation.RotationComponent;
import dev.meoray.client.util.combat.RaytracingUtil;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.MathHelper;

public class Sloth2Rotation extends RotationBase {
    private float legacyYawSpeed = 0.0F;
    private float legacyPitchSpeed = 0.0F;
    private float aimFatigue = 0.0F;
    private int legacyIdleTicks = 0;
    private int reactionDelayTicks = 0;

    public void update(LivingEntity target, Rotation targetAngle, boolean elytraVisual) {
        float deltaYaw = MathHelper.wrapDegrees(targetAngle.getYaw() - this.lastYaw);
        float deltaPitch = targetAngle.getPitch() - this.lastPitch;

        boolean hasTrace = RaytracingUtil.rayTrace(mc.player.getRotationVector(), 999.0, target.getBoundingBox());
        boolean isElytra = mc.player.isGliding();
        float angularDist = (float) Math.sqrt(deltaYaw * deltaYaw + deltaPitch * deltaPitch);

        if (!hasTrace) {
            int reactionThreshold = 3 + Math.min(this.legacyIdleTicks / 4, 6) + (int)(Math.random() * 2);
            if (this.reactionDelayTicks < reactionThreshold) {
                this.reactionDelayTicks++;
                if (Math.random() < 0.25) {
                    deltaYaw += (float)(Math.random() - 0.5) * 2.0F;
                    deltaPitch += (float)(Math.random() - 0.5) * 0.9F;
                }
            } else {
                float targetSpeed = angularDist > 10.0F ? 0.85F : 0.4F;
                this.legacyYawSpeed = MathHelper.lerp(0.04F, this.legacyYawSpeed, targetSpeed);
                this.legacyPitchSpeed = MathHelper.lerp(0.04F, this.legacyPitchSpeed, targetSpeed);
            }
            this.legacyIdleTicks = 0;
        } else {
            this.reactionDelayTicks = 0;
            this.legacyYawSpeed = MathHelper.lerp(0.65F, this.legacyYawSpeed, 0.0F);
            this.legacyPitchSpeed = MathHelper.lerp(0.65F, this.legacyPitchSpeed, 0.0F);
            this.legacyIdleTicks++;
        }

        this.aimFatigue = MathHelper.clamp(this.aimFatigue + 0.00015F, 0.0F, 0.40F);

        float baseYawSpeed = (float)(Math.random() * 7.0 + 12.0) / (isElytra ? 2.0F : 1.0F);
        float basePitchSpeed = (float)(Math.random() * 4.0 + 6.0) / (isElytra ? 2.0F : 1.0F);

        float yawS = this.legacyYawSpeed * this.legacyYawSpeed * (3.0F - 2.0F * this.legacyYawSpeed);
        float pitchS = this.legacyPitchSpeed * this.legacyPitchSpeed * (3.0F - 2.0F * this.legacyPitchSpeed);

        float yawSpeed = baseYawSpeed * yawS * (1.0F - this.aimFatigue);
        float pitchSpeed = basePitchSpeed * pitchS * (1.0F - this.aimFatigue);

        if (!mc.player.isOnGround()) {
            deltaPitch *= 0.10F;
        }

        if (hasTrace && this.legacyIdleTicks > 2 && Math.random() < 0.20) {
            deltaYaw += (float)(Math.random() - 0.5) * 2.8F;
            deltaPitch += (float)(Math.random() - 0.5) * 1.4F;
        }

        float clampedYaw = MathHelper.clamp(deltaYaw, -yawSpeed, yawSpeed);
        float clampedPitch = MathHelper.clamp(deltaPitch, -pitchSpeed, pitchSpeed);

        if (hasTrace && Math.abs(clampedYaw) < 0.4F) {
            clampedYaw = 0.0F;
        }
        if (hasTrace && Math.abs(clampedPitch) < 0.2F) {
            clampedPitch = 0.0F;
        }

        this.lastYaw = this.lastYaw + clampedYaw;
        this.lastPitch = this.lastPitch + clampedPitch;

        Rotation rot = new Rotation(this.lastYaw, this.lastPitch);
        RotationComponent.update(rot, 360.0F, 360.0F, 360.0F, 360.0F, 0, 1, elytraVisual);
    }

    @Override
    public void update(Rotation targetAngle, boolean elytraVisual) {
    }
}
