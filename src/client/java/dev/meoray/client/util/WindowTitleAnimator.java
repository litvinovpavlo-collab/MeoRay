package dev.meoray.client.util;

import net.minecraft.client.MinecraftClient;
import org.lwjgl.glfw.GLFW;

public class WindowTitleAnimator {
    private static final String TEXT = "MeoRay v1.0.0 --> https://MeoRayClient.ru";
    private static final long CHAR_DELAY = 120;
    private static final long HOLD = 2000;
    private static final long ERASE_DELAY = 80;
    private static final long PAUSE = 600;

    private enum Phase { TYPING, HOLDING, ERASING, PAUSING }
    private static Phase phase = Phase.TYPING;
    private static int index = 0;
    private static long lastUpdate = 0;

    public static void tick() {
        long now = System.currentTimeMillis();
        long handle = MinecraftClient.getInstance().getWindow().getHandle();
        switch (phase) {
            case TYPING -> {
                if (now - lastUpdate >= CHAR_DELAY) {
                    lastUpdate = now;
                    index++;
                    if (index >= TEXT.length()) {
                        index = TEXT.length();
                        phase = Phase.HOLDING;
                    }
                    GLFW.glfwSetWindowTitle(handle, TEXT.substring(0, index) + "_");
                }
            }
            case HOLDING -> {
                if (now - lastUpdate >= HOLD) {
                    lastUpdate = now;
                    phase = Phase.ERASING;
                }
            }
            case ERASING -> {
                if (now - lastUpdate >= ERASE_DELAY) {
                    lastUpdate = now;
                    index--;
                    if (index <= 0) {
                        index = 0;
                        phase = Phase.PAUSING;
                    }
                    GLFW.glfwSetWindowTitle(handle, TEXT.substring(0, index) + "_");
                }
            }
            case PAUSING -> {
                if (now - lastUpdate >= PAUSE) {
                    lastUpdate = now;
                    phase = Phase.TYPING;
                }
            }
        }
    }
}