package dev.meoray.client.mixin;

import dev.meoray.client.account.accessor.IMinecraftClientMixin;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.session.Session;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;

@Environment(EnvType.CLIENT)
@Mixin(MinecraftClient.class)
public abstract class MinecraftClientMixin implements IMinecraftClientMixin {

    @Mutable @Final @Shadow private Session session;

    @Override
    public void setSession(Session session) {
        this.session = session;
    }
}
