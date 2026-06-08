package dev.meoray.client.feature.render;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.meoray.client.core.Category;
import dev.meoray.client.core.Module;
import dev.meoray.client.core.setting.BooleanSetting;
import dev.meoray.client.core.setting.ColorSetting;
import dev.meoray.client.core.setting.NumberSetting;
import dev.meoray.client.core.setting.SectionSetting;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.BlockView;
import org.joml.Matrix4f;

import java.awt.Color;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class XRay extends Module {

    private final MinecraftClient mc = MinecraftClient.getInstance();

    // === ORES ===
    public final SectionSetting oresSection = add(new SectionSetting("Ores"));
    public final BooleanSetting diamond = oresSection.add(new BooleanSetting("Diamond", true));
    public final BooleanSetting redstone = oresSection.add(new BooleanSetting("Redstone", true));
    public final BooleanSetting gold = oresSection.add(new BooleanSetting("Gold", true));
    public final BooleanSetting iron = oresSection.add(new BooleanSetting("Iron", true));
    public final BooleanSetting lapis = oresSection.add(new BooleanSetting("Lapis", true));

    // === COLORS ===
    public final SectionSetting colorsSection = add(new SectionSetting("Colors"));
    public final ColorSetting diamondColor = colorsSection.add(new ColorSetting("Diamond Color", new Color(85, 255, 255, 220)));
    public final ColorSetting redstoneColor = colorsSection.add(new ColorSetting("Redstone Color", new Color(255, 30, 30, 220)));
    public final ColorSetting goldColor = colorsSection.add(new ColorSetting("Gold Color", new Color(255, 215, 0, 220)));
    public final ColorSetting ironColor = colorsSection.add(new ColorSetting("Iron Color", new Color(220, 180, 150, 220)));
    public final ColorSetting lapisColor = colorsSection.add(new ColorSetting("Lapis Color", new Color(40, 100, 255, 220)));

    // === RENDER ===
    public final SectionSetting renderSection = add(new SectionSetting("Render"));
    public final BooleanSetting outline = renderSection.add(new BooleanSetting("Outline", true));
    public final BooleanSetting filled = renderSection.add(new BooleanSetting("Filled", true));
    public final NumberSetting lineWidth = renderSection.add(new NumberSetting("Line Width", 1.5, 0.5, 4.0, 0.1));
    public final NumberSetting fillAlpha = renderSection.add(new NumberSetting("Fill Alpha", 0.25, 0.05, 0.6, 0.01));
    public final NumberSetting maxDistance = renderSection.add(new NumberSetting("Max Distance", 48, 8, 128, 1));
    public final BooleanSetting tracers = renderSection.add(new BooleanSetting("Tracers", true));
    public final NumberSetting tracerWidth = renderSection.add(new NumberSetting("Tracer Width", 1.0, 0.5, 4.0, 0.1));
    public final BooleanSetting nameTags = renderSection.add(new BooleanSetting("Name Tags", true));
    public final BooleanSetting showDistance = renderSection.add(new BooleanSetting("Show Distance", true));

    // === MODE ===
    public final SectionSetting modeSection = add(new SectionSetting("Mode"));
    public final BooleanSetting exposedOnly = modeSection.add(new BooleanSetting("Exposed Only (Anti-Cheat Bypass)", true));
    public final BooleanSetting hideOtherBlocks = modeSection.add(new BooleanSetting("Hide Other Blocks", false));

    // === MISC ===
    public final SectionSetting miscSection = add(new SectionSetting("Misc"));
    public final BooleanSetting fullbright = miscSection.add(new BooleanSetting("Fullbright", true));
    public final NumberSetting gamma = miscSection.add(new NumberSetting("Gamma", 100, 1, 1000, 1));
    public final BooleanSetting reloadOnToggle = miscSection.add(new BooleanSetting("Reload On Toggle", true));

    private final Map<Block, ColorSetting> blockColors = new HashMap<>();
    private final Map<Block, String> blockNames = new HashMap<>();
    private double originalGamma = 1.0;
    private boolean gammaSaved = false;

    public XRay() {
        super("XRay", "Видит руды сквозь блоки (Anti-AntiXray)", Category.RENDER);
    }

    @Override
    public void onEnable() {
        rebuildMap();
        applyGamma(true);
        if (reloadOnToggle.getValue()) reloadChunks();
        super.onEnable();
    }

    @Override
    public void onDisable() {
        applyGamma(false);
        if (reloadOnToggle.getValue()) reloadChunks();
        super.onDisable();
    }

    @Override
    public void onTick() {
        rebuildMap();
        if (fullbright.getValue()) applyGamma(true);
    }

    private void rebuildMap() {
        blockColors.clear();
        blockNames.clear();
        if (diamond.getValue()) {
            blockColors.put(Blocks.DIAMOND_ORE, diamondColor);
            blockColors.put(Blocks.DEEPSLATE_DIAMOND_ORE, diamondColor);
            blockNames.put(Blocks.DIAMOND_ORE, "Алмазы");
            blockNames.put(Blocks.DEEPSLATE_DIAMOND_ORE, "Алмазы");
        }
        if (redstone.getValue()) {
            blockColors.put(Blocks.REDSTONE_ORE, redstoneColor);
            blockColors.put(Blocks.DEEPSLATE_REDSTONE_ORE, redstoneColor);
            blockNames.put(Blocks.REDSTONE_ORE, "Редстоун");
            blockNames.put(Blocks.DEEPSLATE_REDSTONE_ORE, "Редстоун");
        }
        if (gold.getValue()) {
            blockColors.put(Blocks.GOLD_ORE, goldColor);
            blockColors.put(Blocks.DEEPSLATE_GOLD_ORE, goldColor);
            blockColors.put(Blocks.NETHER_GOLD_ORE, goldColor);
            blockNames.put(Blocks.GOLD_ORE, "Золото");
            blockNames.put(Blocks.DEEPSLATE_GOLD_ORE, "Золото");
            blockNames.put(Blocks.NETHER_GOLD_ORE, "Золото");
        }
        if (iron.getValue()) {
            blockColors.put(Blocks.IRON_ORE, ironColor);
            blockColors.put(Blocks.DEEPSLATE_IRON_ORE, ironColor);
            blockNames.put(Blocks.IRON_ORE, "Железо");
            blockNames.put(Blocks.DEEPSLATE_IRON_ORE, "Железо");
        }
        if (lapis.getValue()) {
            blockColors.put(Blocks.LAPIS_ORE, lapisColor);
            blockColors.put(Blocks.DEEPSLATE_LAPIS_ORE, lapisColor);
            blockNames.put(Blocks.LAPIS_ORE, "Лазурит");
            blockNames.put(Blocks.DEEPSLATE_LAPIS_ORE, "Лазурит");
        }
    }

    /**
     * Главный метод проверки: должна ли руда отображаться.
     * @param state блок
     * @param pos позиция
     * @param view мир (для проверки соседей)
     * @return true если рендерим, false если скрываем
     */
    public boolean shouldShowOre(BlockState state, BlockPos pos, BlockView view) {
        if (state == null || state.isAir()) return false;
        if (!blockColors.containsKey(state.getBlock())) return false;
        if (!exposedOnly.getValue()) return true;
        return isExposed(pos, view);
    }

    /**
     * Проверяет, есть ли у блока соседняя грань с воздухом/водой/лавой.
     * Это значит что руда РЕАЛЬНАЯ (сервер показал её, потому что она в пещере),
     * а не фейковая от Orebfuscator.
     */
    public boolean isExposed(BlockPos pos, BlockView view) {
        if (view == null) return true;
        for (Direction dir : Direction.values()) {
            BlockState neighbor = view.getBlockState(pos.offset(dir));
            if (neighbor.isAir()) return true;
            if (!neighbor.getFluidState().isEmpty()) return true;
        }
        return false;
    }

    /**
     * Используется миксином, чтобы решить — скрывать ли все остальные блоки.
     */
    public boolean isOre(BlockState state) {
        if (state == null || state.isAir()) return false;
        return blockColors.containsKey(state.getBlock());
    }

    public boolean shouldHideNonOres() {
        return isEnabled() && hideOtherBlocks.getValue();
    }

    // === RENDER ===
    public void onWorldRender(MatrixStack matrices, float tickDelta) {
        if (!isEnabled()) return;
        if (mc.world == null || mc.player == null) return;

        Camera cam = mc.gameRenderer.getCamera();
        if (cam == null) return;
        Vec3d camPos = cam.getPos();

        int maxDist = maxDistance.getValue().intValue();
        int maxDistSq = maxDist * maxDist;

        BlockPos playerPos = mc.player.getBlockPos();

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);

        float fa = fillAlpha.getValue().floatValue();
        float lw = lineWidth.getValue().floatValue();

        for (int x = -maxDist; x <= maxDist; x++) {
            for (int y = -maxDist; y <= maxDist; y++) {
                for (int z = -maxDist; z <= maxDist; z++) {
                    if (x * x + y * y + z * z > maxDistSq) continue;

                    BlockPos pos = playerPos.add(x, y, z);
                    BlockState state = mc.world.getBlockState(pos);

                    if (!shouldShowOre(state, pos, mc.world)) continue;

                    ColorSetting cs = blockColors.get(state.getBlock());
                    if (cs == null) continue;
                    int col = cs.getValue().getRGB();

                    double rx = pos.getX() - camPos.x;
                    double ry = pos.getY() - camPos.y;
                    double rz = pos.getZ() - camPos.z;

                    matrices.push();
                    matrices.translate(rx, ry, rz);
                    Matrix4f mat = matrices.peek().getPositionMatrix();

                    if (filled.getValue()) {
                        drawBoxFilled(mat, withAlpha(col, fa));
                    }
                    if (outline.getValue()) {
                        RenderSystem.lineWidth(lw);
                        drawBoxOutline(mat, col);
                        RenderSystem.lineWidth(1f);
                    }

                    matrices.pop();
                }
            }
        }

        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }

    public void onHudRender(DrawContext context, float tickDelta) {
        if (!isEnabled()) return;
        if (mc.world == null || mc.player == null) return;
        if (!tracers.getValue() && !nameTags.getValue()) return;

        Camera cam = mc.gameRenderer.getCamera();
        if (cam == null) return;

        Matrix4f projView = dev.meoray.client.util.render.esp.EspMatrixHolder.projView;
        if (projView == null) return;

        Vec3d camPos = cam.getPos();
        int maxDist = maxDistance.getValue().intValue();
        int maxDistSq = maxDist * maxDist;
        BlockPos playerPos = mc.player.getBlockPos();

        int scW = mc.getWindow().getScaledWidth();
        int scH = mc.getWindow().getScaledHeight();
        float startX = scW / 2f;
        float startY = scH;
        TextRenderer tr = mc.textRenderer;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);

        Matrix4f mat = context.getMatrices().peek().getPositionMatrix();

        for (int x = -maxDist; x <= maxDist; x++) {
            for (int y = -maxDist; y <= maxDist; y++) {
                for (int z = -maxDist; z <= maxDist; z++) {
                    if (x * x + y * y + z * z > maxDistSq) continue;

                    BlockPos pos = playerPos.add(x, y, z);
                    BlockState state = mc.world.getBlockState(pos);

                    if (!shouldShowOre(state, pos, mc.world)) continue;

                    ColorSetting cs = blockColors.get(state.getBlock());
                    if (cs == null) continue;
                    int col = cs.getValue().getRGB();
                    float[] c = unpack(col);

                    // === ТРЕЙСЕР (к центру блока) ===
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

                    // === НАЗВАНИЕ (над верхней гранью) ===
                    if (nameTags.getValue()) {
                        String name = blockNames.get(state.getBlock());
                        if (name == null) name = "Ore";

                        double dist = Math.sqrt(pos.getSquaredDistance(mc.player.getPos()));
                        String label = "[" + name + "]";
                        if (showDistance.getValue()) {
                            label += " " + String.format(Locale.ROOT, "%.0fm", dist);
                        }

                        double tx = pos.getX() + 0.5 - camPos.x;
                        double ty = pos.getY() + 1.3 - camPos.y;
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

    private void drawBoxOutline(Matrix4f mat, int col) {
        float[] c = unpack(col);
        Tessellator tess = Tessellator.getInstance();
        BufferBuilder buf = tess.begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);
        line(buf, mat, 0, 0, 0, 1, 0, 0, c);
        line(buf, mat, 1, 0, 0, 1, 0, 1, c);
        line(buf, mat, 1, 0, 1, 0, 0, 1, c);
        line(buf, mat, 0, 0, 1, 0, 0, 0, c);
        line(buf, mat, 0, 1, 0, 1, 1, 0, c);
        line(buf, mat, 1, 1, 0, 1, 1, 1, c);
        line(buf, mat, 1, 1, 1, 0, 1, 1, c);
        line(buf, mat, 0, 1, 1, 0, 1, 0, c);
        line(buf, mat, 0, 0, 0, 0, 1, 0, c);
        line(buf, mat, 1, 0, 0, 1, 1, 0, c);
        line(buf, mat, 1, 0, 1, 1, 1, 1, c);
        line(buf, mat, 0, 0, 1, 0, 1, 1, c);
        BufferRenderer.drawWithGlobalProgram(buf.end());
    }

    private void line(BufferBuilder buf, Matrix4f mat,
                       float x1, float y1, float z1, float x2, float y2, float z2, float[] c) {
        buf.vertex(mat, x1, y1, z1).color(c[0], c[1], c[2], c[3]);
        buf.vertex(mat, x2, y2, z2).color(c[0], c[1], c[2], c[3]);
    }

    private void drawBoxFilled(Matrix4f mat, int col) {
        float[] c = unpack(col);
        Tessellator tess = Tessellator.getInstance();
        BufferBuilder buf = tess.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        quad(buf, mat, 0, 0, 0, 1, 0, 0, 1, 0, 1, 0, 0, 1, c);
        quad(buf, mat, 0, 1, 0, 1, 1, 0, 1, 1, 1, 0, 1, 1, c);
        quad(buf, mat, 0, 0, 0, 1, 0, 0, 1, 1, 0, 0, 1, 0, c);
        quad(buf, mat, 0, 0, 1, 1, 0, 1, 1, 1, 1, 0, 1, 1, c);
        quad(buf, mat, 0, 0, 0, 0, 0, 1, 0, 1, 1, 0, 1, 0, c);
        quad(buf, mat, 1, 0, 0, 1, 0, 1, 1, 1, 1, 1, 1, 0, c);
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

    private void applyGamma(boolean activate) {
        if (mc.options == null) return;
        try {
            if (activate && fullbright.getValue()) {
                if (!gammaSaved) {
                    originalGamma = mc.options.getGamma().getValue();
                    gammaSaved = true;
                }
                mc.options.getGamma().setValue(Math.min(1.0, (double) gamma.getValue().intValue()));
            } else {
                if (gammaSaved) {
                    mc.options.getGamma().setValue(originalGamma);
                    gammaSaved = false;
                }
            }
        } catch (Throwable ignored) {}
    }

    private void reloadChunks() {
        try {
            if (mc.worldRenderer != null) {
                mc.worldRenderer.reload();
            }
        } catch (Throwable ignored) {}
    }
}
