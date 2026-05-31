package dev.meoray.client.feature.render;

import dev.meoray.client.MeoRayClient;
import dev.meoray.client.core.Category;
import dev.meoray.client.core.Module;
import dev.meoray.client.core.setting.BooleanSetting;
import dev.meoray.client.core.setting.ModeSetting;
import dev.meoray.client.util.render.esp.EspMatrixHolder;
import dev.meoray.client.util.render.esp.FlatEspLayer;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.LimbAnimator;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Vector4f;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

public class ESP extends Module {

    public final BooleanSetting players = add(new BooleanSetting("Players", true));
    public final BooleanSetting monsters = add(new BooleanSetting("Monsters", false));
    public final BooleanSetting animals = add(new BooleanSetting("Animals", false));

    public final BooleanSetting chams = add(new BooleanSetting("Chams", false));
    public final ModeSetting chamsMode = add(new ModeSetting("Mode", "Glow", "Glow", "New", "Flat", "Glass", "Skeleton"));

    public final BooleanSetting nametag = add(new BooleanSetting("Nametag", false));
    public final BooleanSetting hpBar = add(new BooleanSetting("HP Bar", false));
    public final BooleanSetting box2d = add(new BooleanSetting("2D Box", false));

    public final BooleanSetting box3d = add(new BooleanSetting("3D Box", false));

    private final MinecraftClient mc = MinecraftClient.getInstance();

    public ESP() {
        super("ESP", "Подсветка игроков сквозь стены", Category.RENDER);
        WorldRenderEvents.AFTER_ENTITIES.register(this::onWorldRender);
        HudRenderCallback.EVENT.register(this::onHudRender);
    }

    @Override
    public void onTick() {
        if (mc.player == null || mc.world == null) return;
    }

    private void onWorldRender(WorldRenderContext ctx) {
        if (!isEnabled()) return;
        if (mc.player == null || mc.world == null) return;

        if (box3d.getValue()) {
            float tickDelta = ctx.tickCounter().getTickDelta(false);
            for (PlayerEntity player : mc.world.getPlayers()) {
                if (player == mc.player || !players.getValue()) continue;
                Box cur = player.getBoundingBox();
                Box prev = player.getBoundingBox().offset(player.prevX - player.getX(), player.prevY - player.getY(), player.prevZ - player.getZ());
                double minX = MathHelper.lerp(tickDelta, prev.minX, cur.minX);
                double minY = MathHelper.lerp(tickDelta, prev.minY, cur.minY);
                double minZ = MathHelper.lerp(tickDelta, prev.minZ, cur.minZ);
                double maxX = MathHelper.lerp(tickDelta, prev.maxX, cur.maxX);
                double maxY = MathHelper.lerp(tickDelta, prev.maxY, cur.maxY);
                double maxZ = MathHelper.lerp(tickDelta, prev.maxZ, cur.maxZ);
                Box smooth = new Box(minX, minY, minZ, maxX, maxY, maxZ);
                int color = MeoRayClient.INSTANCE.getThemeManager().getCurrentTheme().accent() & 0xFFFFFF | 0xFF000000;
                drawBox(smooth, color, 1.24F, ctx);
            }
        }

        if (chams.getValue() && chamsMode.getValue().equals("Skeleton")) {
            float tickDelta = ctx.tickCounter().getTickDelta(true);
            for (PlayerEntity player : mc.world.getPlayers()) {
                if (player == mc.player || !players.getValue()) continue;
                renderSkeleton(player, tickDelta, ctx);
            }
        }
    }

