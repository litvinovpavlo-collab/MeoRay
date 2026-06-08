package dev.meoray.client.util;

import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.MathHelper;

public final class MovingUtil {
    private static final MinecraftClient mc = MinecraftClient.getInstance();

    public static void fixMovementFocus(float yaw) {
        if (mc.player == null) return;
        float forward = mc.player.input.movementForward;
        float strafe = mc.player.input.movementSideways;
        if (forward == 0.0F && strafe == 0.0F) return;

        double targetAngle = MathHelper.wrapDegrees(Math.toDegrees(direction(yaw, forward, strafe)));
        float bestForward = 0.0F;
        float bestStrafe = 0.0F;
        float smallestDifference = Float.MAX_VALUE;

        for (float testForward = -1.0F; testForward <= 1.0F; testForward++) {
            for (float testStrafe = -1.0F; testStrafe <= 1.0F; testStrafe++) {
                if (testForward == 0.0F && testStrafe == 0.0F) continue;
                double testAngle = MathHelper.wrapDegrees(Math.toDegrees(direction(yaw, testForward, testStrafe)));
                float difference = Math.abs(MathHelper.wrapDegrees((float) (targetAngle - testAngle)));
                if (difference < smallestDifference) {
                    smallestDifference = difference;
                    bestForward = testForward;
                    bestStrafe = testStrafe;
                }
            }
        }

        mc.player.input.movementForward = bestForward;
        mc.player.input.movementSideways = bestStrafe;
    }

    public static void fixMovementFree() {
        if (mc.player == null) return;
        float forward = mc.player.input.movementForward;
        float strafe = mc.player.input.movementSideways;
        if (forward == 0.0F && strafe == 0.0F) return;

        double angle = MathHelper.wrapDegrees(Math.toDegrees(direction(mc.player.getYaw(), forward, strafe)));
        float bestForward = 0.0F;
        float bestStrafe = 0.0F;
        float smallestDifference = Float.MAX_VALUE;

        for (float testForward = -1.0F; testForward <= 1.0F; testForward++) {
            for (float testStrafe = -1.0F; testStrafe <= 1.0F; testStrafe++) {
                if (testForward == 0.0F && testStrafe == 0.0F) continue;
                double testAngle = MathHelper.wrapDegrees(Math.toDegrees(direction(mc.player.getYaw(), testForward, testStrafe)));
                float difference = (float) Math.abs(angle - testAngle);
                if (difference < smallestDifference) {
                    smallestDifference = difference;
                    bestForward = testForward;
                    bestStrafe = testStrafe;
                }
            }
        }

        mc.player.input.movementForward = bestForward;
        mc.player.input.movementSideways = bestStrafe;
    }

    public static double direction(float rotationYaw, float moveForward, float moveStrafing) {
        if (moveForward < 0.0F) rotationYaw += 180.0F;
        float forward = 1.0F;
        if (moveForward < 0.0F) forward = -0.5F;
        if (moveForward > 0.0F) forward = 0.5F;
        if (moveStrafing > 0.0F) rotationYaw -= 90.0F * forward;
        if (moveStrafing < 0.0F) rotationYaw += 90.0F * forward;
        return Math.toRadians(rotationYaw);
    }

    private MovingUtil() {}
}
