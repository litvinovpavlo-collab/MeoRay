package dev.meoray.client.render;

import net.minecraft.client.MinecraftClient;

public class ScissorUtil {
    private ScissorUtil() {}

    public static void beginScissor(int x, int y, int width, int height) {
        MinecraftClient mc = MinecraftClient.getInstance();
        int scaleFactor = (int) mc.getWindow().getScaleFactor();
        int windowHeight = mc.getWindow().getScaledHeight();

        org.lwjgl.opengl.GL11.glEnable(org.lwjgl.opengl.GL11.GL_SCISSOR_TEST);
        org.lwjgl.opengl.GL11.glScissor(
                x * scaleFactor,
                (windowHeight - y - height) * scaleFactor,
                width * scaleFactor,
                height * scaleFactor
        );
    }

    public static void endScissor() {
        org.lwjgl.opengl.GL11.glDisable(org.lwjgl.opengl.GL11.GL_SCISSOR_TEST);
    }
}