    private void onHudRender(DrawContext ctx, net.minecraft.client.render.RenderTickCounter tickCounter) {
        if (!isEnabled()) return;
        if (mc.player == null || mc.world == null) return;
        if (!nametag.getValue() && !box2d.getValue() && !hpBar.getValue()) return;

        for (PlayerEntity player : mc.world.getPlayers()) {
            if (player == mc.player || !players.getValue()) continue;

            double scale = mc.getWindow().getScaleFactor();
            int fbW = mc.getFramebuffer().textureWidth;
            int fbH = mc.getFramebuffer().textureHeight;
            float td = mc.getRenderTickCounter().getTickDelta(false);
            Vec3d cam = EspMatrixHolder.camera != null ? EspMatrixHolder.camera.getPos() : Vec3d.ZERO;
            double px = MathHelper.lerp(td, player.prevX, player.getX()) - cam.x;
            double py = MathHelper.lerp(td, player.prevY, player.getY()) - cam.y;
            double pz = MathHelper.lerp(td, player.prevZ, player.getZ()) - cam.z;
            float h = player.getHeight();
            float hw = player.getWidth() / 2.0F;
            Vec3d[] corners = new Vec3d[]{
                new Vec3d(px - hw, py, pz - hw), new Vec3d(px + hw, py, pz - hw),
                new Vec3d(px - hw, py, pz + hw), new Vec3d(px + hw, py, pz + hw),
                new Vec3d(px - hw, py + h, pz - hw), new Vec3d(px + hw, py + h, pz - hw),
                new Vec3d(px - hw, py + h, pz + hw), new Vec3d(px + hw, py + h, pz + hw)
            };
            float minX = Float.MAX_VALUE, minY = Float.MAX_VALUE;
            float maxX = -Float.MAX_VALUE, maxY = -Float.MAX_VALUE;
            boolean anyVisible = false;

            for (Vec3d c : corners) {
                float[] p = project(c, EspMatrixHolder.projView, fbW, fbH);
                if (p != null) {
                    float sx = p[0] / (float) scale;
                    float sy = p[1] / (float) scale;
                    minX = Math.min(minX, sx);
                    maxX = Math.max(maxX, sx);
                    minY = Math.min(minY, sy);
                    maxY = Math.max(maxY, sy);
                    anyVisible = true;
                }
            }

            if (!anyVisible) continue;

            float PAD = 2.0F;
            minX -= PAD; minY -= PAD; maxX += PAD; maxY += PAD;
            float boxW = maxX - minX;
            float boxH = maxY - minY;
            if (boxW < 2.0F || boxH < 2.0F) continue;

            Matrix4f matrix = ctx.getMatrices().peek().getPositionMatrix();
            int themeRGB = MeoRayClient.INSTANCE.getThemeManager().getCurrentTheme().accent();
            Color theme = new Color(themeRGB);

            if (box2d.getValue()) {
                float len = 8.0F;
                float thick = 1.5F;
                float l = Math.min(len, Math.min(boxW / 2.0F, boxH / 2.0F));
                rect(matrix, minX, minY, l, thick, theme, 0.0F);
                rect(matrix, minX, minY, thick, l, theme, 0.0F);
                rect(matrix, maxX - l, minY, l, thick, theme, 0.0F);
                rect(matrix, maxX - thick, minY, thick, l, theme, 0.0F);
                rect(matrix, minX, maxY - thick, l, thick, theme, 0.0F);
                rect(matrix, minX, maxY - l, thick, l, theme, 0.0F);
                rect(matrix, maxX - l, maxY - thick, l, thick, theme, 0.0F);
                rect(matrix, maxX - thick, maxY - l, thick, l, theme, 0.0F);
            }

            if (hpBar.getValue()) {
                float hp = player.getHealth();
                float maxHp = player.getMaxHealth();
                float hpFrac = MathHelper.clamp(hp / maxHp, 0.0F, 1.0F);
                Color hpCol = new Color(lerpColor(0xFF2020, 0x44FF44, hpFrac));
                float barX = minX - 4.0F;
                rect(matrix, barX, minY, 2.0F, boxH, new Color(0x80000000, true), 1.0F);
                float fillH = boxH * hpFrac;
                rect(matrix, barX, maxY - fillH, 2.0F, fillH, hpCol, 1.0F);
            }

            if (nametag.getValue()) {
                String name = player.getNameForScoreboard();
                if (name.isEmpty() || !name.matches(".*[a-zA-Zа-яА-Я0-9].*")) name = "unknown";
                String text = name + " [" + (int) player.getHealth() + "]";
                TextRenderer textRenderer = mc.textRenderer;
                float tw = textRenderer.getWidth(text);
                float tx = (minX + maxX - tw) / 2.0F;
                float ty = minY - 14.0F;
                rect(matrix, tx - 2.0F, ty - 1.0F, tw + 4.0F, 10.0F, new Color(0x80000000, true), 1.0F);
                ctx.getMatrices().push();
                ctx.getMatrices().translate(0, 0, 100);
                ctx.drawText(textRenderer, text, (int) tx, (int) ty, 0xFFFFFFFF, true);
                ctx.getMatrices().pop();
            }
        }
    }

