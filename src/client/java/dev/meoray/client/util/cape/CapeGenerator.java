package dev.meoray.client.util.cape;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;

public class CapeGenerator {

    public static final Identifier CAPE_ID = Identifier.of("meoray", "textures/meoray_cape.png");
    private static boolean registered = false;

    private static final int BG_BLACK       = argb(255,  10,  10,  12);
    private static final int PAW_WHITE      = argb(255, 235, 235, 240);
    private static final int PAW_SHADOW     = argb(255, 195, 195, 200);
    private static final int MINI_PAW       = argb(70,  255, 255, 255);
    private static final int TRANSPARENT    = argb(0,   0,   0,   0);

    private static final int[][] BIG_PAW = {
            {0, 0, 1, 0, 0, 0, 0, 1, 0, 0},
            {0, 1, 1, 1, 0, 0, 1, 1, 1, 0},
            {0, 1, 1, 1, 0, 0, 1, 1, 1, 0},
            {0, 1, 1, 1, 0, 0, 1, 1, 1, 0},
            {1, 1, 2, 1, 0, 0, 1, 2, 1, 1},
            {1, 1, 1, 0, 0, 0, 0, 1, 1, 1},
            {1, 2, 0, 0, 0, 0, 0, 0, 2, 1},
            {0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
            {0, 0, 0, 1, 1, 1, 1, 0, 0, 0},
            {0, 0, 1, 1, 1, 1, 1, 1, 0, 0},
            {0, 1, 1, 2, 1, 1, 2, 1, 1, 0},
            {0, 1, 1, 1, 1, 1, 1, 1, 1, 0},
            {0, 1, 1, 1, 1, 1, 1, 1, 1, 0},
            {0, 0, 1, 1, 2, 2, 1, 1, 0, 0},
            {0, 0, 0, 1, 1, 1, 1, 0, 0, 0},
            {0, 0, 0, 0, 1, 1, 0, 0, 0, 0},
    };

    private static final int[][] MINI = {
            {1, 0, 1},
            {0, 0, 0},
            {0, 1, 0},
    };

    public static void registerCapeTexture() {
        if (registered) return;

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.getTextureManager() == null) return;

        try {
            NativeImage image = generateCapeImage();
            NativeImageBackedTexture texture = new NativeImageBackedTexture(image);
            mc.getTextureManager().registerTexture(CAPE_ID, texture);
            registered = true;
            System.out.println("[MeoRay] Custom cape texture registered: " + CAPE_ID);
        } catch (Exception e) {
            System.err.println("[MeoRay] Failed to register cape texture:");
            e.printStackTrace();
        }
    }

    private static NativeImage generateCapeImage() {
        NativeImage img = new NativeImage(64, 32, true);

        for (int x = 0; x < 64; x++) {
            for (int y = 0; y < 32; y++) {
                img.setColorArgb(x, y, TRANSPARENT);
            }
        }

        fillRect(img, 1, 0, 10, 1, BG_BLACK);
        fillRect(img, 11, 0, 10, 1, BG_BLACK);
        fillRect(img, 1, 1, 10, 16, BG_BLACK);
        fillRect(img, 11, 1, 10, 16, BG_BLACK);
        fillRect(img, 0, 1, 1, 16, BG_BLACK);
        fillRect(img, 21, 1, 1, 16, BG_BLACK);

        drawBigPaw(img, 1, 1);
        drawBigPaw(img, 11, 1);

        scatterMiniPaws(img, 1, 1, 10, 16);
        scatterMiniPaws(img, 11, 1, 10, 16);

        return img;
    }

    private static void drawBigPaw(NativeImage img, int offX, int offY) {
        for (int y = 0; y < BIG_PAW.length; y++) {
            for (int x = 0; x < BIG_PAW[y].length; x++) {
                int v = BIG_PAW[y][x];
                if (v == 0) continue;

                int color = (v == 2) ? PAW_SHADOW : PAW_WHITE;
                setSafe(img, offX + x, offY + y, color);
            }
        }
    }

    private static void scatterMiniPaws(NativeImage img, int zoneX, int zoneY,
                                         int zoneW, int zoneH) {
        int[][] miniPositions = {
                {0, 0}, {7, 0}, {0, 13}, {7, 13},
                {0, 6}, {7, 6},
        };

        for (int[] pos : miniPositions) {
            drawMiniPaw(img, zoneX + pos[0], zoneY + pos[1]);
        }
    }

    private static void drawMiniPaw(NativeImage img, int offX, int offY) {
        for (int y = 0; y < MINI.length; y++) {
            for (int x = 0; x < MINI[y].length; x++) {
                if (MINI[y][x] == 1) {
                    setSafeBlend(img, offX + x, offY + y, MINI_PAW);
                }
            }
        }
    }

    private static void fillRect(NativeImage img, int sx, int sy, int w, int h, int color) {
        for (int x = sx; x < sx + w; x++) {
            for (int y = sy; y < sy + h; y++) {
                setSafe(img, x, y, color);
            }
        }
    }

    private static void setSafe(NativeImage img, int x, int y, int color) {
        if (x < 0 || y < 0 || x >= img.getWidth() || y >= img.getHeight()) return;
        img.setColorArgb(x, y, color);
    }

    private static void setSafeBlend(NativeImage img, int x, int y, int color) {
        if (x < 0 || y < 0 || x >= img.getWidth() || y >= img.getHeight()) return;

        int existing = img.getColorArgb(x, y);
        int existingR = (existing >> 16) & 0xFF;
        int existingG = (existing >>  8) & 0xFF;
        int existingB =  existing        & 0xFF;

        if (existingR > 100 || existingG > 100 || existingB > 100) return;

        img.setColorArgb(x, y, color);
    }

    private static int argb(int a, int r, int g, int b) {
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    public static Identifier getCapeIdentifier() {
        return CAPE_ID;
    }

    public static boolean isRegistered() {
        return registered;
    }
}
