package dev.meoray.client.account;

import java.util.UUID;

public class MeoRayAccount {
    private String username;
    private String uuid;
    private long addedAt;

    public MeoRayAccount() {}

    public MeoRayAccount(String username) {
        this.username = username;
        this.uuid = generateOfflineUUID(username).toString();
        this.addedAt = System.currentTimeMillis();
    }

    public String getUsername() { return username; }
    public String getUuid() { return uuid; }
    public long getAddedAt() { return addedAt; }

    public UUID getUUIDObject() {
        return UUID.fromString(uuid);
    }

    public static UUID generateOfflineUUID(String username) {
        return UUID.nameUUIDFromBytes(("OfflinePlayer:" + username).getBytes());
    }
}
