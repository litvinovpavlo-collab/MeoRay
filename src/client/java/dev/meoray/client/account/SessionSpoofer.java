package dev.meoray.client.account;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.UserApiService;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.session.Session;

import java.lang.reflect.Field;
import java.util.UUID;

public class SessionSpoofer {

    public static void apply(MeoRayAccount account) {
        if (account == null) return;

        try {
            MinecraftClient mc = MinecraftClient.getInstance();
            UUID uuid = account.getUUIDObject();

            Session newSession = new Session(
                    account.getUsername(),
                    uuid,
                    "0",
                    java.util.Optional.empty(),
                    java.util.Optional.empty(),
                    Session.AccountType.LEGACY
            );

            Field sessionField = findSessionField(mc);
            sessionField.setAccessible(true);
            sessionField.set(mc, newSession);

            System.out.println("[Nova] Session spoofed to: " + account.getUsername());
        } catch (Exception e) {
            System.err.println("[Nova] Failed to spoof session: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static Field findSessionField(MinecraftClient mc) throws NoSuchFieldException {
        for (Field f : MinecraftClient.class.getDeclaredFields()) {
            if (f.getType() == Session.class) {
                return f;
            }
        }
        throw new NoSuchFieldException("Session field not found in MinecraftClient");
    }
}
