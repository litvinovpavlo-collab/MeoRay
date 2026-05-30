package dev.meoray.client.util.animations;

import java.util.function.Function;

public enum Easing {

    LINEAR(t -> t),

    EASE_OUT_SINE(t -> (float) Math.sin((t * Math.PI) / 2.0)),

    EASE_OUT_CUBIC(t -> 1 - (float) Math.pow(1 - t, 3)),

    EASE_OUT_QUART(t -> 1 - (float) Math.pow(1 - t, 4)),

    EASE_OUT_QUINT(t -> 1 - (float) Math.pow(1 - t, 5)),

    EASE_OUT_EXPO(t -> t >= 1 ? 1 : 1 - (float) Math.pow(2, -10 * t)),

    EASE_IN_OUT_SINE(t -> (float) (-(Math.cos(Math.PI * t) - 1) / 2)),

    EASE_IN_OUT_CUBIC(t -> t < 0.5
        ? 4 * t * t * t
        : 1 - (float) Math.pow(-2 * t + 2, 3) / 2),

    EASE_IN_OUT_QUINT(t -> t < 0.5
        ? 16 * t * t * t * t * t
        : 1 - (float) Math.pow(-2 * t + 2, 5) / 2),

    EASE_IN_OUT_QUART(t -> t < 0.5
        ? 8 * t * t * t * t
        : 1 - (float) Math.pow(-2 * t + 2, 4) / 2);

    private final Function<Float, Float> function;

    Easing(Function<Float, Float> function) {
        this.function = function;
    }

    public float apply(float t) {
        return function.apply(clamp(t));
    }

    private static float clamp(float t) {
        return Math.max(0, Math.min(1, t));
    }
}
