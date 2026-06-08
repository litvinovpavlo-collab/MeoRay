package dev.meoray.client.feature.render;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.meoray.client.core.Category;
import dev.meoray.client.core.Module;
import dev.meoray.client.core.setting.BooleanSetting;
import dev.meoray.client.core.setting.ColorSetting;
import dev.meoray.client.core.setting.NumberSetting;
import dev.meoray.client.core.setting.SectionSetting;
import net.minecraft.block.entity.BarrelBlockEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.block.entity.EnderChestBlockEntity;
import net.minecraft.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.block.entity.TrappedChestBlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.chunk.WorldChunk;
import org.joml.Matrix4f;

import java.awt.Color;
import java.util.Locale;

public class ChestESP extends Module {

    private final MinecraftClient mc = MinecraftClient.getInstance();

    // === TYPES ===
    public final SectionSetting typesSection = add(new SectionSetting("Types"));
    public final BooleanSetting chests = typesSection.add(new BooleanSetting("Chests", true));
    public final BooleanSetting trappedChests = typesSection.add(new BooleanSetting("Trapped Chests", true));
    public final BooleanSetting shulkers = typesSection.add(new BooleanSetting("Shulker Boxes", true));
    public final BooleanSetting enderChests = typesSection.add(new BooleanSetting("Ender Chests", true));
    public final BooleanSetting barrels = typesSection.add(new BooleanSetting("Barrels", true));

    // === COLORS ===
    public final SectionSetting colorsSection = add(new SectionSetting("Colors"));
    public final ColorSetting chestColor = colorsSection.add(new ColorSetting("Chest Color", new Color(50, 220, 80, 220)));
    public final ColorSetting trappedColor = colorsSection.add(new ColorSetting("Trapped Color", new Color(255, 70, 70, 220)));
    public final ColorSetting shulkerColor = colorsSection.add(new ColorSetting("Shulker Color", new Color(200, 60, 255, 220)));
    public final ColorSetting enderColor = colorsSection.add(new ColorSetting("Ender Color", new Color(40, 230, 230, 220)));
    public final ColorSetting barrelColor = colorsSection.add(new ColorSetting("Barrel Color", new Color(255, 150, 30, 220)));

    // === RENDER ===
    public final SectionSetting renderSection = add(new SectionSetting("Render"));
    public final BooleanSetting outline = renderSection.add(new BooleanSetting("Outline", true));
    public final BooleanSetting filled = renderSection.add(new BooleanSetting("Filled", true));
    public final NumberSetting lineWidth = renderSection.add(new NumberSetting("Line Width", 1.5, 0.5, 4.0, 0.1));
    public final NumberSetting fillAlpha = renderSection.add(new NumberSetting("Fill Alpha", 0.15, 0.05, 0.6, 0.01));
    public final BooleanSetting throughWalls = renderSection.add(new BooleanSetting("Through Walls", true));
    public final NumberSetting maxDistance = renderSection.add(new NumberSetting("Max Distance", 64, 8, 256, 1));

    // === TRACERS ===
    public final SectionSetting tracersSection = add(new SectionSetting("Tracers"));
    public final BooleanSetting tracers = tracersSection.add(new BooleanSetting("Tracers", true));
    public final NumberSetting tracerWidth = tracersSection.add(new NumberSetting("Tracer Width", 1.0, 0.5, 4.0, 0.1));
    public final BooleanSetting nameTags = tracersSection.add(new BooleanSetting("Name Tags", true));
    public final BooleanSetting showDistance = tracersSection.add(new BooleanSetting("Show Distance", true));

    public ChestESP() {
        super("ChestESP", "Подсветка сундуков, шалкеров, бочек", Category.RENDER);
    }

    private Color getColorFor(BlockEntity be) {
        if (be instanceof TrappedChestBlockEntity) {
            return trappedChests.getValue() ? trappedColor.getValue() : null;
        }
        if (be instanceof ChestBlockEntity) {
            return chests.getValue() ? chestColor.getValue() : null;
        }
        if (be instanceof ShulkerBoxBlockEntity) {
            return shulkers.getValue() ? shulkerColor.getValue() : null;
        }
        if (be instanceof EnderChestBlockEntity) {
            return enderChests.getValue() ? enderColor.getValue() : null;
        }
        if (be instanceof BarrelBlockEntity) {
            return barrels.getValue() ? barrelColor.getValue() : null;
        }
        return null;
    }

