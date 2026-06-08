package dev.meoray.client.feature.render;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.meoray.client.MeoRayClient;
import dev.meoray.client.core.Category;
import dev.meoray.client.core.Module;
import dev.meoray.client.core.setting.BooleanSetting;
import dev.meoray.client.core.setting.NumberSetting;
import dev.meoray.client.core.setting.SectionSetting;
import dev.meoray.client.managers.FontManager;
import dev.meoray.client.util.render.builders.Builder;
import dev.meoray.client.util.render.builders.states.QuadColorState;
import dev.meoray.client.util.render.builders.states.QuadRadiusState;
import dev.meoray.client.util.render.builders.states.SizeState;
import dev.meoray.client.util.render.msdf.MsdfFont;
import dev.meoray.client.util.render.renderers.impl.BuiltRectangle;
import dev.meoray.client.util.render.renderers.impl.BuiltText;
import dev.meoray.client.util.render.renderers.impl.BuiltTexture;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.Camera;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

public class Arrows extends Module {

    private static final Identifier ARROW_TEX = Identifier.of("meoray", "textures/gui/arrow.png");

    public final SectionSetting appearanceSection = add(new SectionSetting("Appearance"));
    public final NumberSetting size = appearanceSection.add(new NumberSetting("Size", 16.0, 6.0, 36.0, 1.0));
    public final NumberSetting radius = appearanceSection.add(new NumberSetting("Radius", 110.0, 40.0, 250.0, 5.0));
    public final NumberSetting maxDistance = appearanceSection.add(new NumberSetting("Max Distance", 100.0, 10.0, 500.0, 5.0));
    public final BooleanSetting showDistance = appearanceSection.add(new BooleanSetting("Show Distance", true));

    public final SectionSetting targetsSection = add(new SectionSetting("Targets"));
    public final BooleanSetting tPlayers = targetsSection.add(new BooleanSetting("Players", true));
    public final BooleanSetting tHostile = targetsSection.add(new BooleanSetting("Hostile Mobs", true));
    public final BooleanSetting tAnimals = targetsSection.add(new BooleanSetting("Animals", false));
    public final BooleanSetting tFriends = targetsSection.add(new BooleanSetting("Friends", true));
    public final BooleanSetting tInvisibles = targetsSection.add(new BooleanSetting("Invisibles", false));

    private static final int COLOR_PLAYER = 0xFFFF4444;
    private static final int COLOR_FRIEND = 0xFF44FF66;
    private static final int COLOR_HOSTILE = 0xFFFFAA22;
    private static final int COLOR_ANIMAL = 0xFF99DDFF;

    public Arrows() {
        super("Arrows", "PNG-стрелки указывают на сущностей", Category.RENDER);
    }

    public void render(DrawContext ctx) {
        if (!isEnabled()) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.world == null) return;

        Camera cam = mc.gameRenderer.getCamera();
        Vec3d camPos = cam.getPos();
        float yaw = cam.getYaw();
        int sw = mc.getWindow().getScaledWidth();
        int sh = mc.getWindow().getScaledHeight();
        float cx = sw / 2f;
        float cy = sh / 2f;
        float maxDist = maxDistance.getValue().floatValue();
        float arrowSize = size.getValue().floatValue();
        float rad = radius.getValue().floatValue();
        boolean showDist = showDistance.getValue();

        Matrix4f matrix = ctx.getMatrices().peek().getPositionMatrix();
        MsdfFont font = FontManager.SUISSEINTMEDIUM.get();

        for (Entity entity : mc.world.getEntities()) {
            if (entity == mc.player) continue;
            if (entity.isRemoved()) continue;
            if (!(entity instanceof LivingEntity living) || !living.isAlive()) continue;

            try { if (dev.meoray.client.feature.misc.FakePlayer.isFake(entity)) continue; } catch (Throwable ignored) {}
            if (living.isInvisible() && !tInvisibles.getValue()) continue;

            boolean isPlayer = entity instanceof PlayerEntity;
            boolean isHostile = entity instanceof HostileEntity;
            boolean isAnimal = entity instanceof AnimalEntity;

            boolean isFriend = false;
            if (isPlayer) {
                try {
                    isFriend = MeoRayClient.INSTANCE.getFriendManager()
                        .isFriend(((PlayerEntity) entity).getGameProfile().getName());
                } catch (Throwable ignored) {}
            }

            if (isFriend) {
                if (!tFriends.getValue()) continue;
            } else {
                if (isPlayer && !tPlayers.getValue()) continue;
                if (isHostile && !tHostile.getValue()) continue;
                if (isAnimal && !tAnimals.getValue()) continue;
                if (!isPlayer && !isHostile && !isAnimal) continue;
            }

            double dist = mc.player.distanceTo(living);
            if (dist > maxDist) continue;

            Vec3d targetPos = living.getPos();
            double dx = targetPos.x - camPos.x;
            double dz = targetPos.z - camPos.z;
            float targetAngle = (float) Math.toDegrees(Math.atan2(-dx, dz));
            float relAngle = MathHelper.wrapDegrees(targetAngle - yaw);
            float relRad = (float) Math.toRadians(relAngle);
            float ax = cx + (float) Math.sin(relRad) * rad;
            float ay = cy - (float) Math.cos(relRad) * rad;

            int col;
            if (isFriend) col = COLOR_FRIEND;
            else if (isPlayer) col = COLOR_PLAYER;
            else if (isHostile) col = COLOR_HOSTILE;
            else col = COLOR_ANIMAL;

            // Set texture right before rendering each arrow to prevent texture state corruption
            RenderSystem.setShaderTexture(0, ARROW_TEX);

            ctx.getMatrices().push();
            ctx.getMatrices().translate(ax, ay, 0);
            ctx.getMatrices().multiply(new org.joml.Quaternionf().rotateZ(relRad));
            Matrix4f local = ctx.getMatrices().peek().getPositionMatrix();

            float hs = arrowSize / 2f;
            ((BuiltTexture) Builder.texture()
                .size(new SizeState(arrowSize, arrowSize))
                .radius(new QuadRadiusState(0f))
                .color(new QuadColorState(col))
                .build()).render(local, -hs, -hs, 0);

            ctx.getMatrices().pop();

            if (showDist) {
                String d = String.format("%.0fm", dist);
                float ts = 5.5f;
                float tw = font.getWidth(d, ts);
                float tx = ax - tw / 2f;
                float ty = ay + arrowSize / 2f + 5;
                ((BuiltRectangle) Builder.rectangle()
                    .size(new SizeState(tw + 6, ts + 4))
                    .color(new QuadColorState(0xC0000000))
                    .radius(new QuadRadiusState(2.0))
                    .smoothness(1.15F)
                    .build()).render(matrix, tx - 3, ty - 2);
                ((BuiltText) Builder.text().font(font).text(d)
                    .color(col).size(ts).thickness(0.04F).build())
                    .render(matrix, tx, ty);
            }
        }
    }
}
