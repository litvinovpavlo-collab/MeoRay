package dev.meoray.client.util;

import java.util.ArrayList;
import java.util.List;

public class TPSUtil {

    private static long lastUpdate = 0;
    private static final List<Float> tpsHistory = new ArrayList<>();
    private static final int MAX_HISTORY = 10;

    public static void update() {
        long now = System.currentTimeMillis();

        if (lastUpdate != 0) {
            long diff = now - lastUpdate;
            if (diff > 0) {
                float tps = 20f * (1000f / diff);
                tps = Math.min(20f, Math.max(1f, tps));

                tpsHistory.add(tps);
                if (tpsHistory.size() > MAX_HISTORY) {
                    tpsHistory.remove(0);
                }
            }
        }

        lastUpdate = now;
    }

    public static float getTPS() {
        if (tpsHistory.isEmpty()) return 20f;

        float sum = 0;
        for (float t : tpsHistory) sum += t;
        return sum / tpsHistory.size();
    }

    public static float getTPSMultiplier() {
        return getTPS() / 20f;
    }

    public static void reset() {
        tpsHistory.clear();
        lastUpdate = 0;
    }
}
