package dev.meoray.client.util;

import dev.meoray.client.rotation.Rotation;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;

public class RotationUtil {

    private static final MinecraftClient mc = MinecraftClient.getInstance();

    // Server-side rotations (silent) — сервер думает что ты смотришь туда
    // но камера игрока не двигается
    private static float serverYaw;
    private static float serverPitch;
    private static boolean rotating = false;

    /**
     * Устанавливает серверные повороты без движения камеры
     */
    public static void setRotation(Rotation rot) {
        serverYaw = rot.yaw;
        serverPitch = rot.pitch;
        rotating = true;
    }

    /**
     * Вызывать каждый тик в конце — сбрасывает silent rotation
     */
    public static void reset() {
        rotating = false;
    }

    public static boolean isRotating() {
        return rotating;
    }

    public static float getServerYaw() {
        return rotating ? serverYaw : mc.player.getYaw();
    }

    public static float getServerPitch() {
        return rotating ? serverPitch : mc.player.getPitch();
    }

    /**
     * Считает нужный yaw и pitch к точке в мире
     */
    public static Rotation getRotationToVec(Vec3d target) {
        if (mc.player == null) return new Rotation(0, 0);

        double dx = target.x - mc.player.getX();
        double dy = target.y - (mc.player.getY() + mc.player.getEyeHeight(mc.player.getPose()));
        double dz = target.z - mc.player.getZ();

        double dist = Math.sqrt(dx * dx + dz * dz);

        float yaw   = (float) Math.toDegrees(Math.atan2(dz, dx)) - 90f;
        float pitch = (float) -Math.toDegrees(Math.atan2(dy, dist));

        return new Rotation(
                Rotation.wrapAngleTo180(yaw),
                Math.max(-90, Math.min(90, pitch))
        );
    }

    /**
     * Считает угол между текущим взглядом игрока и целью (в градусах)
     */
    public static float getAngleDifference(Rotation a, Rotation b) {
        float yawDiff   = Math.abs(Rotation.wrapAngleTo180(a.yaw - b.yaw));
        float pitchDiff = Math.abs(a.pitch - b.pitch);
        return (float) Math.sqrt(yawDiff * yawDiff + pitchDiff * pitchDiff);
    }

    /**
     * Считает угол от игрока до энтити
     */
    public static float getAngleTo(Entity entity) {
        if (mc.player == null) return 180f;

        Vec3d targetVec = entity.getPos().add(0, entity.getHeight() * 0.5, 0);
        Rotation needed  = getRotationToVec(targetVec);
        Rotation current = new Rotation(mc.player.getYaw(), mc.player.getPitch());

        return getAngleDifference(needed, current);
    }
}
