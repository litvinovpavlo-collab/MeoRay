package dev.meoray.client.rotation;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.Random;

public class NeuroRotation {

    private final MinecraftClient mc = MinecraftClient.getInstance();
    private final Random random = new Random();

    private String rotationMode = "Neuro";
    private float smoothness = 78;
    private float humanization = 1.65f;
    private float prediction = 1.45f;
    private float gcd = 0.5f;

    private Rotation current = new Rotation(0, 0);
    private Rotation prev = new Rotation(0, 0);
    private boolean hasPrev = false;

    public NeuroRotation() {
    }

    public void setRotationMode(String mode) {
        this.rotationMode = mode;
    }

    public void setSmoothness(float value) {
        this.smoothness = value;
    }

    public void setHumanization(float value) {
        this.humanization = value;
    }

    public void setPrediction(float value) {
        this.prediction = value;
    }

    public void setGCD(float value) {
        this.gcd = value;
    }

    public void reset() {
        hasPrev = false;
        current = new Rotation(0, 0);
        prev = new Rotation(0, 0);
    }

    public Rotation update(Entity target, String aimPoint) {
        if (mc.player == null) return current;

        Vec3d targetPos = getTargetVec(target, aimPoint);

        if (prediction > 0 && target instanceof LivingEntity living) {
            Vec3d vel = living.getVelocity();
            targetPos = targetPos.add(vel.x * prediction * 0.5, vel.y * prediction * 0.3, vel.z * prediction * 0.5);
        }

        double dx = targetPos.x - mc.player.getX();
        double dy = targetPos.y - mc.player.getY() - mc.player.getEyeHeight(mc.player.getPose());
        double dz = targetPos.z - mc.player.getZ();

        double dist = Math.sqrt(dx * dx + dz * dz);

        float targetYaw = (float) Math.toDegrees(Math.atan2(dz, dx)) - 90;
        float targetPitch = (float) -Math.toDegrees(Math.atan2(dy, dist));

        targetYaw = Rotation.wrapAngleTo180(targetYaw);

        if (humanization > 0) {
            targetYaw += (float) (random.nextGaussian() * humanization * 0.15);
            targetPitch += (float) (random.nextGaussian() * humanization * 0.1);
        }

        targetYaw = Rotation.wrapAngleTo180(targetYaw);

        switch (rotationMode) {
            case "Matrix":
                current = applySmooth(current, targetYaw, targetPitch, smoothness * 0.6f);
                break;
            case "Vulcan":
                current = applySmooth(current, targetYaw, targetPitch, smoothness * 0.4f);
                break;
            default:
                current = applySmooth(current, targetYaw, targetPitch, smoothness);
                break;
        }

        current = applyGCD(current);

        prev = current;
        hasPrev = true;

        return current;
    }

    private Rotation applySmooth(Rotation from, float targetYaw, float targetPitch, float smooth) {
        float yawDiff = Rotation.wrapAngleTo180(targetYaw - from.yaw);
        float pitchDiff = targetPitch - from.pitch;

        float factor = Math.min(1, smooth / 200f);
        if (smooth >= 100) factor = Math.min(1, (smooth - 40) / 120f);

        float newYaw = from.yaw + yawDiff * factor;
        float newPitch = from.pitch + pitchDiff * factor;

        return new Rotation(newYaw, newPitch);
    }

    private Rotation applyGCD(Rotation rot) {
        if (gcd <= 0) return rot;

        float yawDelta = hasPrev ? rot.yaw - prev.yaw : 0;
        float pitchDelta = hasPrev ? rot.pitch - prev.pitch : 0;

        float yawGCD = applyGCDValue(yawDelta);
        float pitchGCD = applyGCDValue(pitchDelta);

        return new Rotation(prev.yaw + yawGCD, prev.pitch + pitchGCD);
    }

    private float applyGCDValue(float delta) {
        float sensitivity = 0.5f + gcd * 0.5f;
        float adjusted = delta * sensitivity;
        float rounded = (float) Math.round(adjusted * 1000f) / 1000f;
        return rounded / sensitivity;
    }

    private Vec3d getTargetVec(Entity target, String aimPoint) {
        if (target == null) return Vec3d.ZERO;

        double height = switch (aimPoint) {
            case "Head" -> target.getHeight() * 0.9;
            case "Body" -> target.getHeight() * 0.5;
            case "Legs" -> target.getHeight() * 0.1;
            default -> {
                double h = target.getHeight();
                yield 0.1 + MathHelper.clamp(random.nextDouble() * 0.8, 0, h - 0.1);
            }
        };

        return target.getPos().add(0, height, 0);
    }
}
