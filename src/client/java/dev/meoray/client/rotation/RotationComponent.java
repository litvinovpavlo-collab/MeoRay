package dev.meoray.client.rotation;

import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec2f;

public final class RotationComponent {
    private static final MinecraftClient mc = MinecraftClient.getInstance();
    private static boolean aiming;
    private static float silentYaw;
    private static float silentPitch;

    public static void update(Rotation target, float yawSpeed, float pitchSpeed,
                              float yawReturnSpeed, float pitchReturnSpeed,
                              int timeout, int priority, boolean elytraVisual) {
        if (mc.player == null) return;

        aiming = true;

        Rotation current = new Rotation(mc.player.getYaw(), mc.player.getPitch());
        float yawDelta = MathHelper.wrapDegrees(target.getYaw() - current.getYaw());
        float pitchDelta = target.getPitch() - current.getPitch();
        float totalDelta = Math.abs(yawDelta) + Math.abs(pitchDelta);

        float clampYaw = totalDelta == 0.0F ? 0.0F : Math.abs(yawDelta / totalDelta) * yawSpeed;
        float clampPitch = totalDelta == 0.0F ? 0.0F : Math.abs(pitchDelta / totalDelta) * pitchSpeed;

        Vec2f raw = new Vec2f(
            mc.player.getYaw() + MathHelper.clamp(yawDelta, -clampYaw, clampYaw),
            MathHelper.clamp(mc.player.getPitch() + MathHelper.clamp(pitchDelta, -clampPitch, clampPitch), -90.0F, 90.0F)
        );
        Vec2f corrected = applySensitivityPatch(raw, new Vec2f(mc.player.getYaw(), mc.player.getPitch()));

        silentYaw = corrected.x;
        silentPitch = corrected.y;
    }

    public static void update(Rotation target, float turnSpeed, float returnSpeed, int timeout, int priority) {
        update(target, turnSpeed, turnSpeed, returnSpeed, returnSpeed, timeout, priority, false);
    }

    public static Vec2f applySensitivityPatch(Vec2f rotation, Vec2f previousRotation) {
        double sens = mc.options.getMouseSensitivity().getValue();
        double gcd = Math.pow(sens * 0.6 + 0.2, 3.0) * 8.0;
        double prevYaw = previousRotation.x;
        double prevPitch = previousRotation.y;
        double yaw = Math.round((rotation.x - prevYaw) / (gcd * 0.15)) * gcd * 0.15;
        double pitch = Math.round((rotation.y - prevPitch) / (gcd * 0.15)) * gcd * 0.15;
        return new Vec2f((float)(prevYaw + yaw), (float)(prevPitch + pitch));
    }

    public static boolean isAiming() { return aiming; }
    public static void setAiming(boolean a) { aiming = a; }
    public static float getSilentYaw() { return silentYaw; }
    public static float getSilentPitch() { return silentPitch; }

    private RotationComponent() {}
}
