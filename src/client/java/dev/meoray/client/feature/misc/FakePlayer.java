package dev.meoray.client.feature.misc;

import com.mojang.authlib.GameProfile;
import dev.meoray.client.core.Category;
import dev.meoray.client.core.Module;
import dev.meoray.client.core.setting.BooleanSetting;
import dev.meoray.client.core.setting.NumberSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.OtherClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class FakePlayer extends Module {

    public final NumberSetting health = add(new NumberSetting("Health", 20.0, 1.0, 100.0, 1.0));
    public final BooleanSetting copyInventory = add(new BooleanSetting("Copy Inventory", true));
    public final BooleanSetting nameAsYou = add(new BooleanSetting("Use Your Name", true));
    public final BooleanSetting persist = add(new BooleanSetting("Persist", true));

    private static FakePlayer INSTANCE;

    private final List<OtherClientPlayerEntity> spawned = new ArrayList<>();

    public static boolean isFake(net.minecraft.entity.Entity entity) {
        if (entity == null) return false;
        if (INSTANCE == null || !INSTANCE.isEnabled()) return false;
        return INSTANCE.spawned.contains(entity);
    }

    public FakePlayer() {
        super("FakePlayer", "Спавнит фейкового игрока для тестов", Category.MISC);
        INSTANCE = this;
    }

    @Override
    protected void onEnable() {
        spawn();
    }

    @Override
    protected void onDisable() {
        removeAll();
    }

    public void spawn() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.world == null) return;

        ClientWorld world = mc.world;

        String name = nameAsYou.getValue()
            ? mc.player.getGameProfile().getName()
            : "FakePlayer_" + (spawned.size() + 1);

        GameProfile profile = new GameProfile(UUID.randomUUID(), name);

        OtherClientPlayerEntity fake = new OtherClientPlayerEntity(world, profile);

        fake.refreshPositionAndAngles(
            mc.player.getX(),
            mc.player.getY(),
            mc.player.getZ(),
            mc.player.getYaw(),
            mc.player.getPitch()
        );
        fake.headYaw = mc.player.headYaw;
        fake.bodyYaw = mc.player.bodyYaw;
        fake.prevYaw = mc.player.getYaw();
        fake.prevPitch = mc.player.getPitch();

        try {
            var hpAttr = fake.getAttributeInstance(EntityAttributes.MAX_HEALTH);
            if (hpAttr != null) hpAttr.setBaseValue(health.getValue().floatValue());
        } catch (Throwable ignored) {}

        try {
            fake.setHealth(health.getValue().floatValue());
        } catch (Throwable ignored) {}

        if (copyInventory.getValue()) {
            try {
                fake.equipStack(EquipmentSlot.MAINHAND, mc.player.getMainHandStack().copy());
                fake.equipStack(EquipmentSlot.OFFHAND, mc.player.getOffHandStack().copy());
                fake.equipStack(EquipmentSlot.HEAD, mc.player.getEquippedStack(EquipmentSlot.HEAD).copy());
                fake.equipStack(EquipmentSlot.CHEST, mc.player.getEquippedStack(EquipmentSlot.CHEST).copy());
                fake.equipStack(EquipmentSlot.LEGS, mc.player.getEquippedStack(EquipmentSlot.LEGS).copy());
                fake.equipStack(EquipmentSlot.FEET, mc.player.getEquippedStack(EquipmentSlot.FEET).copy());
            } catch (Throwable ignored) {}
        }

        try {
            world.addEntity(fake);
            spawned.add(fake);
            mc.player.sendMessage(Text.literal("§a[MeoRay] §fFakePlayer заспавнен §7(" + name + ")"), false);
        } catch (Throwable e) {
            mc.player.sendMessage(Text.literal("§c[MeoRay] Не удалось заспавнить: " + e.getMessage()), false);
        }
    }

    public void removeLast() {
        if (spawned.isEmpty()) return;
        OtherClientPlayerEntity p = spawned.remove(spawned.size() - 1);
        p.setRemoved(net.minecraft.entity.Entity.RemovalReason.DISCARDED);
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player != null) {
            mc.player.sendMessage(Text.literal("§e[MeoRay] §fFakePlayer удалён"), false);
        }
    }

    public void removeAll() {
        for (OtherClientPlayerEntity p : spawned) {
            try {
                p.setRemoved(net.minecraft.entity.Entity.RemovalReason.DISCARDED);
            } catch (Throwable ignored) {}
        }
        spawned.clear();
    }

    @Override
    public void onTick() {
        if (!isEnabled()) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.world == null) return;

        spawned.removeIf(p -> p.isRemoved() || !p.isAlive());

        for (OtherClientPlayerEntity p : spawned) {
            try {
                if (p.getHealth() < health.getValue().floatValue() * 0.5f) {
                    p.setHealth(health.getValue().floatValue());
                }
                p.fallDistance = 0;
                p.setOnGround(true);
            } catch (Throwable ignored) {}
        }
    }
}
