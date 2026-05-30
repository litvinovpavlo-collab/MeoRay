package dev.meoray.client.account;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class AccountManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_DIR = FabricLoader.getInstance().getConfigDir().resolve("meoray");
    private static final Path ACCOUNTS_FILE = CONFIG_DIR.resolve("accounts.json");

    private static List<MeoRayAccount> accounts = new ArrayList<>();
    private static MeoRayAccount activeAccount = null;

    public static void load() {
        try {
            if (!Files.exists(CONFIG_DIR)) {
                Files.createDirectories(CONFIG_DIR);
            }
            if (!Files.exists(ACCOUNTS_FILE)) {
                save();
                return;
            }
            String json = Files.readString(ACCOUNTS_FILE);
            Type listType = new TypeToken<List<MeoRayAccount>>(){}.getType();
            List<MeoRayAccount> loaded = GSON.fromJson(json, listType);
            if (loaded != null) {
                accounts = loaded;
            }
        } catch (IOException e) {
            System.err.println("[Nova] Failed to load accounts: " + e.getMessage());
        }
    }

    public static void save() {
        try {
            if (!Files.exists(CONFIG_DIR)) {
                Files.createDirectories(CONFIG_DIR);
            }
            String json = GSON.toJson(accounts);
            Files.writeString(ACCOUNTS_FILE, json);
        } catch (IOException e) {
            System.err.println("[Nova] Failed to save accounts: " + e.getMessage());
        }
    }

    public static List<MeoRayAccount> getAccounts() {
        return accounts;
    }

    public static void addAccount(String username) {
        for (MeoRayAccount acc : accounts) {
            if (acc.getUsername().equalsIgnoreCase(username)) {
                return;
            }
        }
        accounts.add(new MeoRayAccount(username));
        save();
    }

    public static void removeAccount(MeoRayAccount account) {
        accounts.remove(account);
        if (activeAccount == account) {
            activeAccount = null;
        }
        save();
    }

    public static MeoRayAccount getActiveAccount() {
        return activeAccount;
    }

    public static void setActiveAccount(MeoRayAccount account) {
        activeAccount = account;
        SessionSpoofer.apply(account);
    }
}
