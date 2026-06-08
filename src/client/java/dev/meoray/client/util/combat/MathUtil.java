package dev.meoray.client.util.combat;

import java.util.concurrent.ThreadLocalRandom;

public final class MathUtil {
    private MathUtil() {}

    public static float random(float min, float max) {
        return ThreadLocalRandom.current().nextFloat() * (max - min) + min;
    }

    public static double random(double min, double max) {
        return ThreadLocalRandom.current().nextDouble() * (max - min) + min;
    }
}
