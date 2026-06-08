package dev.meoray.client.rotation.impl;

import dev.meoray.client.rotation.Rotation;
import dev.meoray.client.rotation.RotationBase;
import dev.meoray.client.rotation.RotationComponent;
import dev.meoray.client.util.combat.RaytracingUtil;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.MathHelper;

public class FunTimeRotation extends RotationBase {
    private float currentYaw = 0.0F;
    private float currentPitch = 0.0F;
    private boolean isInitialized = false;

    private LivingEntity lastTarget = null;
    private float lastDeltaYaw = 0.0F;
    private float lastDeltaPitch = 0.0F;

    private float humanOffsetX = 0.0F;
    private float humanOffsetY = 0.0F;
    private long lastNoiseTime = 0L;

    public void update(LivingEntity target, Rotation targetAngle, boolean elytraVisual) {
        long now = System.currentTimeMillis();

        if (!isInitialized) {
            this.currentYaw = mc.player.getYaw();
            this.currentPitch = mc.player.getPitch();
            this.isInitialized = true;
        }

        if (target != lastTarget) {
            this.lastTarget = target;
            this.humanOffsetX = (rng.nextFloat() - 0.5F) * 1.4F;
            this.humanOffsetY = (rng.nextFloat() - 0.5F) * 0.8F;
        }

        if (now - lastNoiseTime > 150 + rng.nextInt(150)) {
            this.humanOffsetX = (float) (rng.nextGaussian() * 0.18F);
            this.humanOffsetY = (float) (rng.nextGaussian() * 0.12F);
            lastNoiseTime = now;
        }

        float targetYaw = MathHelper.wrapDegrees(targetAngle.getYaw() + humanOffsetX);
        float targetPitch = MathHelper.clamp(targetAngle.getPitch() + humanOffsetY, -90.0F, 90.0F);

        float deltaYaw = MathHelper.wrapDegrees(targetYaw - this.currentYaw);
        float deltaPitch = targetPitch - this.currentPitch;

        float distance = (float) Math.sqrt(deltaYaw * deltaYaw + deltaPitch * deltaPitch);

        boolean hasTrace = RaytracingUtil.rayTrace(mc.player.getRotationVector(), 3.0, target.getBoundingBox());

        float speedModifier = hasTrace ? 0.08F + (rng.nextFloat() * 0.04F) : 0.15F + (rng.nextFloat() * 0.08F);

        float stepYaw = deltaYaw * speedModifier;
        float stepPitch = deltaPitch * speedModifier;

        stepYaw = (stepYaw * 0.7F) + (this.lastDeltaYaw * 0.3F);
        stepPitch = (stepPitch * 0.7F) + (this.lastDeltaPitch * 0.3F);

        this.lastDeltaYaw = stepYaw;
        this.lastDeltaPitch = stepPitch;

        float gcd = Rotation.gcd();

        int mX = Math.round(stepYaw / gcd);
        int mY = Math.round(stepPitch / gcd);

        if (mX == 0 && mY == 0) {
            return;
        }

        float finalYaw = this.currentYaw + (mX * gcd);
        float finalPitch = this.currentPitch + (mY * gcd);
        finalPitch = MathHelper.clamp(finalPitch, -90.0F, 90.0F);

        correctMovement(finalYaw);

        Rotation finalRot = new Rotation(finalYaw, finalPitch);

        float rotationSpeed = 45.0F + (distance * 1.5F) + (rng.nextFloat() * 15.0F);
        rotationSpeed = MathHelper.clamp(rotationSpeed, 40.0F, 85.0F);

        RotationComponent.update(finalRot, rotationSpeed, rotationSpeed, rotationSpeed, rotationSpeed, 0, 1, elytraVisual);

        this.currentYaw = finalYaw;
        this.currentPitch = finalPitch;
        this.lastYaw = finalYaw;
        this.lastPitch = finalPitch;
    }

    private void correctMovement(float rotationYaw) {
        if (mc.player == null) return;
        float forward = mc.player.input.movementForward;
        float strafe = mc.player.input.movementSideways;
        if (forward == 0.0F && strafe == 0.0F) return;

        float clientYaw = mc.player.getYaw();
        double angleRad = Math.toRadians(MathHelper.wrapDegrees(rotationYaw - clientYaw));

        float newForward = (float) (forward * Math.cos(angleRad) + strafe * Math.sin(angleRad));
        float newStrafe = (float) (strafe * Math.cos(angleRad) - forward * Math.sin(angleRad));

        mc.player.input.movementForward = newForward;
        mc.player.input.movementSideways = newStrafe;
    }

    @Override
    public void update(Rotation targetAngle, boolean elytraVisual) {
        if (mc.player != null) {
            this.currentYaw = mc.player.getYaw();
            this.currentPitch = mc.player.getPitch();
        }
        this.isInitialized = false;
        this.lastDeltaYaw = 0.0F;
        this.lastDeltaPitch = 0.0F;
    }
}
