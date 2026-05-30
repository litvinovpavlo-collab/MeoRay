package dev.meoray.client.render;

public class AnimationUtil {
    public static float smoothStep(float t) {
        t = Math.max(0, Math.min(1, t));
        return t * t * (3 - 2 * t);
    }

    public static float animate(float current, float target, float speed) {
        float diff = target - current;
        if (Math.abs(diff) < 0.01f) return target;
        return current + diff * speed;
    }
}
