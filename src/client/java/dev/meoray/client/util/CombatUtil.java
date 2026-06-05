package dev.meoray.client.util;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.AxeItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.SwordItem;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

public class CombatUtil {

    private static final MinecraftClient mc = MinecraftClient.getInstance();

    public static boolean isHoldingSword() {
        if (mc.player == null) return false;
        ItemStack stack = mc.player.getMainHandStack();
        return stack.getItem() instanceof SwordItem;
    }

    public static boolean canBreakShield() {
        if (mc.player == null) return false;
        ItemStack stack = mc.player.getMainHandStack();
        return stack.getItem() instanceof AxeItem;
    }

    public static boolean targetHoldingShield(PlayerEntity target) {
        if (target == null) return false;
        return target.getMainHandStack().getItem() == Items.SHIELD
            || target.getOffHandStack().getItem() == Items.SHIELD
            || target.isBlocking();
    }

    public static boolean isUsingItem() {
        if (mc.player == null) return false;
        return mc.player.isUsingItem();
    }

    public static void sprintResetPacket() {
        if (mc.player == null || mc.player.networkHandler == null) return;

        mc.player.networkHandler.sendPacket(
                new net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket(
                        mc.player,
                        net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket.Mode.STOP_SPRINTING
                )
        );
    }

    public static void sprintResetVanilla() {
        if (mc.player == null) return;
        mc.player.setSprinting(false);
    }

    public static void desyncShield() {
        if (mc.player == null || mc.player.networkHandler == null) return;

        mc.player.networkHandler.sendPacket(
                new PlayerActionC2SPacket(
                        PlayerActionC2SPacket.Action.RELEASE_USE_ITEM,
                        BlockPos.ORIGIN,
                        Direction.DOWN
                )
        );
    }
}
