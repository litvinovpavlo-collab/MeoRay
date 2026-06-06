package dev.meoray.client.feature.misc;

import dev.meoray.client.MeoRayClient;
import dev.meoray.client.core.Category;
import dev.meoray.client.core.Module;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;

public class ClickFriend extends Module {

    public ClickFriend() {
        super("ClickFriend", "ЛКМ + Shift по игроку — добавить/убрать из друзей", Category.MISC);
    }

    public boolean tryToggleAtCrosshair() {
        if (!isEnabled()) return false;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.crosshairTarget == null) return false;
        if (mc.crosshairTarget.getType() != HitResult.Type.ENTITY) return false;

        Entity entity = ((EntityHitResult) mc.crosshairTarget).getEntity();
        if (!(entity instanceof PlayerEntity p)) return false;
        if (p == mc.player) return false;

        String name = p.getGameProfile().getName();
        boolean added = MeoRayClient.INSTANCE.getFriendManager().toggle(name);

        if (mc.player != null) {
            Text msg = added
                ? Text.literal("§a[MeoRay] §f" + name + " §7добавлен в друзья")
                : Text.literal("§c[MeoRay] §f" + name + " §7удалён из друзей");
            mc.player.sendMessage(msg, false);
        }
        return true;
    }
}
