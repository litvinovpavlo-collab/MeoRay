package dev.meoray.client.rotation;

public class Rotation {
    public final float yaw;
    public final float pitch;

    public Rotation(float yaw, float pitch) {
        this.yaw = yaw;
        this.pitch = Math.max(-90, Math.min(90, pitch));
    }

    public Rotation normalize() {
        return new Rotation(wrapDegrees(yaw), pitch);
    }

    public Rotation copy() {
        return new Rotation(yaw, pitch);
    }

    public static float wrapDegrees(float value) {
        value %= 360;
        if (value >= 180) value -= 360;
        if (value < -180) value += 360;
        return value;
    }

    public static float wrapAngleTo180(float value) {
        value %= 360;
        if (value >= 180) value -= 360;
        if (value < -180) value += 360;
        return value;
    }
}
