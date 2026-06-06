package dev.meoray.client.feature.render;

import dev.meoray.client.MeoRayClient;
import dev.meoray.client.core.Category;
import dev.meoray.client.core.Module;
import dev.meoray.client.core.setting.GroupSetting;
import dev.meoray.client.core.setting.NumberSetting;
import dev.meoray.client.managers.FontManager;
import dev.meoray.client.util.render.builders.Builder;
import dev.meoray.client.util.render.builders.states.QuadColorState;
import dev.meoray.client.util.render.builders.states.QuadRadiusState;
import dev.meoray.client.util.render.builders.states.SizeState;
import dev.meoray.client.util.render.msdf.MsdfFont;
import dev.meoray.client.util.render.renderers.impl.BuiltRectangle;
import dev.meoray.client.util.render.renderers.impl.BuiltText;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.Camera;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

public class Arrows extends Module {

    public final NumberSetting maxDistance = add(new NumberSetting("Max Distance", 100.0, 10.0, 500.0, 5.0));
    public final NumberSetting size = add(new NumberSetting("Size", 16.0, 6.0, 36.0, 1.0));
    public final NumberSetting radius = add(new NumberSetting("Radius", 110.0, 40.0, 250.0, 5.0));

    public final GroupSetting targets = add(new GroupSetting("Targets", "Кого подсвечивать стрелками")
            .add("Players", true)
            .add("Hostile Mobs", true)
            .add("Animals", false)
            .add("Friends", true)
            .add("Invisibles", false));

    public final GroupSetting visuals = add(new GroupSetting("Visuals", "Доп. инфа")
            .add("Show Distance", true));

    private static final int COLOR_PLAYER = 0xFFFF4444;
    private static final int COLOR_FRIEND = 0xFF44FF66;
    private static final int COLOR_HOSTILE = 0xFFFFAA22;
    private static final int COLOR_ANIMAL = 0xFF99DDFF;

    public Arrows() {
        super("Arrows", "Цветные стрелки указывают на сущности", Category.RENDER);
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

        boolean showPlayers = targets.get("Players");
        boolean showHostile = targets.get("Hostile Mobs");
        boolean showAnimals = targets.get("Animals");
        boolean showFriends = targets.get("Friends");
        boolean showInvis = targets.get("Invisibles");

        boolean showDist = visuals.get("Show Distance");

        Matrix4f matrix = ctx.getMatrices().peek().getPositionMatrix();
        MsdfFont font = FontManager.SUISSEINTMEDIUM.get();

        for (Entity entity : mc.world.getEntities()) {
            if (entity == mc.player) continue;
            if (entity.isRemoved()) continue;
            if (!(entity instanceof LivingEntity living) || !living.isAlive()) continue;

            try {
                if (dev.meoray.client.feature.misc.FakePlayer.isFake(entity)) continue;
            } catch (Throwable ignored) {}

            if (living.isInvisible() && !showInvis) continue;

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
                if (!showFriends) continue;
            } else {
                if (isPlayer && !showPlayers) continue;
                if (isHostile && !showHostile) continue;
                if (isAnimal && !showAnimals) continue;
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

            // === Чистая стрелка-указатель ===
            ctx.getMatrices().push();
            ctx.getMatrices().translate(ax, ay, 0);
            ctx.getMatrices().multiply(new org.joml.Quaternionf().rotateZ(relRad));
            Matrix4f local = ctx.getMatrices().peek().getPositionMatrix();

            drawArrow(local, arrowSize, col);

            ctx.getMatrices().pop();

            // === Дистанция снизу ===
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

                ((BuiltText) Builder.text()
                    .font(font).text(d)
                    .color(col).size(ts).thickness(0.04F)
                    .build()).render(matrix, tx, ty);
            }
        }
    }

    private void drawArrow(Matrix4f local, float arrowSize, int col) {
        float width = arrowSize * 0.85f;
        float height = arrowSize * 0.95f;

        int shadow = 0x90000000;

        drawFilledTriangle(local, width + 2f, height + 2f, shadow);

        drawFilledTriangle(local, width, height, col);
    }

    private void drawFilledTriangle(Matrix4f local, float width, float height, int col) {
        int slices = Math.max(16, (int) (height * 2.5f));
        float sliceH = height / slices;

        float topY = -height / 2f;
        float halfW = width / 2f;

        for (int i = 0; i < slices; i++) {
            float t = (i + 0.5f) / slices;
            float currentHalfW = halfW * t;

            float y = topY + i * sliceH;
            float x = -currentHalfW;
            float w = currentHalfW * 2f;

            ((BuiltRectangle) Builder.rectangle()
                .size(new SizeState(w, sliceH + 0.6f))
                .color(new QuadColorState(col))
                .radius(new QuadRadiusState(0.0))
                .smoothness(1.0F)
                .build()).render(local, x, y);
        }
    }
}
