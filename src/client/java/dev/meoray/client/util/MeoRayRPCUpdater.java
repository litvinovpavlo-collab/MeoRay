package dev.meoray.client.util;

import net.minecraft.client.MinecraftClient;

import java.util.Random;

public class MeoRayRPCUpdater {
    private static long lastUpdate = 0;
    private static final long UPDATE_INTERVAL = 5000; // каждые 5 секунд

    private static final Random random = new Random();

    // Фразы для главного меню
    private static final String[] MENU_PHRASES = {
            "💎 Premium Edition",
            "🚀 Best Client 2026",
            "✨ Undetected",
            "🔥 Made by lovely",
            "⚡ Lightning Fast",
            "🌟 Elite Member",
            "💜 Powered by MeoRay",
            "🎯 No Cheater Found",
            "🛡️ Bypass Active",
            "👑 King of Clients"
    };

    // Фразы для сервера
    private static final String[] SERVER_PHRASES = {
            "⚔ PvP Mode Active",
            "🔥 Tearing servers apart",
            "💀 Destroying enemies",
            "🎯 Locked on target",
            "⚡ Lightning reflexes",
            "🛡️ Untouchable",
            "👑 Top Player",
            "💎 Premium User",
            "🚀 Bypass Working",
            "✨ Smooth gameplay"
    };

    // Фразы для одиночки
    private static final String[] SINGLEPLAYER_PHRASES = {
            "🌍 Exploring worlds",
            "🛠️ Testing new modules",
            "💎 Building empire",
            "⛏️ Mining diamonds",
            "🏰 Creating masterpiece",
            "🌲 Wandering in forest",
            "✨ Crafting magic",
            "🎮 Having fun"
    };

    public static void tick() {
        long now = System.currentTimeMillis();
        if (now - lastUpdate < UPDATE_INTERVAL) return;
        lastUpdate = now;

        if (!MeoRayRPC.isConnected()) return;

        MinecraftClient mc = MinecraftClient.getInstance();
        String details;
        String state;

        if (mc.player == null || mc.world == null) {
            details = "🌟 In Main Menu";
            state = randomFrom(MENU_PHRASES);
        } else if (mc.getCurrentServerEntry() != null) {
            String server = mc.getCurrentServerEntry().address;
            if (server.endsWith(":25565")) {
                server = server.substring(0, server.length() - 6);
            }
            details = "⚔ Playing on " + server;
            state = randomFrom(SERVER_PHRASES);
        } else if (mc.isIntegratedServerRunning()) {
            details = "🌍 Singleplayer";
            state = randomFrom(SINGLEPLAYER_PHRASES);
        } else {
            details = "Playing Minecraft";
            state = randomFrom(MENU_PHRASES);
        }

        if (details.length() > 128) details = details.substring(0, 125) + "...";
        if (state.length() > 128) state = state.substring(0, 125) + "...";

        MeoRayRPC.update(details, state);
    }

    private static String randomFrom(String[] array) {
        return array[random.nextInt(array.length)];
    }
}