    private void renderSkeleton(PlayerEntity player, float partialTicks, WorldRenderContext ctx) {
        Camera camera = mc.gameRenderer.getCamera();
        Vec3d camPos = camera.getPos();
        Vec3d pos = player.getLerpedPos(partialTicks);
        double x = pos.x - camPos.x;
        double y = pos.y - camPos.y;
        double z = pos.z - camPos.z;
        float scale = player.isBaby() ? 0.5F : 1.05F;
        double headY = 1.6 * scale;
        double torsoTopY = 1.2 * scale;
        double torsoBottomY = 0.6 * scale;
        double armOffset = 0.33;
        double handY = 0.8 * scale;
        double footY = 0.0;
        double legOffset = 0.15 * scale;

        if (player.isSwimming()) {
            headY = 1.5 * scale; torsoTopY = 1.2 * scale; torsoBottomY = 0.6 * scale;
            handY = 0.8 * scale; footY = 0.1 * scale; armOffset = 0.33;
            legOffset = 0.25 * scale;
        } else if (player.isInSneakingPose()) {
            headY = 1.25 * scale; torsoTopY = 0.95 * scale; torsoBottomY = 0.45 * scale;
            handY = 0.55 * scale; footY = -0.1 * scale;
        } else if (player.isGliding()) {
            headY = 1.5 * scale; torsoTopY = 1.2 * scale; torsoBottomY = 0.6 * scale;
            handY = 0.8 * scale; footY = 0.1 * scale; armOffset = 0.33;
        }

        int lineColor = MeoRayClient.INSTANCE.getThemeManager().getCurrentTheme().accent() & 0xFFFFFF | 0xFF000000;
        float lineWidth = 1.0F;
        float bodyYaw = MathHelper.lerpAngleDegrees(partialTicks, player.prevBodyYaw, player.bodyYaw);
        float headYaw = MathHelper.lerpAngleDegrees(partialTicks, player.prevHeadYaw, player.headYaw);
        float headPitch = MathHelper.lerp(partialTicks, player.prevPitch, player.getPitch());
        float netHeadYaw = headYaw - bodyYaw;
        headPitch = MathHelper.clamp(headPitch, -60.0F, 60.0F);
        LimbAnimator limb = player.limbAnimator;
        float limbAngle = limb.getPos(partialTicks);
        float limbDistance = limb.getSpeed(partialTicks);
        if (player.isBaby()) { limbAngle *= 3.0F; limbDistance *= 0.8F; }
        if (limbDistance > 1.0F) limbDistance = 1.0F;

        float rightArmRX = MathHelper.cos(limbAngle * 0.6662F + (float) Math.PI) * 2.0F * limbDistance * 0.5F;
        float leftArmRX = MathHelper.cos(limbAngle * 0.6662F) * 2.0F * limbDistance * 0.5F;
        float rightLegRX = MathHelper.cos(limbAngle * 0.6662F) * 1.4F * limbDistance;
        float leftLegRX = MathHelper.cos(limbAngle * 0.6662F + (float) Math.PI) * 1.4F * limbDistance;
        float swingProgress = player.getHandSwingProgress(partialTicks);
        if (swingProgress > 0.0F) leftArmRX += -MathHelper.sin(swingProgress * (float) Math.PI) * 1.5F;

        var matrices = new net.minecraft.client.util.math.MatrixStack();
        matrices.push();
        matrices.translate(x, y, z);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-bodyYaw));
        if (!player.isSwimming() && !player.isGliding()) {
            if (player.isInSneakingPose()) {
                matrices.translate(0.0F, -0.1F * scale, 0.0F);
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(4.0F));
            }
        } else {
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(headPitch + 90.0F));
        }

        var bodyEntry = matrices.peek();
        matrices.push();
        matrices.translate(0.0F, (float) torsoTopY, 0.0F);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-netHeadYaw));
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(headPitch * 0.5F));
        drawLine(matrices.peek(), 0, 0, 0, 0, headY - torsoTopY, 0, lineColor, lineWidth);
        matrices.pop();
        drawLine(bodyEntry, 0, torsoTopY, 0, 0, torsoBottomY, 0, lineColor, lineWidth);
        drawLine(bodyEntry, 0, torsoTopY, 0, -armOffset, torsoTopY, 0, lineColor, lineWidth);
        matrices.push();
        matrices.translate(-armOffset, (float) torsoTopY, 0.0F);
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(leftArmRX * (180F / (float) Math.PI)));
        drawLine(matrices.peek(), 0, 0, 0, 0, handY - torsoTopY, 0, lineColor, lineWidth);
        matrices.pop();
        drawLine(bodyEntry, 0, torsoTopY, 0, armOffset, torsoTopY, 0, lineColor, lineWidth);
        matrices.push();
        matrices.translate(armOffset, (float) torsoTopY, 0.0F);
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(rightArmRX * (180F / (float) Math.PI)));
        drawLine(matrices.peek(), 0, 0, 0, 0, handY - torsoTopY, 0, lineColor, lineWidth);
        matrices.pop();
        drawLine(bodyEntry, 0, torsoBottomY, 0, -legOffset, torsoBottomY, 0, lineColor, lineWidth);
        matrices.push();
        matrices.translate(-legOffset, (float) torsoBottomY, 0.0F);
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(leftLegRX * (180F / (float) Math.PI)));
        drawLine(matrices.peek(), 0, 0, 0, 0, footY - torsoBottomY, 0, lineColor, lineWidth);
        matrices.pop();
        drawLine(bodyEntry, 0, torsoBottomY, 0, legOffset, torsoBottomY, 0, lineColor, lineWidth);
        matrices.push();
        matrices.translate(legOffset, (float) torsoBottomY, 0.0F);
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(rightLegRX * (180F / (float) Math.PI)));
        drawLine(matrices.peek(), 0, 0, 0, 0, footY - torsoBottomY, 0, lineColor, lineWidth);
        matrices.pop();
        matrices.pop();
    }

    private void drawLine(net.minecraft.client.util.math.MatrixStack.Entry entry, double x1, double y1, double z1, double x2, double y2, double z2, int color, float width) {
        Matrix4f m = entry.getPositionMatrix();
        var buf = Tessellator.getInstance().begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);
        buf.vertex(m, (float) x1, (float) y1, (float) z1).color(color);
        buf.vertex(m, (float) x2, (float) y2, (float) z2).color(color);
        BufferRenderer.drawWithGlobalProgram(buf.end());
    }

    private void drawBox(Box box, int color, float lineWidth, WorldRenderContext ctx) {
        if (ctx.matrixStack() == null) return;
        var matrices = ctx.matrixStack();
        var camera = ctx.camera();
        Vec3d camPos = camera.getPos();
        Matrix4f m = matrices.peek().getPositionMatrix();
        double minX = box.minX - camPos.x, minY = box.minY - camPos.y, minZ = box.minZ - camPos.z;
        double maxX = box.maxX - camPos.x, maxY = box.maxY - camPos.y, maxZ = box.maxZ - camPos.z;

        var buf = Tessellator.getInstance().begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);
        renderEdge(buf, m, color, minX, minY, minZ, maxX, minY, minZ);
        renderEdge(buf, m, color, maxX, minY, minZ, maxX, minY, maxZ);
        renderEdge(buf, m, color, maxX, minY, maxZ, minX, minY, maxZ);
        renderEdge(buf, m, color, minX, minY, minZ, minX, minY, maxZ);
        renderEdge(buf, m, color, minX, maxY, minZ, maxX, maxY, minZ);
        renderEdge(buf, m, color, maxX, maxY, minZ, maxX, maxY, maxZ);
        renderEdge(buf, m, color, maxX, maxY, maxZ, minX, maxY, maxZ);
        renderEdge(buf, m, color, minX, maxY, minZ, minX, maxY, maxZ);
        renderEdge(buf, m, color, minX, minY, minZ, minX, maxY, minZ);
        renderEdge(buf, m, color, maxX, minY, minZ, maxX, maxY, minZ);
        renderEdge(buf, m, color, maxX, minY, maxZ, maxX, maxY, maxZ);
        renderEdge(buf, m, color, minX, minY, maxZ, minX, maxY, maxZ);
        BufferRenderer.drawWithGlobalProgram(buf.end());
    }

    private void renderEdge(BufferBuilder buf, Matrix4f m, int color, double x1, double y1, double z1, double x2, double y2, double z2) {
        buf.vertex(m, (float) x1, (float) y1, (float) z1).color(color);
        buf.vertex(m, (float) x2, (float) y2, (float) z2).color(color);
    }

    public static float[] project(Vec3d pos, Matrix4f projView, int fbW, int fbH) {
        if (projView == null) return null;
        Vector4f v = new Vector4f((float) pos.getX(), (float) pos.getY(), (float) pos.getZ(), 1.0F);
        v.mul(projView);
        if (v.w <= 0.0F) return null;
        float nx = v.x / v.w;
        float ny = -v.y / v.w;
        return new float[]{(nx + 1.0F) / 2.0F * (float) fbW, (ny + 1.0F) / 2.0F * (float) fbH};
    }

    private static void rect(Matrix4f m, float x, float y, float w, float h, Color color, float radius) {
        if (w <= 0.0F || h <= 0.0F) return;
        var rect = dev.meoray.client.util.render.builders.Builder.rectangle()
            .size(new dev.meoray.client.util.render.builders.states.SizeState(w, h))
            .color(new dev.meoray.client.util.render.builders.states.QuadColorState(color))
            .radius(new dev.meoray.client.util.render.builders.states.QuadRadiusState(radius))
            .smoothness(1.0F).build();
        rect.render(m, x, y);
    }

    private static int lerpColor(int from, int to, float t) {
        t = MathHelper.clamp(t, 0.0F, 1.0F);
        int r = (int) ((from >> 16 & 255) * (1.0F - t) + (to >> 16 & 255) * t);
        int g = (int) ((from >> 8 & 255) * (1.0F - t) + (to >> 8 & 255) * t);
        int b = (int) ((from & 255) * (1.0F - t) + (to & 255) * t);
        return r << 16 | g << 8 | b;
    }
}