    // === WORLD RENDER ===
    public void onWorldRender(MatrixStack matrices, float tickDelta) {
        if (!isEnabled()) return;
        if (mc.world == null || mc.player == null) return;

        Camera cam = mc.gameRenderer.getCamera();
        if (cam == null) return;
        Vec3d camPos = cam.getPos();
        double maxDistSq = maxDistance.getValue().doubleValue() * maxDistance.getValue().doubleValue();

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        if (throughWalls.getValue()) RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);

        float fa = fillAlpha.getValue().floatValue();
        float lw = lineWidth.getValue().floatValue();

        ClientWorld world = mc.world;
        int viewDist = mc.options.getViewDistance().getValue();
        int playerChunkX = mc.player.getChunkPos().x;
        int playerChunkZ = mc.player.getChunkPos().z;

        for (int cx = playerChunkX - viewDist; cx <= playerChunkX + viewDist; cx++) {
            for (int cz = playerChunkZ - viewDist; cz <= playerChunkZ + viewDist; cz++) {
                WorldChunk chunk = world.getChunkManager().getWorldChunk(cx, cz);
                if (chunk == null) continue;

                for (BlockEntity be : chunk.getBlockEntities().values()) {
                    Color color = getColorFor(be);
                    if (color == null) continue;

                    BlockPos pos = be.getPos();
                    double distSq = pos.getSquaredDistance(mc.player.getPos());
                    if (distSq > maxDistSq) continue;

                    int col = color.getRGB();

                    double rx = pos.getX() - camPos.x;
                    double ry = pos.getY() - camPos.y;
                    double rz = pos.getZ() - camPos.z;

                    matrices.push();
                    matrices.translate(rx, ry, rz);
                    Matrix4f mat = matrices.peek().getPositionMatrix();

                    float maxY = (be instanceof ChestBlockEntity || be instanceof EnderChestBlockEntity)
                                  ? 0.875f : 1f;

                    if (filled.getValue()) {
                        drawBoxFilled(mat, 0f, 0f, 0f, 1f, maxY, 1f, withAlpha(col, fa));
                    }
                    if (outline.getValue()) {
                        RenderSystem.lineWidth(lw);
                        drawBoxOutline(mat, 0f, 0f, 0f, 1f, maxY, 1f, col);
                        RenderSystem.lineWidth(1f);
                    }

                    matrices.pop();
                }
            }
        }

