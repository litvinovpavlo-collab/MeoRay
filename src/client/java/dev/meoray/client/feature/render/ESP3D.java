package dev.meoray.client.feature.render;

import dev.meoray.client.core.Category;
import dev.meoray.client.core.Module;
import dev.meoray.client.core.setting.BooleanSetting;
import dev.meoray.client.core.setting.ColorSetting;
import dev.meoray.client.core.setting.ModeSetting;
import dev.meoray.client.core.setting.NumberSetting;
import dev.meoray.client.utils.render.Render3DUtils;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Box;

import java.awt.Color;

public class ESP3D extends Module {

    private final ModeSetting mode = new ModeSetting("Mode", "Outline", "Outline", "Filled", "Both", "Tracer");
    private final BooleanSetting players = new BooleanSetting("Players", true);
    private final BooleanSetting mobs = new BooleanSetting("Mobs", true);
    private final BooleanSetting tracers = new BooleanSetting("Tracers", false);

    private final NumberSetting lineWidth = new NumberSetting("Line Width", 2.0, 0.5, 5.0, 0.5);
    private final NumberSetting fillAlpha = new NumberSetting("Fill Alpha", 40.0, 0.0, 255.0, 5.0);

    private final ColorSetting boxColor = new ColorSetting("Box Color", new Color(180, 60, 255));
    private final ColorSetting tracerColor = new ColorSetting("Tracer Color", new Color(180, 60, 255));

    public ESP3D() {
        super("ESP3D", "Draws 3D boxes around entities", Category.RENDER);
        addSettings(mode, players, mobs, tracers, lineWidth, fillAlpha, boxColor, tracerColor);
    }

    public void onWorldRender(MatrixStack matrices, float tickDelta) {
        if (mc.world == null || mc.player == null) return;

        for (Entity entity : mc.world.getEntities()) {
            if (entity == mc.player) continue;
            if (!isValid(entity)) continue;

            LivingEntity living = (LivingEntity) entity;
            Box box = Render3DUtils.getInterpolatedBox(entity, tickDelta);

            int color = getEntityColor(living);
            String m = mode.getValue();

            if (m.equals("Filled") || m.equals("Both")) {
                int fillColor = (color & 0x00FFFFFF) | (fillAlpha.getValue().intValue() << 24);
                Render3DUtils.drawBoxFilled(matrices, box, fillColor);
            }

            if (m.equals("Outline") || m.equals("Both")) {
                Render3DUtils.drawBoxOutline(matrices, box, color, lineWidth.getValue().floatValue());
            }

            if (m.equals("Tracer") || tracers.getValue()) {
                int tc = tracerColor.getRGB() | 0xFF000000;
                Render3DUtils.drawTracer(matrices,
                        box.getCenter().add(0, (box.maxY - box.minY) / 2, 0),
                        tc, lineWidth.getValue().floatValue());
            }
        }
    }

    private int getEntityColor(LivingEntity entity) {
        int base = boxColor.getRGB() | 0xFF000000;

        if (entity instanceof PlayerEntity) {
            if (entity.hurtTime > 0) return new Color(255, 50, 50).getRGB();
            return base;
        }
        if (entity instanceof HostileEntity) {
            return new Color(255, 80, 80).getRGB();
        }
        return base;
    }

    private boolean isValid(Entity entity) {
        if (!(entity instanceof LivingEntity living)) return false;
        if (!living.isAlive()) return false;
        if (entity == mc.player) return false;

        if (entity instanceof PlayerEntity) return players.getValue();
        return mobs.getValue();
    }
}
