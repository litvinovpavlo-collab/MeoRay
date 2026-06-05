package dev.meoray.client.util;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;

public class CriticalUtil {

    private static final MinecraftClient mc = MinecraftClient.getInstance();

    /**
     * Проверяет — можем ли мы сейчас крит
     * Условия для 1.21.4:
     * 1. Игрок в воздухе (или падает)
     * 2. Не в воде/лаве
     * 3. Не на лестнице
     * 4. Нет эффекта blindness
     * 5. Скорость падения вниз (velocity.y < 0)
     */
    public static boolean canCrit() {
        ClientPlayerEntity p = mc.player;
        if (p == null) return false;

        return !p.isOnGround()
                && !p.isTouchingWater()
                && !p.isInLava()
                && !p.isClimbing()
                && p.getVelocity().y < 0
                && !p.hasStatusEffect(net.minecraft.entity.effect.StatusEffects.BLINDNESS)
                && !p.isRiding();
    }

    /**
     * Пакетный крит — отправляем серверу пакеты движения
     * чтобы сервер думал что мы в воздухе
     * Работает на большинстве анархия-серверов
     */
    public static void doCritPacket() {
        ClientPlayerEntity p = mc.player;
        if (p == null || p.networkHandler == null) return;

        // Симулируем прыжок — 4 пакета
        // Это стандартный packet-крит для 1.12+
        p.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(
                p.getX(), p.getY() + 0.0625, p.getZ(), false, p.horizontalCollision
        ));
        p.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(
                p.getX(), p.getY(), p.getZ(), false, p.horizontalCollision
        ));
        p.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(
                p.getX(), p.getY() + 1.1E-5, p.getZ(), false, p.horizontalCollision
        ));
        p.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(
                p.getX(), p.getY(), p.getZ(), false, p.horizontalCollision
        ));
    }

    /**
     * Прыжковый крит — реальный прыжок
     * Максимально легитимный, но нельзя использовать часто
     */
    public static void doCritJump() {
        ClientPlayerEntity p = mc.player;
        if (p == null) return;

        if (p.isOnGround()) {
            p.jump();
        }
    }
}