        RenderSystem.depthMask(true);
        if (throughWalls.getValue()) RenderSystem.enableDepthTest();
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }

    private String getNameFor(BlockEntity be) {
        if (be instanceof TrappedChestBlockEntity) return "Сундук-Ловушка";
        if (be instanceof ChestBlockEntity) return "Сундук";
        if (be instanceof ShulkerBoxBlockEntity) return "Шалкер";
        if (be instanceof EnderChestBlockEntity) return "Эндер Сундук";
        if (be instanceof BarrelBlockEntity) return "Бочка";
        return "Контейнер";
    }

    // === HUD RENDER (TRACERS + NAME TAGS) ===
    public void onHudRender(DrawContext context, float tickDelta) {
        if (!isEnabled()) return;
        if (!tracers.getValue() && !nameTags.getValue()) return;
        if (mc.world == null || mc.player == null) return;

        Camera cam = mc.gameRenderer.getCamera();
        if (cam == null) return;

        Matrix4f projView = dev.meoray.client.util.render.esp.EspMatrixHolder.projView;
        if (projView == null) return;

        Vec3d camPos = cam.getPos();
        double maxDistSq = maxDistance.getValue().doubleValue() * maxDistance.getValue().doubleValue();

        int scW = mc.getWindow().getScaledWidth();
        int scH = mc.getWindow().getScaledHeight();
        float startX = scW / 2f;
        float startY = scH;
        TextRenderer tr = mc.textRenderer;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);

        Matrix4f mat = context.getMatrices().peek().getPositionMatrix();

        ClientWorld world = mc.world;
        int viewDist = mc.options.getViewDistance().getValue();
        int playerChunkX = mc.player.getChunkPos().x;
        int playerChunkZ = mc.player.getChunkPos().z;

        for (int cx = playerChunkX - viewDist; cx <= playerChunkX + viewDist; cx++) {
            for (int cz = playerChunkZ - viewDist; cz <= playerChunkZ + viewDist; cz++) {
                WorldChunk chunk = world.getChunkManager().getWorldChunk(cx, cz);
                if (chunk == null) continue;

                for (BlockEntity be : chunk.getBlockEntities().values()) {
                    Color color = getColorFor(be);
                    if (color == null) continue;

                    BlockPos pos = be.getPos();
                    double distSq = pos.getSquaredDistance(mc.player.getPos());
                    if (distSq > maxDistSq) continue;

                    int col = color.getRGB();
                    float[] c = unpack(col);

                    // === ТРЕЙСЕР ===
                    if (tracers.getValue()) {
                        double wx = pos.getX() + 0.5 - camPos.x;
                        double wy = pos.getY() + 0.5 - camPos.y;
                        double wz = pos.getZ() + 0.5 - camPos.z;

                        org.joml.Vector4f screenPos = new org.joml.Vector4f((float) wx, (float) wy, (float) wz, 1f);
                        projView.transform(screenPos);
                        if (screenPos.w > 0) {
                            float invW = 1f / screenPos.w;
                            float sx = (screenPos.x * invW * 0.5f + 0.5f) * scW;
                            float sy = (1f - (screenPos.y * invW * 0.5f + 0.5f)) * scH;

                            RenderSystem.lineWidth(tracerWidth.getValue().floatValue());
                            Tessellator tess = Tessellator.getInstance();
                            BufferBuilder buf = tess.begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);
                            buf.vertex(mat, startX, startY, 0f).color(c[0], c[1], c[2], c[3] * 0.15f);
                            buf.vertex(mat, sx, sy, 0f).color(c[0], c[1], c[2], c[3]);
                            BufferRenderer.drawWithGlobalProgram(buf.end());
                            RenderSystem.lineWidth(1f);
                        }
                    }

                    // === НАЗВАНИЕ ===
                    if (nameTags.getValue()) {
                        String name = getNameFor(be);
                        double dist = Math.sqrt(distSq);

                        String label = "[" + name + "]";
                        if (showDistance.getValue()) {
                            label += " " + String.format(Locale.ROOT, "%.0fm", dist);
                        }

                        double offsetY = (be instanceof ChestBlockEntity || be instanceof EnderChestBlockEntity)
                                          ? 1.15 : 1.3;

                        double tx = pos.getX() + 0.5 - camPos.x;
                        double ty = pos.getY() + offsetY - camPos.y;
                        double tz = pos.getZ() + 0.5 - camPos.z;

                        org.joml.Vector4f tagPos = new org.joml.Vector4f((float) tx, (float) ty, (float) tz, 1f);
                        projView.transform(tagPos);
                        if (tagPos.w <= 0) continue;

                        float invW = 1f / tagPos.w;
                        float screenX = (tagPos.x * invW * 0.5f + 0.5f) * scW;
                        float screenY = (1f - (tagPos.y * invW * 0.5f + 0.5f)) * scH;

                        int labelW = tr.getWidth(label);
                        int textX = (int) (screenX - labelW / 2f);
                        int textY = (int) screenY;
                        int pad = 3;

                        int bgCol = 0xB0000000;
                        context.fill(textX - pad, textY - 2, textX + labelW + pad, textY + 10, bgCol);
                        context.fill(textX - pad, textY + 9, textX + labelW + pad, textY + 10, col | 0xFF000000);
                        context.drawText(tr, label, textX, textY, col | 0xFF000000, false);
                    }
                }
            }
        }

        RenderSystem.disableBlend();
    }

    // === DRAW HELPERS ===
    private void drawBoxOutline(Matrix4f mat, float minX, float minY, float minZ,
                                 float maxX, float maxY, float maxZ, int col) {
        float[] c = unpack(col);
        Tessellator tess = Tessellator.getInstance();
        BufferBuilder buf = tess.begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);
        line(buf, mat, minX, minY, minZ, maxX, minY, minZ, c);
        line(buf, mat, maxX, minY, minZ, maxX, minY, maxZ, c);
        line(buf, mat, maxX, minY, maxZ, minX, minY, maxZ, c);
        line(buf, mat, minX, minY, maxZ, minX, minY, minZ, c);
        line(buf, mat, minX, maxY, minZ, maxX, maxY, minZ, c);
        line(buf, mat, maxX, maxY, minZ, maxX, maxY, maxZ, c);
        line(buf, mat, maxX, maxY, maxZ, minX, maxY, maxZ, c);
        line(buf, mat, minX, maxY, maxZ, minX, maxY, minZ, c);
        line(buf, mat, minX, minY, minZ, minX, maxY, minZ, c);
        line(buf, mat, maxX, minY, minZ, maxX, maxY, minZ, c);
        line(buf, mat, maxX, minY, maxZ, maxX, maxY, maxZ, c);
        line(buf, mat, minX, minY, maxZ, minX, maxY, maxZ, c);
        BufferRenderer.drawWithGlobalProgram(buf.end());
    }

    private void line(BufferBuilder buf, Matrix4f mat,
                       float x1, float y1, float z1, float x2, float y2, float z2, float[] c) {
        buf.vertex(mat, x1, y1, z1).color(c[0], c[1], c[2], c[3]);
        buf.vertex(mat, x2, y2, z2).color(c[0], c[1], c[2], c[3]);
    }

    private void drawBoxFilled(Matrix4f mat, float minX, float minY, float minZ,
                                float maxX, float maxY, float maxZ, int col) {
        float[] c = unpack(col);
        Tessellator tess = Tessellator.getInstance();
        BufferBuilder buf = tess.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        quad(buf, mat, minX, minY, minZ, maxX, minY, minZ, maxX, minY, maxZ, minX, minY, maxZ, c);
        quad(buf, mat, minX, maxY, minZ, maxX, maxY, minZ, maxX, maxY, maxZ, minX, maxY, maxZ, c);
        quad(buf, mat, minX, minY, minZ, maxX, minY, minZ, maxX, maxY, minZ, minX, maxY, minZ, c);
        quad(buf, mat, minX, minY, maxZ, maxX, minY, maxZ, maxX, maxY, maxZ, minX, maxY, maxZ, c);
        quad(buf, mat, minX, minY, minZ, minX, minY, maxZ, minX, maxY, maxZ, minX, maxY, minZ, c);
        quad(buf, mat, maxX, minY, minZ, maxX, minY, maxZ, maxX, maxY, maxZ, maxX, maxY, minZ, c);
        BufferRenderer.drawWithGlobalProgram(buf.end());
    }

    private void quad(BufferBuilder buf, Matrix4f mat,
                       float x1, float y1, float z1, float x2, float y2, float z2,
                       float x3, float y3, float z3, float x4, float y4, float z4, float[] c) {
        buf.vertex(mat, x1, y1, z1).color(c[0], c[1], c[2], c[3]);
        buf.vertex(mat, x2, y2, z2).color(c[0], c[1], c[2], c[3]);
        buf.vertex(mat, x3, y3, z3).color(c[0], c[1], c[2], c[3]);
        buf.vertex(mat, x4, y4, z4).color(c[0], c[1], c[2], c[3]);
    }

    private float[] unpack(int col) {
        return new float[]{
                ((col >> 16) & 0xFF) / 255f,
                ((col >> 8) & 0xFF) / 255f,
                (col & 0xFF) / 255f,
                ((col >> 24) & 0xFF) / 255f
        };
    }

    private int withAlpha(int col, float mul) {
        int a = (int)(((col >> 24) & 0xFF) * mul);
        return (Math.max(0, Math.min(255, a)) << 24) | (col & 0x00FFFFFF);
    }
}
