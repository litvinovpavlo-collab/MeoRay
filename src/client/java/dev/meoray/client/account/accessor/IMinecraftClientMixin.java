package dev.meoray.client.account.accessor;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.session.Session;

@Environment(EnvType.CLIENT)
public interface IMinecraftClientMixin {
    void setSession(Session var1);
}
