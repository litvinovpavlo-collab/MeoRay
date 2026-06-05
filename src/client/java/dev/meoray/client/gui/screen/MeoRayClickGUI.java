package dev.meoray.client.gui.screen;

import dev.meoray.client.MeoRayClient;
import dev.meoray.client.core.Category;
import dev.meoray.client.core.Module;
import dev.meoray.client.core.setting.BooleanSetting;
import dev.meoray.client.core.setting.ModeSetting;
import dev.meoray.client.core.setting.NumberSetting;
import dev.meoray.client.core.setting.Setting;
import dev.meoray.client.gui.theme.Theme;
import dev.meoray.client.gui.theme.ThemeManager;
import dev.meoray.client.managers.FontManager;
import dev.meoray.client.ui.gui.elements.SearchElement;
import dev.meoray.client.util.animations.AnimatedFloat;
import dev.meoray.client.util.animations.Easing;
import dev.meoray.client.util.render.builders.Builder;
import dev.meoray.client.util.render.builders.states.QuadColorState;
import dev.meoray.client.util.render.builders.states.QuadRadiusState;
import dev.meoray.client.util.render.builders.states.SizeState;
import dev.meoray.client.util.render.msdf.MsdfFont;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.meoray.client.util.render.GlowRenderer;
import dev.meoray.client.util.render.renderers.impl.BuiltBlur;
import dev.meoray.client.util.render.renderers.impl.BuiltBorder;
import dev.meoray.client.util.render.renderers.impl.BuiltRectangle;
import dev.meoray.client.util.render.renderers.impl.BuiltText;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.Map.entry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.joml.Matrix4f;
import org.lwjgl.glfw.GLFW;
import java.awt.Color;

public class MeoRayClickGUI extends Screen {

    private static final int W = 510;
    private static final int H = 340;
    private static final int BOTTOM_BAR = 66;

    private static final long OPEN_DURATION = 450;
    private static final long CLOSE_DURATION = 350;
    private static final long TOGGLE_DURATION = 300;
    private static final long EXPAND_DURATION = 250;

    // === DESIGN TOKENS ===
    // Module card
    private static final float CARD_RADIUS = 10f;
    private static final int CARD_BORDER_COL = 0x40FFFFFF; // rgba(255,255,255,0.25)
    private static final int CARD_HEADER_BG = 0xE61A1A1F;
    private static final int CARD_BODY_BG = 0xE6111116;
    private static final float HEADER_H = 30f;
    private static final float ACCENT_STRIPE_W = 3f;
    private static final float ACCENT_STRIPE_H = 12f;
    private static final float HEADER_PAD_X = 10f;
    private static final float HEADER_PAD_RIGHT = 8f;
    private static final float CARD_BORDER_THICKNESS = 0.014F;

    // Toggle (right side of header)
    private static final float TOGGLE_W = 22f;
    private static final float TOGGLE_H = 12f;
    private static final float TOGGLE_KNOB = 8f;
    private static final float TOGGLE_KNOB_PAD = 2f;
    private static final int TOGGLE_OFF_COL = 0xFF2A2A30;
    private static final int TOGGLE_BORDER_COL = 0x33FFFFFF;

    // Settings body
    private static final float SETTING_PAD = 8f;
    private static final float SETTING_GAP = 6f;
    private static final float SETTING_BTN_RADIUS = 6f;
    private static final float SETTING_BTN_H = 20f;
    private static final int SETTING_BTN_BG = 0xFF2A2A2A;
    private static final int SETTING_BTN_BORDER_COL = 0x26FFFFFF; // rgba(255,255,255,0.15)
    private static final int SETTING_BTN_ACTIVE_COL = 0xFFAB47BC; // purple accent

    // Keybind picker
    private static final float KEYBIND_H = 20f;
    private static final float KEYBIND_RADIUS = 6f;
    private static final int KEYBIND_BG = 0xFF1A1A1A;
    private static final int KEYBIND_BORDER_COL = 0x33FFFFFF; // rgba(255,255,255,0.2)
    private static final int KEYBIND_BINDING_BORDER = 0xFFAB47BC;

    // Category hover animation
    private static final long CAT_HOVER_DUR_IN = 400;
    private static final long CAT_HOVER_DUR_OUT = 400;
    private static final int CAT_HOVER_TINT = 0x18FFFFFF;

    private static int persistedCategory = 0;
    private int selectedCategory;
    private int prevCategory;
    private boolean closing = false;
    private final AnimatedFloat openAnim = new AnimatedFloat(0f);
    private final AnimatedFloat catFadeIn = new AnimatedFloat(1f);
    private final AnimatedFloat catFadeOut = new AnimatedFloat(1f);
    private int catPhase = 0;
    private int pendingCategory = -1;
    private String keybindTarget;

    // GUI dragging state
    private static float guiOffsetXTarget = 0;
    private static float guiOffsetYTarget = 0;
    private static float guiOffsetX = 0;
    private static float guiOffsetY = 0;
    private boolean draggingGui = false;
    private float dragGuiOffX, dragGuiOffY;

    private static class Particle {
        float x, y, vx, vy, size, alpha, baseAlpha, speed;
        int type; // 0=snow, 1=star, 2=bubble
        float phase;
        float life = 0f;
        boolean dead;
    }

    private final java.util.List<Particle> bgParticles = new java.util.ArrayList<>();
    private boolean particlesInit;

    private boolean snowOn = MeoRayClient.snowOn;
    private boolean starsOn = MeoRayClient.starsOn;
    private boolean bubblesOn = MeoRayClient.bubblesOn;

    private boolean effectsDropdownOpen;

    private final AnimatedFloat effectsDropdownAnim = new AnimatedFloat(0f);
    private float effectsDropdownCompactWidth = 90f;
    private float mainScaleSetting = 1.00f;

    public float getMainScaleSetting() { return mainScaleSetting; }
    public void setMainScaleSetting(float v) { this.mainScaleSetting = Math.max(0.5f, Math.min(1.5f, v)); }
    public float getGuiOffsetX() { return guiOffsetX; }
    public float getGuiOffsetY() { return guiOffsetY; }
    public void setGuiOffsetX(float v) { this.guiOffsetXTarget = v; this.guiOffsetX = v; }
    public void setGuiOffsetY(float v) { this.guiOffsetYTarget = v; this.guiOffsetY = v; }
    private boolean draggingMainScale;
    private java.util.Random rand = new java.util.Random();

    private static class ModuleAnimState {
        final AnimatedFloat knob = new AnimatedFloat(0f);
        final AnimatedFloat highlight = new AnimatedFloat(0f);
        final AnimatedFloat expand = new AnimatedFloat(0f);
        final AnimatedFloat gearAngle = new AnimatedFloat(0f);
        final AnimatedFloat hoverOffset = new AnimatedFloat(0f);
        final Map<String, AnimatedFloat> settingAnims = new HashMap<>();
    }

    private final Map<String, ModuleAnimState> moduleAnims = new HashMap<>();
    private final Map<String, Boolean> expandedModules = new HashMap<>();
    private final Map<String, AnimatedFloat> moduleFadeAnims = new HashMap<>();
    private final Map<String, Float> moduleFadeLastTarget = new HashMap<>();
    private final Map<Integer, AnimatedFloat> catHoverAnims = new HashMap<>();
    private final Map<Integer, AnimatedFloat> catSelectAnims = new HashMap<>();
    private final Map<Integer, Float> catSelectLastTarget = new HashMap<>();
    private final Map<Integer, AnimatedFloat> themeHoverAnims = new HashMap<>();
    private AnimatedFloat sidebarBounceAnim = new AnimatedFloat(0f);
    private Module hoveredModule;
    private float hoveredModuleCardX;
    private float hoveredModuleCardY;
    private float hoveredModuleCardW;
    private final AnimatedFloat tooltipAnim = new AnimatedFloat(0f);
    private static final long TOOLTIP_DURATION = 200;

    private final SearchElement searchElement = new SearchElement();
    private final Map<String, Float> sliderAnimatedProgress = new HashMap<>();
    private final Map<String, AnimatedFloat> modeDropdownAnims = new HashMap<>();
    private final Map<String, Float> bodyScrollOffsets = new HashMap<>();
    private final Map<String, Float> bodyScrollMax = new HashMap<>();
    private String hoveredSliderKey;
    private String draggingSliderMod;
    private String draggingSliderName;
    private boolean draggingSlider;
    private String draggingBodyScrollMod;
    private float contentScroll = 0f;
    private boolean typeGridExpanded = true;
    private static final Map<String, String> RU_DESCRIPTIONS = Map.ofEntries(
        entry("AutoPotion", "Автоматическое использование зелий"),
        entry("Sprint", "Автоматический спринт"),
        entry("NoSlow", "Убирает замедление"),
        entry("NoFall", "Отключает урон от падения"),
        entry("TargetHUD", "Информация о цели"),
        entry("ChinaHat", "Китайская шляпа"),
        entry("Interface", "Управление интерфейсом")
    );

    public MeoRayClickGUI() {
        super(Text.literal("MeoRay"));
    }

    @Override
    protected void init() {
        closing = false;
        selectedCategory = persistedCategory;
        openAnim.snapTo(0f);
        openAnim.animate(1f, OPEN_DURATION, Easing.EASE_OUT_QUINT);
        particlesInit = false;
        super.init();
    }

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        float open = openAnim.update();

        // Smooth GUI dragging via lerp
        float lerpFactor = 0.35f;
        guiOffsetX += (guiOffsetXTarget - guiOffsetX) * lerpFactor;
        guiOffsetY += (guiOffsetYTarget - guiOffsetY) * lerpFactor;

        if (closing && openAnim.isFinished()) {
            MinecraftClient.getInstance().setScreen(null);
            return;
        }

        // Update draggable HUD positions while ClickGUI is open
        if (MeoRayClient.INSTANCE.getDraggableManager() != null) {
            MeoRayClient.INSTANCE.getDraggableManager().updatePositions(mx, my);
            MeoRayClient.INSTANCE.getDraggableManager().renderPanels(ctx);
        }

        int gxi = Math.round((width - W) / 2f + guiOffsetX);
        int gyi = Math.round((height - H) / 2f + guiOffsetY);
        // GUI renders 1:1 (no matrix scale), so local coords = screen coords
        float lx = (float) mx;
        float ly = (float) my;

        Matrix4f matrix = ctx.getMatrices().peek().getPositionMatrix();

        Theme theme = MeoRayClient.INSTANCE.getThemeManager().getRenderTheme();

        // === Category fade transition (fade out → swap → fade in) ===
        if (catPhase == 1) {
            // Fading out current content
            catFadeOut.update();
            if (catFadeOut.getValue() < 0.02f) {
                // Swap to pending category
                if (pendingCategory >= 0) {
                    selectedCategory = pendingCategory;
                    persistedCategory = pendingCategory;
                }
                pendingCategory = -1;
                catFadeIn.snapTo(0f);
                catFadeIn.animate(1f, 240, Easing.EASE_IN_OUT_CUBIC);
                catPhase = 2;
            }
        } else if (catPhase == 2) {
            // Fading in new content
            if (catFadeIn.update() > 0.98f) {
                catPhase = 0;
            }
        } else {
            catFadeOut.update();
            catFadeIn.update();
        }
        float contentAlpha;
        if (catPhase == 1) {
            contentAlpha = catFadeOut.getValue();
        } else if (catPhase == 2) {
            contentAlpha = catFadeIn.getValue();
        } else {
            contentAlpha = 1f;
        }

        try {
            // Apply blur overlay (like vanilla screens)
            applyBlur();
            int overlayBg = alphaBlend(0x000000, open, 128);
            ((BuiltRectangle) Builder.rectangle()
                .size(new SizeState(width, height))
                .color(new QuadColorState(overlayBg))
                .radius(new QuadRadiusState(0.0))
                .smoothness(1.15F)
                .build()).render(matrix, 0, 0);

            renderWindowBg(matrix, gxi, gyi, open, theme);
            renderBackgroundEffects(matrix, gxi, gyi);
            renderBottomBar(matrix, gxi, gyi, lx, ly, open, theme);
            renderContent(matrix, gxi, gyi, lx, ly, open, theme, delta, contentAlpha);

            drawTooltip(ctx, gxi, gyi, lx, ly, open, theme);
        } catch (Exception e) {
            // Prevent crash from toasts/overlays conflicting with custom rendering
            com.mojang.blaze3d.systems.RenderSystem.disableScissor();
            com.mojang.blaze3d.systems.RenderSystem.enableCull();
            com.mojang.blaze3d.systems.RenderSystem.depthMask(true);
            com.mojang.blaze3d.systems.RenderSystem.blendFunc(
                com.mojang.blaze3d.platform.GlStateManager.SrcFactor.SRC_ALPHA,
                com.mojang.blaze3d.platform.GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA);
            com.mojang.blaze3d.systems.RenderSystem.setShaderColor(1, 1, 1, 1);
        }
        ctx.getMatrices().pop();

        if (draggingSlider) {
            handleSliderDrag(lx, ly);
        }
    }

    private void renderWindowBg(Matrix4f matrix, int gx, int gy, float open, Theme theme) {
        int bg = alphaBlend(0x7A1A1A1A, open, 255);
        ((BuiltRectangle) Builder.rectangle()
            .size(new SizeState(W, H))
            .color(new QuadColorState(bg))
            .radius(new QuadRadiusState(11.0, 11.0, 11.0, 11.0))
            .smoothness(1.15F)
            .build()).render(matrix, gx, gy);

        int borderCol = (theme.border() & 0x00FFFFFF) | 0x60000000;
        borderCol = alphaBlend(borderCol, open, 255);
        ((BuiltBorder) Builder.border()
            .size(new SizeState(W, H))
            .color(new QuadColorState(borderCol))
            .radius(new QuadRadiusState(11.0, 11.0, 11.0, 11.0))
            .thickness(0.8f)
            .smoothness(0.65F, 0.65F)
            .build()).render(matrix, gx, gy);

        int accentGlow = (theme.accent() & 0x00FFFFFF) | 0xD0000000;
        accentGlow = alphaBlend(accentGlow, open, 255);
        GlowRenderer.drawGlow(matrix, gx, gy, W, H, accentGlow, 20f, 11.0f);

        // === Drag handle dots (top center) ===
        int handleCol = alphaBlend(0x30FFFFFF, open, 255);
        float handleY = gy + 5;
        float handleCX = gx + W / 2f;
        for (int i = -2; i <= 2; i++) {
            ((BuiltRectangle) Builder.rectangle()
                .size(new SizeState(2.5f, 2.5f))
                .color(new QuadColorState(handleCol))
                .radius(new QuadRadiusState(1.25f))
                .smoothness(1.15F)
                .build()).render(matrix, handleCX + i * 6f - 1.25f, handleY);
        }
    }

    private void renderWhiteOutline(Matrix4f matrix, int gx, int gy, float open) {
    }

    private void renderWindowOutline(Matrix4f matrix, int gx, int gy, float open, Theme theme) {
    }

    private void renderBottomBar(Matrix4f matrix, int gx, int gy, float lx, float ly, float open, Theme theme) {
        int barY = gy + H - BOTTOM_BAR;

        // === Floating panel container with cubes ===
        Category[] cats = Category.ALL;
        float iconSize = 14.0F;
        float cubeW = 30f;
        float cubeH = 30f;
        float gap = 4f;
        float panelPadX = 6f;
        float panelPadY = 5f;
        float totalCubesW = cats.length * cubeW + (cats.length - 1) * gap;
        float panelW = totalCubesW + panelPadX * 2;
        float panelH = cubeH + panelPadY * 2;
        float panelX = gx + (W - panelW) / 2f;
        float panelY = barY + (BOTTOM_BAR - panelH) / 2f - 5f;

        // === Blurred glass panel background ===
        var winBlur = MinecraftClient.getInstance().getWindow();
        RenderSystem.enableScissor(
            (int)(panelX * winBlur.getScaleFactor()),
            0,
            (int)(panelW * winBlur.getScaleFactor()),
            winBlur.getFramebufferHeight()
        );
        ((BuiltBlur) Builder.blur()
            .size(new SizeState(panelW, panelH))
            .radius(new QuadRadiusState(14.0))
            .color(new QuadColorState(new Color(255, 255, 255, (int)(22 * open))))
            .blurRadius(18f)
            .smoothness(1.15F)
            .build()).render(matrix, panelX, panelY);
        RenderSystem.disableScissor();

        // Glass tint overlay (very subtle dark, more translucent)
        int panelTint = (Math.round(14f * open) << 24) | (0x14 << 16) | (0x12 << 8) | 0x24;
        ((BuiltRectangle) Builder.rectangle()
            .size(new SizeState(panelW, panelH))
            .color(new QuadColorState(panelTint))
            .radius(new QuadRadiusState(14.0))
            .smoothness(1.15F)
            .build()).render(matrix, panelX, panelY);

        // Panel border (subtle white, soft)
        int panelBorder = (Math.round(40f * open) << 24) | 0xFFFFFF;
        ((BuiltBorder) Builder.border()
            .size(new SizeState(panelW, panelH))
            .color(new QuadColorState(panelBorder))
            .radius(new QuadRadiusState(14.0))
            .thickness(0.015F)
            .smoothness(0.65F, 0.65F)
            .build()).render(matrix, panelX, panelY);

        // Cubes inside the panel
        float startX = panelX + panelPadX;
        float cubeY = panelY + panelPadY;

        for (int i = 0; i < cats.length; i++) {
            float bx = startX + i * (cubeW + gap);
            boolean hover = lx >= bx && lx <= bx + cubeW
                && ly >= cubeY && ly <= cubeY + cubeH;
            boolean sel = i == selectedCategory;

            AnimatedFloat ha = catHoverAnims.computeIfAbsent(i, k -> new AnimatedFloat(0f));
            float hoverTarget = hover ? 1f : 0f;
            if (Math.abs(ha.getValue() - hoverTarget) > 0.005f) {
                ha.animate(hoverTarget, hover ? CAT_HOVER_DUR_IN : CAT_HOVER_DUR_OUT, Easing.EASE_IN_OUT_CUBIC);
            }
            float hp = ha.update();

            AnimatedFloat sa = catSelectAnims.computeIfAbsent(i, k -> new AnimatedFloat(sel ? 1f : 0f));
            float selTarget = sel ? 1f : 0f;
            float lastTarget = catSelectLastTarget.getOrDefault(i, -999f);
            if (Math.abs(lastTarget - selTarget) > 0.001f) {
                sa.animate(selTarget, 320, Easing.EASE_IN_OUT_CUBIC);
                catSelectLastTarget.put(i, selTarget);
            }
            float sp = sa.update();

            // === Soft diffuse white glow halo around every icon box (like reference) ===
            float glowAlpha = (0.08f + 0.10f * hp + 0.14f * sp) * open;
            int whiteGlow = (Math.round(glowAlpha * 255f) << 24) | 0xFFFFFF;
            GlowRenderer.drawGlow(matrix, bx - 3f, cubeY - 3f, cubeW + 6f, cubeH + 6f, whiteGlow, 16f, 9.0f);

            // Cube background: dark glass base (translucent)
            int darkR = 0x18, darkG = 0x14, darkB = 0x22;
            float darkA = 0.42f + 0.20f * hp;
            int baseBg = (Math.round(darkA * 255f * open) << 24) | (darkR << 16) | (darkG << 8) | darkB;
            baseBg = alphaBlend(baseBg, open, 255);
            ((BuiltRectangle) Builder.rectangle()
                .size(new SizeState(cubeW, cubeH))
                .color(new QuadColorState(baseBg))
                .radius(new QuadRadiusState(9.0))
                .smoothness(1.15F)
                .build()).render(matrix, bx, cubeY);

            // Glass reflection: white gradient top-to-transparent bottom
            int reflectA = Math.round((50 + 30 * hp + 60 * sp) * open);
            int reflectTop = Math.min(255, reflectA) << 24 | 0xFFFFFF;
            int reflectBot = 0x00FFFFFF;
            ((BuiltRectangle) Builder.rectangle()
                .size(new SizeState(cubeW, cubeH * 0.55f))
                .color(new QuadColorState(reflectTop, reflectBot, reflectBot, reflectTop))
                .radius(new QuadRadiusState(9.0))
                .smoothness(1.15F)
                .build()).render(matrix, bx, cubeY);

            // Subtle white border around cube
            float borderA = (0.12f + 0.10f * hp + 0.18f * sp) * open;
            int cubeBorder = (Math.round(borderA * 255f) << 24) | 0xFFFFFF;
            ((BuiltBorder) Builder.border()
                .size(new SizeState(cubeW, cubeH))
                .color(new QuadColorState(cubeBorder))
                .radius(new QuadRadiusState(9.0))
                .thickness(0.014F)
                .smoothness(0.65F, 0.65F)
                .build()).render(matrix, bx, cubeY);

            // Icon — animates from dim to white as selection fills
            int iconDim = 0xFFD0D0E0;
            int iconBright = 0xFFFFFFFF;
            int iconColor = lerpColor(iconDim, iconBright, Math.max(hp, sp));
            iconColor = alphaBlend(iconColor, open, 255);

            String icon = cats[i].getIcon();
            MsdfFont iconFont = FontManager.getFont(cats[i].getFont());
            var iconMetrics = iconFont.getMetrics();
            float baseline = iconMetrics.baselineHeight() * iconSize;
            float ascender = iconMetrics.ascender() * iconSize;
            float visibleH = ascender;
            float textW = iconFont.getWidth(icon, iconSize);
            float iconX = bx + (cubeW - textW) / 2f;
            float iconY = cubeY + (cubeH - visibleH) / 2f - (baseline - ascender);

            ((BuiltText) Builder.text()
                .font(iconFont).text(icon)
                .color(iconColor).size(iconSize).thickness(0.05F)
                .build()).render(matrix, iconX, iconY);
        }
    }

    private void renderContent(Matrix4f matrix, int gx, int gy, float lx, float ly, float open, Theme theme, float delta, float contentAlpha) {
        int cx = gx + 14;
        int cy = gy + 14;
        int cw = W - 28;
        int ch = H - 28 - BOTTOM_BAR;

        Category cat = Category.ALL[selectedCategory];

        int headerBg = alphaBlend(theme.bgMain(), open, 255);
        ((BuiltRectangle) Builder.rectangle()
            .size(new SizeState(cw, 16))
            .color(new QuadColorState(headerBg))
            .radius(new QuadRadiusState(4.0))
            .smoothness(1.15F)
            .build()).render(matrix, cx, cy - 2);

        int catColor = alphaBlend(theme.textPrimary(), open, 255);
        MsdfFont medium = FontManager.SUISSEINTMEDIUM.get();
        ((BuiltText) Builder.text()
            .font(medium).text(cat.getName())
            .color(catColor).size(7.0F).thickness(0.05F)
            .build()).render(matrix, cx + 6, cy + 1);

        int headerLine = (theme.accent() & 0x00FFFFFF) | 0x66000000;
        headerLine = alphaBlend(headerLine, open, 255);
        ((BuiltRectangle) Builder.rectangle()
            .size(new SizeState(12.0, 1.0))
            .color(new QuadColorState(headerLine))
            .radius(new QuadRadiusState(0.0))
            .smoothness(1.15F)
            .build()).render(matrix, cx + 4, cy + 6);

        int headerBottom = cy + 18;

        // Search bar (below GUI, centered)
        int searchW = 150;
        int searchH = 14;
        int searchX = (int)(gx + W / 2f - searchW / 2f);
        int searchY = gy + H + 6;
        int searchBg = alphaBlend(0x33000000, open, 255);
        ((BuiltRectangle) Builder.rectangle()
            .size(new SizeState(searchW, searchH))
            .color(new QuadColorState(searchBg))
            .radius(new QuadRadiusState(3.0))
            .smoothness(1.15F)
            .build()).render(matrix, searchX, searchY);
        float searchGlow = searchElement.getFocusGlow();
        int searchBorderCol = lerpColor(
            (theme.border() & 0x00FFFFFF) | 0x44000000,
            (theme.accent() & 0x00FFFFFF) | 0xBB000000,
            searchGlow
        );
        searchBorderCol = alphaBlend(searchBorderCol, open, 255);
        float borderThick = 0.015f + 0.025f * searchGlow;
        ((BuiltBorder) Builder.border()
            .size(new SizeState(searchW, searchH))
            .color(new QuadColorState(searchBorderCol))
            .radius(new QuadRadiusState(3.0))
            .thickness(borderThick)
            .smoothness(0.65F, 0.65F)
            .build()).render(matrix, searchX, searchY);
        searchElement.setAnimation(open);
        searchElement.updateMouseHover(lx - searchX, ly - searchY, 0, 0);
        searchElement.render(matrix, searchX, searchY, delta);

        boolean hasSearch = !searchElement.getValue().isEmpty();

        boolean foundHover = false;
        float contentEnd = headerBottom;

        enableContentScissor(cx - 5, headerBottom - 5, cw + 10, gy + H - BOTTOM_BAR - headerBottom);
        RenderSystem.setShaderColor(1f, 1f, 1f, contentAlpha);

        if (cat == Category.THEMES) {
            renderThemesTab(matrix, cx + 5, headerBottom, cw - 10, ch - 20, lx, ly, open, contentScroll);
            hoveredModule = null;
            Theme[] themes = MeoRayClient.INSTANCE.getThemeManager().getThemes();
            int cols = 4, gapX = 8, gapY = 10, cardH = 100, cardW = (cw - (cols - 1) * gapX) / cols;
            int rows = (themes.length + cols - 1) / cols;
            contentEnd = headerBottom + rows * (cardH + gapY);
        } else if (cat == Category.SETTINGS) {
            renderSettingsTab(matrix, cx, headerBottom, cw, ch - 20, lx, ly, open, theme, contentScroll);
            hoveredModule = null;
            contentEnd = headerBottom + 200;
        } else {
            java.util.List<Module> modules = MeoRayClient.INSTANCE.moduleManager.getByCategory(cat);
            String searchText = searchElement.getValue();
            if (!searchText.isEmpty()) {
                String lower = searchText.toLowerCase();
                modules = modules.stream().filter(m -> m.getName().toLowerCase().contains(lower)).collect(java.util.stream.Collectors.toList());
            }

            if (!modules.isEmpty()) {
                for (Module mod : modules) {
                    ModuleAnimState st = moduleAnims.get(mod.getName());
                    if (st != null) {
                        st.knob.update(); st.highlight.update(); st.expand.update();
                        st.gearAngle.update();
                    }
                }

                int gap = 5, colW = (cw - gap) / 2;
                int[] moduleCol = new int[modules.size()];
                for (int i = 0; i < modules.size(); i++) moduleCol[i] = i % 2;

                float[] colYs = { headerBottom, headerBottom };

                for (int i = 0; i < modules.size(); i++) {
                    Module mod = modules.get(i);
                    int col = moduleCol[i];
                    float cardH = getCardAnimatedHeight(mod);
                    float cardX = cx + col * (colW + gap);
                    float cardY = colYs[col] - contentScroll;

                    ModuleAnimState st = getOrCreateState(mod);
                    st.hoverOffset.update();
                    float offsetY = st.hoverOffset.getValue();

                    // Per-module fade target based on catPhase
                    float fadeTarget;
                    if (catPhase == 1) {
                        fadeTarget = 0f;
                    } else if (catPhase == 2) {
                        // Stagger: later modules fade in slightly later
                        int idxDelay = Math.min(i, 6);
                        float delay = idxDelay * 30f;
                        fadeTarget = 1f;
                        AnimatedFloat ma = moduleFadeAnims.computeIfAbsent(mod.getName(), k -> new AnimatedFloat(0f));
                        float lastT = moduleFadeLastTarget.getOrDefault(mod.getName(), -999f);
                        if (Math.abs(lastT - 1f) > 0.001f) {
                            ma.animate(1f, 240, Easing.EASE_OUT_CUBIC);
                            moduleFadeLastTarget.put(mod.getName(), 1f);
                        }
                        float maV = ma.getValue();
                        if (i > 0) maV = Math.max(0f, maV - delay / 240f);
                        float visualY = cardY + offsetY + (1f - maV) * 8f;
                        st.hoverOffset.snapTo(0f);
                        st.hoverOffset.animate(0f, 1, Easing.LINEAR);
                        boolean hover = (int) lx >= cardX && (int) lx <= cardX + colW
                            && (int) ly >= visualY && (int) ly <= visualY + cardH;
                        if (hover) { hoveredModule = mod; hoveredModuleCardX = cardX; hoveredModuleCardY = visualY; hoveredModuleCardW = colW; foundHover = true; }
                        RenderSystem.setShaderColor(1f, 1f, 1f, maV * open);
                        renderCard(matrix, cardX, visualY, colW, cardH, mod, hover, open, theme, lx, ly);
                        colYs[col] = colYs[col] + cardH + gap;
                        continue;
                    } else {
                        fadeTarget = 1f;
                    }

                    // For catPhase 0 and 1: compute fade anim normally
                    AnimatedFloat ma = moduleFadeAnims.computeIfAbsent(mod.getName(), k -> new AnimatedFloat(1f));
                    float lastT = moduleFadeLastTarget.getOrDefault(mod.getName(), -999f);
                    if (Math.abs(lastT - fadeTarget) > 0.001f) {
                        ma.animate(fadeTarget, 180, Easing.EASE_IN_OUT_CUBIC);
                        moduleFadeLastTarget.put(mod.getName(), fadeTarget);
                    }
                    float maV = ma.getValue();
                    float visualY = cardY + offsetY + (1f - maV) * 6f;
                    boolean hover = (int) lx >= cardX && (int) lx <= cardX + colW
                        && (int) ly >= visualY && (int) ly <= visualY + cardH;
                    if (hover) { hoveredModule = mod; hoveredModuleCardX = cardX; hoveredModuleCardY = visualY; hoveredModuleCardW = colW; foundHover = true; }

                    float targetOff = hover ? -1f : 0f;
                    if (Math.abs(st.hoverOffset.getValue() - targetOff) > 0.005f)
                        st.hoverOffset.animate(targetOff, 120, Easing.EASE_OUT_CUBIC);

                    RenderSystem.setShaderColor(1f, 1f, 1f, maV * open);
                    renderCard(matrix, cardX, visualY, colW, cardH, mod, hover, open, theme, lx, ly);
                    RenderSystem.setShaderColor(1f, 1f, 1f, contentAlpha);
        enableContentScissor(cx - 5, headerBottom - 5, cw + 10, gy + H - BOTTOM_BAR - headerBottom);
                    colYs[col] = colYs[col] + cardH + gap;
                }
                contentEnd = Math.max(colYs[0], colYs[1]);
            }

            if (modules.isEmpty()) {
                String empty = "No modules";
                float textW = medium.getWidth(empty, 7.0F);
                int emptyColor = alphaBlend(theme.textSecondary(), open, 255);
                ((BuiltText) Builder.text().font(medium).text(empty).color(emptyColor).size(7.0F).thickness(0.05F).build()).render(matrix, cx + (cw - textW) / 2, cy + ch / 2);
            }
        }

        if (!foundHover) hoveredModule = null;
        RenderSystem.disableScissor();
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);

        float contentH = contentEnd - headerBottom;
        float visibleH = gy + H - 8 - headerBottom;
        float maxScroll = Math.max(0, contentH - visibleH);
        // Bottom overscroll allowed (elastic), top clamped to 0 (default position)
        float bottomOverscroll = 70f;
        float softMin = 0f;
        float softMax = maxScroll + bottomOverscroll;
        float excess = contentScroll < softMin ? contentScroll - softMin
                    : contentScroll > softMax ? contentScroll - softMax
                    : 0;
        if (Math.abs(excess) > 0.5f) {
            contentScroll -= excess * 0.06f;
        } else if (Math.abs(excess) > 0.01f) {
            contentScroll = Math.max(softMin, Math.min(contentScroll, softMax));
        }

        if (maxScroll > 1) {
            float sbX = cx + cw - 6;
            float sbMaxH = gy + H - BOTTOM_BAR - headerBottom - 5;
            float sbTrackH = Math.min(ch - 20, sbMaxH);
            float sbThumbH = Math.max(12, Math.min(sbTrackH, sbTrackH * (visibleH / contentH)));
            float scrollRange = (sbTrackH - sbThumbH);
            float denom = maxScroll > 0 ? maxScroll : 1;
            float sbThumbY = headerBottom + (contentScroll / denom) * scrollRange;
            sbThumbY = Math.max(headerBottom, Math.min(sbThumbY, headerBottom + scrollRange));

            RenderSystem.enableScissor((int)(sbX - 1), 0, 8, (int)(gy + H - BOTTOM_BAR));
            int trackCol = alphaBlend(0x30FFFFFF, open, 255);
            ((BuiltRectangle) Builder.rectangle().size(new SizeState(3.0, sbTrackH)).color(new QuadColorState(trackCol)).radius(new QuadRadiusState(1.5)).smoothness(1.15F).build()).render(matrix, sbX, headerBottom);

            int thumbCol = alphaBlend(theme.accent(), open, 200);
            ((BuiltRectangle) Builder.rectangle().size(new SizeState(3.0, sbThumbH)).color(new QuadColorState(thumbCol)).radius(new QuadRadiusState(1.5)).smoothness(1.15F).build()).render(matrix, sbX, sbThumbY);
            RenderSystem.disableScissor();
        }
    }

    private void renderCard(Matrix4f matrix, float x, float y, float w, float h, Module mod, boolean hover, float open, Theme theme, float lx, float ly) {
        ModuleAnimState state = getOrCreateState(mod);
        float highlight = state.highlight.getValue();
        float knobPos = state.knob.getValue();
        float expand = state.expand.getValue();

        float bodyH = h - HEADER_H;

        // === Card background (full) ===
        int cardBg = alphaBlend(CARD_HEADER_BG, open, 255);
        ((BuiltRectangle) Builder.rectangle()
            .size(new SizeState(w, h))
            .color(new QuadColorState(cardBg))
            .radius(new QuadRadiusState(CARD_RADIUS))
            .smoothness(1.15F)
            .build()).render(matrix, x, y);

        // === Body region (slightly darker, smooth reveal via expand) ===
        if (expand > 0.005f && bodyH > 0.5f) {
            int bodyBg = alphaBlend(CARD_BODY_BG, open, (int)(255 * expand));
            ((BuiltRectangle) Builder.rectangle()
                .size(new SizeState(w, bodyH))
                .color(new QuadColorState(bodyBg))
                .radius(new QuadRadiusState(0.0, CARD_RADIUS, CARD_RADIUS, 0.0))
                .smoothness(1.15F)
                .build()).render(matrix, x, y + HEADER_H);
        }

        // === Accent stripe (left side, only when enabled) ===
        if (highlight > 0.01f) {
            int stripeCol = alphaBlend(theme.accent(), open, 220);
            float stripeY = y + (HEADER_H - ACCENT_STRIPE_H) / 2f;
            float stripeX = x + 10;
            ((BuiltRectangle) Builder.rectangle()
                .size(new SizeState(ACCENT_STRIPE_W, ACCENT_STRIPE_H))
                .color(new QuadColorState(stripeCol))
                .radius(new QuadRadiusState(1.5F))
                .smoothness(1.15F)
                .build()).render(matrix, stripeX, stripeY);
        }

        // === Header text (module name) ===
        MsdfFont medium = FontManager.SUISSEINTMEDIUM.get();
        int textC = alphaBlend(theme.textPrimary(), open, 255);
        // Stripe right before text, no extra padding
        float nameX = x + 10 + (highlight > 0.01f ? ACCENT_STRIPE_W + 5 : 0);
        float nameY = y + (HEADER_H - 8.0F) / 2f - 0.5f;
        ((BuiltText) Builder.text()
            .font(medium).text(mod.getName())
            .color(textC).size(8.0F).thickness(0.05F)
            .build()).render(matrix, nameX, nameY);

        // === Toggle (right side, vertically centered) ===
        float tx = x + w - HEADER_PAD_RIGHT - TOGGLE_W;
        float ty = y + (HEADER_H - TOGGLE_H) / 2f;

        int toggleTrack = lerpColor(TOGGLE_OFF_COL, theme.accent() & 0x00FFFFFF | 0xE0000000, knobPos);
        toggleTrack = alphaBlend(toggleTrack, open, 255);
        ((BuiltRectangle) Builder.rectangle()
            .size(new SizeState(TOGGLE_W, TOGGLE_H))
            .color(new QuadColorState(toggleTrack))
            .radius(new QuadRadiusState(TOGGLE_H / 2f))
            .smoothness(1.15F)
            .build()).render(matrix, tx, ty);

        int toggleBorderCol = alphaBlend(TOGGLE_BORDER_COL, open, 255);
        ((BuiltBorder) Builder.border()
            .size(new SizeState(TOGGLE_W, TOGGLE_H))
            .color(new QuadColorState(toggleBorderCol))
            .radius(new QuadRadiusState(TOGGLE_H / 2f))
            .thickness(0.02F)
            .smoothness(0.65F, 0.65F)
            .build()).render(matrix, tx, ty);

        float kx = tx + TOGGLE_KNOB_PAD + (TOGGLE_W - TOGGLE_KNOB - TOGGLE_KNOB_PAD * 2) * knobPos;
        float ky = ty + (TOGGLE_H - TOGGLE_KNOB) / 2f;
        int knobCol = alphaBlend(0xFFFFFFFF, open, 255);
        ((BuiltRectangle) Builder.rectangle()
            .size(new SizeState(TOGGLE_KNOB, TOGGLE_KNOB))
            .color(new QuadColorState(knobCol))
            .radius(new QuadRadiusState(TOGGLE_KNOB / 2f))
            .smoothness(1.15F)
            .build()).render(matrix, kx, ky);

        // === Drag/gear icon (left of toggle) ===
        float gearS = 8.0F;
        float gearX = tx - 4 - gearS;
        float gearY = y + (HEADER_H - gearS) / 2f;
        boolean isExpanded = expandedModules.getOrDefault(mod.getName(), false);
        int gearColor = isExpanded || expand > 0.5f
            ? alphaBlend(theme.accent(), open, 255)
            : alphaBlend(theme.textPrimary(), open, 180);

        Matrix4f gearMatrix = new Matrix4f(matrix);
        float gcx = gearX + gearS / 2f;
        float gcy = gearY + gearS / 2f;
        gearMatrix.translate(gcx, gcy, 0);
        gearMatrix.rotate(state.gearAngle.getValue(), 0, 0, 1);
        gearMatrix.translate(-gcx, -gcy, 0);
        ((BuiltText) Builder.text()
            .font(FontManager.ICONS.get()).text("F")
            .color(gearColor).size(gearS).thickness(0.05F)
            .build()).render(gearMatrix, gearX, gearY);

        // === Single border around whole card (drawn last so it sits on top) ===
        int borderCol = alphaBlend(CARD_BORDER_COL, open, 255);
        ((BuiltBorder) Builder.border()
            .size(new SizeState(w, h))
            .color(new QuadColorState(borderCol))
            .radius(new QuadRadiusState(CARD_RADIUS))
            .smoothness(0.65F, 0.65F)
            .thickness(CARD_BORDER_THICKNESS)
            .build()).render(matrix, x, y);

        // === Settings body content (clipped to body region for smooth close animation) ===
        if (expand > 0.005f && bodyH > 0.5f) {
            float setsY = y + HEADER_H;
            float setsH = bodyH;
            // Calculate content height for scroll
            float contentH = computeContentHeight(mod);
            float maxScroll = Math.max(0f, contentH - setsH);
            float scroll = bodyScrollOffsets.getOrDefault(mod.getName(), 0f);
            if (scroll > maxScroll) scroll = maxScroll;
            if (scroll < 0) scroll = 0;
            bodyScrollOffsets.put(mod.getName(), scroll);
            bodyScrollMax.put(mod.getName(), maxScroll);
            // Clip body content to its visible region
            enableBodyScissor(x, setsY, w, setsH);
            renderSettingsContent(matrix, x, setsY, w, setsH, mod, expand, open, theme, lx, ly, scroll);
            RenderSystem.disableScissor();
            // Render scrollbar
            if (maxScroll > 1f) {
                renderScrollbar(matrix, x + w - 3f, setsY + 2, 2f, setsH - 4, scroll, maxScroll, theme, open);
            }
        }
    }

    private float computeContentHeight(Module mod) {
        List<Setting<?>> settings = mod.getSettings();
        if (settings.isEmpty()) return 0f;
        float h = SETTING_PAD;
        h += KEYBIND_H + SETTING_GAP;
        int boolCount = 0, numCount = 0, modeCount = 0;
        for (Setting<?> s : settings) {
            if (mod.getName().equals("Particles") && s instanceof BooleanSetting && s.getName().startsWith("Type ")) continue;
            if (s instanceof NumberSetting) numCount++;
            else if (s instanceof BooleanSetting) boolCount++;
            else if (s instanceof ModeSetting) modeCount++;
        }
        int boolRows = (boolCount + 1) / 2;
        h += boolRows * (SETTING_BTN_H + SETTING_GAP);
        h += numCount * (26f + SETTING_GAP);
        h += modeCount * (SETTING_BTN_H + SETTING_GAP);

        int typeCount = 0;
        for (Setting<?> s : settings) {
            if (mod.getName().equals("Particles") && s instanceof BooleanSetting && s.getName().startsWith("Type ")) typeCount++;
        }
        if (typeCount > 0) {
            int cols = 3;
            float cellH = SETTING_BTN_H;
            int rows = (typeCount + cols - 1) / cols;
            h += 2 + SETTING_BTN_H + SETTING_GAP + rows * (cellH + SETTING_GAP) + 2;
        }
        h += SETTING_PAD;
        return h;
    }

    private void renderScrollbar(Matrix4f matrix, float x, float y, float w, float h, float scroll, float maxScroll, Theme theme, float open) {
        float trackH = h;
        int trackC = (theme.border() & 0x00FFFFFF) | 0x30000000;
        trackC = alphaBlend(trackC, open, 255);
        ((BuiltRectangle) Builder.rectangle()
            .size(new SizeState(w, trackH))
            .color(new QuadColorState(trackC))
            .radius(new QuadRadiusState(w / 2f))
            .smoothness(1.15F)
            .build()).render(matrix, x, y);
        float thumbH = Math.max(12f, trackH * (trackH / (trackH + maxScroll)));
        float thumbY = y + (trackH - thumbH) * (scroll / maxScroll);
        int thumbC = alphaBlend(theme.accent(), open, 200);
        ((BuiltRectangle) Builder.rectangle()
            .size(new SizeState(w, thumbH))
            .color(new QuadColorState(thumbC))
            .radius(new QuadRadiusState(w / 2f))
            .smoothness(1.15F)
            .build()).render(matrix, x, thumbY);
    }

    private void enableBodyScissor(float bx, float by, float bw, float bh) {
        var window = MinecraftClient.getInstance().getWindow();
        double sf = window.getScaleFactor();
        int fbH = window.getFramebufferHeight();
        int scX = (int) (bx * sf);
        int scY = fbH - (int) ((by + bh) * sf);
        int scW = Math.max(0, (int) (bw * sf));
        int scH = Math.max(0, (int) (bh * sf));
        RenderSystem.enableScissor(scX, scY, scW, scH);
    }

    private void enableWindowScissor(int gx, int gy) {
        var window = MinecraftClient.getInstance().getWindow();
        double sf = window.getScaleFactor();
        int fbH = window.getFramebufferHeight();
        int scX = (int) (gx * sf);
        int scY = fbH - (int) ((gy + H) * sf);
        int scW = Math.max(0, (int) (W * sf));
        int scH = Math.max(0, (int) (H * sf));
        RenderSystem.enableScissor(scX, scY, scW, scH);
    }

    private void enableContentScissor(float cx, float cy, float cw, float ch) {
        var window = MinecraftClient.getInstance().getWindow();
        double sf = window.getScaleFactor();
        int fbH = window.getFramebufferHeight();
        int scX = (int) (cx * sf);
        int scY = fbH - (int) ((cy + ch) * sf);
        int scW = Math.max(0, (int) (cw * sf));
        int scH = Math.max(0, (int) (ch * sf));
        RenderSystem.enableScissor(scX, scY, scW, scH);
    }

    private void renderSettingsContent(Matrix4f matrix, float sx, float sy, float sw, float sh, Module mod, float expand, float open, Theme theme, float lx, float ly, float scroll) {
        List<Setting<?>> settings = mod.getSettings();
        if (settings.isEmpty()) return;

        boolean locked = !mod.isEnabled();
        float lockAlpha = locked ? 0.275f : 1f;

        boolean isParticles = mod.getName().equals("Particles");
        java.util.List<BooleanSetting> typeSettings = new java.util.ArrayList<>();
        java.util.List<BooleanSetting> bools = new java.util.ArrayList<>();
        java.util.List<NumberSetting> nums = new java.util.ArrayList<>();
        java.util.List<ModeSetting> modes = new java.util.ArrayList<>();

        for (Setting<?> s : settings) {
            if (s instanceof BooleanSetting bs) {
                if (isParticles && bs.getName().startsWith("Type ")) {
                    typeSettings.add(bs);
                } else {
                    bools.add(bs);
                }
            } else if (s instanceof NumberSetting ns) {
                nums.add(ns);
            } else if (s instanceof ModeSetting ms) {
                modes.add(ms);
            }
        }

        MsdfFont medium = FontManager.SUISSEINTMEDIUM.get();
        float settingX = sx + SETTING_PAD;
        float settingW = sw - SETTING_PAD * 2f;
        float curY = sy + SETTING_PAD - scroll;
        float visibleStart = sy;
        float visibleEnd = sy + sh;

        // === Keybind picker (visually distinct, [ K ] format) ===
        boolean binding = keybindTarget != null && keybindTarget.equals(mod.getName());
        String keyName = mod.getKey() == 0 ? "NONE" : getKeyName(mod.getKey());
        String keyText = "[ " + (binding ? "..." : keyName) + " ]";
        float keyW = settingW;
        int keyBgCol = alphaBlend(KEYBIND_BG, open, (int)(255 * lockAlpha));
        ((BuiltRectangle) Builder.rectangle()
            .size(new SizeState(keyW, KEYBIND_H))
            .color(new QuadColorState(keyBgCol))
            .radius(new QuadRadiusState(KEYBIND_RADIUS))
            .smoothness(1.15F)
            .build()).render(matrix, settingX, curY);

        int keyBorderCol = alphaBlend(binding ? KEYBIND_BINDING_BORDER : KEYBIND_BORDER_COL, open, (int)(255 * lockAlpha));
        ((BuiltBorder) Builder.border()
            .size(new SizeState(keyW, KEYBIND_H))
            .color(new QuadColorState(keyBorderCol))
            .radius(new QuadRadiusState(KEYBIND_RADIUS))
            .thickness(binding ? 0.045F : 0.022F)
            .smoothness(0.65F, 0.65F)
            .build()).render(matrix, settingX, curY);

        int keyTxtCol = alphaBlend(binding ? theme.accent() : theme.textPrimary(), open, (int)(220 * lockAlpha));
        ((BuiltText) Builder.text()
            .font(medium).text(keyText)
            .color(keyTxtCol).size(6.0F).thickness(0.04F)
            .build()).render(matrix, settingX + keyW / 2f - medium.getWidth(keyText, 6.0F) / 2f, curY + (KEYBIND_H - 6.0F) / 2f + 0.5f);

        curY += KEYBIND_H + SETTING_GAP;

        // === Booleans: 2-column grid ===
        float boolCellW = (settingW - SETTING_GAP) / 2f;
        for (int i = 0; i < bools.size(); i += 2) {
            if (curY + SETTING_BTN_H > visibleEnd) break;
            renderBooleanCell(matrix, settingX, curY, boolCellW, mod, bools.get(i), lockAlpha, open, theme, medium, lx, ly);
            if (i + 1 < bools.size()) {
                renderBooleanCell(matrix, settingX + boolCellW + SETTING_GAP, curY, boolCellW, mod, bools.get(i + 1), lockAlpha, open, theme, medium, lx, ly);
            }
            curY += SETTING_BTN_H + SETTING_GAP;
        }

        // === Sliders: full width ===
        float sliderH = 26f;
        for (NumberSetting ns : nums) {
            if (curY + sliderH > visibleEnd) break;
            renderSliderCell(matrix, settingX, curY, settingW, mod, ns, lockAlpha, open, theme, medium, lx, ly);
            curY += sliderH + SETTING_GAP;
        }

        // === Modes: row of buttons or dropdown ===
        for (ModeSetting ms : modes) {
            float modeRowH = SETTING_BTN_H;
            if (curY + modeRowH > visibleEnd) break;
            renderModeCell(matrix, settingX, curY, settingW, mod, ms, lockAlpha, open, theme, medium, lx, ly);
            curY += modeRowH + SETTING_GAP;
        }

        // === Particles type grid (special) ===
        if (!typeSettings.isEmpty()) {
            float headerH = SETTING_BTN_H;
            curY += 2;
            int headerBgC = alphaBlend((theme.accent() & 0x00FFFFFF) | 0x20000000, open, 255);
            ((BuiltRectangle) Builder.rectangle()
                .size(new SizeState(settingW, headerH))
                .color(new QuadColorState(headerBgC))
                .radius(new QuadRadiusState(SETTING_BTN_RADIUS))
                .smoothness(1.15F)
                .build()).render(matrix, settingX, curY);
            int headerBorderC = alphaBlend(SETTING_BTN_BORDER_COL, open, 160);
            ((BuiltBorder) Builder.border()
                .size(new SizeState(settingW, headerH))
                .color(new QuadColorState(headerBorderC))
                .radius(new QuadRadiusState(SETTING_BTN_RADIUS))
                .thickness(0.022F)
                .smoothness(0.65F, 0.65F)
                .build()).render(matrix, settingX, curY);

            String arrow = typeGridExpanded ? "\u25BC" : "\u25B6";
            int arrowCol = alphaBlend(theme.accent(), open, 220);
            ((BuiltText) Builder.text()
                .font(medium).text(arrow)
                .color(arrowCol).size(6.0F).thickness(0.04F)
                .build()).render(matrix, settingX + 8, curY + (headerH - 6.0F) / 2f);

            int onCount = 0;
            for (BooleanSetting ts : typeSettings) if (ts.getValue()) onCount++;
            String headerText = "Particle Types (" + onCount + "/" + typeSettings.size() + ")";
            int headerTxtCol = alphaBlend(theme.textPrimary(), open, 200);
            ((BuiltText) Builder.text()
                .font(medium).text(headerText)
                .color(headerTxtCol).size(6.0F).thickness(0.04F)
                .build()).render(matrix, settingX + 22, curY + (headerH - 6.0F) / 2f);

            curY += headerH + SETTING_GAP;

            if (typeGridExpanded) {
                int cols = 3;
                float cellGap = SETTING_GAP;
                float cellW = (settingW - (cols - 1) * cellGap) / cols;
                float cellH = SETTING_BTN_H;
                for (int ti = 0; ti < typeSettings.size(); ti++) {
                    int tCol = ti % cols;
                    int tRow = ti / cols;
                    float cellX = settingX + tCol * (cellW + cellGap);
                    float cellY = curY + tRow * (cellH + cellGap);
                    if (cellY + cellH > visibleEnd) break;
                    renderTypeCell(matrix, cellX, cellY, cellW, cellH, mod, typeSettings.get(ti), lockAlpha, open, theme, medium, lx, ly);
                }
                int rows = (typeSettings.size() + cols - 1) / cols;
                curY += rows * (cellH + cellGap) + 2;
            }
        }
    }

    private void renderBooleanCell(Matrix4f matrix, float cellX, float cellY, float cellW, Module mod, BooleanSetting bs, float lockAlpha, float open, Theme theme, MsdfFont medium, float lx, float ly) {
        boolean val = bs.getValue();
        String sKey = bs.getName();
        ModuleAnimState mst = moduleAnims.get(mod.getName());
        if (mst != null && !mst.settingAnims.containsKey(sKey)) {
            mst.settingAnims.put(sKey, new AnimatedFloat(val ? 1f : 0f));
        }
        AnimatedFloat sAnim = mst != null ? mst.settingAnims.get(sKey) : null;
        float animVal = sAnim != null ? sAnim.update() : (val ? 1f : 0f);

        int bgCol = alphaBlend(SETTING_BTN_BG, open, (int)(255 * lockAlpha));
        ((BuiltRectangle) Builder.rectangle()
            .size(new SizeState(cellW, SETTING_BTN_H))
            .color(new QuadColorState(bgCol))
            .radius(new QuadRadiusState(SETTING_BTN_RADIUS))
            .smoothness(1.15F)
            .build()).render(matrix, cellX, cellY);

        int borderCol = alphaBlend(SETTING_BTN_BORDER_COL, open, (int)(180 * lockAlpha));
        ((BuiltBorder) Builder.border()
            .size(new SizeState(cellW, SETTING_BTN_H))
            .color(new QuadColorState(borderCol))
            .radius(new QuadRadiusState(SETTING_BTN_RADIUS))
            .thickness(0.022F)
            .smoothness(0.65F, 0.65F)
            .build()).render(matrix, cellX, cellY);

        // Text (left)
        int txtCol = alphaBlend(theme.textPrimary(), open, (int)(220 * lockAlpha));
        ((BuiltText) Builder.text()
            .font(medium).text(sKey)
            .color(txtCol).size(5.5F).thickness(0.04F)
            .build()).render(matrix, cellX + 8, cellY + (SETTING_BTN_H - 5.5F) / 2f);

        // Checkbox (right)
        float cbSize = 11f;
        float cbX = cellX + cellW - cbSize - 7;
        float cbY = cellY + (SETTING_BTN_H - cbSize) / 2f;
        int cbBgCol = val
            ? alphaBlend(SETTING_BTN_ACTIVE_COL, open, 255)
            : alphaBlend(0xFF1A1A1F, open, 255);
        ((BuiltRectangle) Builder.rectangle()
            .size(new SizeState(cbSize, cbSize))
            .color(new QuadColorState(cbBgCol))
            .radius(new QuadRadiusState(3f))
            .smoothness(1.15F)
            .build()).render(matrix, cbX, cbY);

        if (!val) {
            int cbBorderCol = alphaBlend(SETTING_BTN_BORDER_COL, open, 200);
            ((BuiltBorder) Builder.border()
                .size(new SizeState(cbSize, cbSize))
                .color(new QuadColorState(cbBorderCol))
                .radius(new QuadRadiusState(3f))
                .thickness(0.022F)
                .smoothness(0.65F, 0.65F)
                .build()).render(matrix, cbX, cbY);
        }

        if (animVal > 0.5f) {
            float checkSize = 5.5f * animVal;
            int checkCol = alphaBlend(0xFFFFFFFF, open, (int)(255 * animVal));
            ((BuiltText) Builder.text()
                .font(medium).text("\u2713")
                .color(checkCol).size(checkSize + 1.0f).thickness(0.06F)
                .build()).render(matrix, cbX + (cbSize - checkSize - 1.0f) / 2f - 0.5f, cbY + (cbSize - checkSize - 1.0f) / 2f - 0.5f);
        }
    }

    private void renderSliderCell(Matrix4f matrix, float cellX, float cellY, float cellW, Module mod, NumberSetting ns, float lockAlpha, float open, Theme theme, MsdfFont medium, float lx, float ly) {
        double val = ns.getValue();
        double min = ns.getMin();
        double max = ns.getMax();
        String name = ns.getName();
        float labelSize = 5.5F;
        String sliderKey = mod.getName() + "|" + name;

        float targetProgress = (float) Math.min(1, Math.max(0, (val - min) / (max - min)));
        float animProg = sliderAnimatedProgress.getOrDefault(sliderKey, targetProgress);
        animProg += (targetProgress - animProg) * 0.18f;
        sliderAnimatedProgress.put(sliderKey, animProg);

        boolean sliderHovered = lockAlpha >= 0.5f && hoveredSliderKey != null && hoveredSliderKey.equals(sliderKey);
        if (lockAlpha >= 0.5f && lx >= cellX && lx <= cellX + cellW && ly >= cellY && ly <= cellY + 26) {
            hoveredSliderKey = sliderKey;
        }

        // Label (left)
        int labelCol = alphaBlend(theme.textPrimary(), open, (int)(220 * lockAlpha));
        ((BuiltText) Builder.text()
            .font(medium).text(name)
            .color(labelCol).size(labelSize).thickness(0.04F)
            .build()).render(matrix, cellX, cellY);

        // Value (right)
        String valStr = ns.formatValue();
        float valW = medium.getWidth(valStr, labelSize);
        int valCol = alphaBlend(theme.accent(), open, (int)(220 * lockAlpha));
        ((BuiltText) Builder.text()
            .font(medium).text(valStr)
            .color(valCol).size(labelSize).thickness(0.04F)
            .build()).render(matrix, cellX + cellW - valW, cellY);

        // Track
        float trackH = 3.5f;
        float trackY = cellY + labelSize + 7;
        int trackBgCol = alphaBlend(0x26FFFFFF, open, (int)(255 * lockAlpha));
        ((BuiltRectangle) Builder.rectangle()
            .size(new SizeState(cellW, trackH))
            .color(new QuadColorState(trackBgCol))
            .radius(new QuadRadiusState(trackH / 2f))
            .smoothness(1.15F)
            .build()).render(matrix, cellX, trackY);

        // Active fill
        float fillW = cellW * animProg;
        if (fillW > 1) {
            int fillCol = alphaBlend(SETTING_BTN_ACTIVE_COL, open, (int)(255 * lockAlpha));
            ((BuiltRectangle) Builder.rectangle()
                .size(new SizeState(fillW, trackH))
                .color(new QuadColorState(fillCol))
                .radius(new QuadRadiusState(trackH / 2f))
                .smoothness(1.15F)
                .build()).render(matrix, cellX, trackY);
        }

        // Thumb
        float baseR = 5.5f;
        float hoverR = 6.8f;
        float targetR = sliderHovered ? hoverR : baseR;
        // Smooth thumb radius via sliderAnimatedProgress? Use simple approach
        float thumbR = baseR + (hoverR - baseR) * (sliderHovered ? 1f : 0f);
        float thumbCX = cellX + fillW;
        float thumbCY = trackY + trackH / 2f;

        // Subtle glow on hover
        if (sliderHovered) {
            int glowCol = (SETTING_BTN_ACTIVE_COL & 0x00FFFFFF) | 0x40000000;
            ((BuiltRectangle) Builder.rectangle()
                .size(new SizeState((thumbR + 2.5f) * 2, (thumbR + 2.5f) * 2))
                .color(new QuadColorState(glowCol))
                .radius(new QuadRadiusState(thumbR + 2.5f))
                .smoothness(1.15F)
                .build()).render(matrix, thumbCX - thumbR - 2.5f, thumbCY - thumbR - 2.5f);
        }

        int thumbCol = sliderHovered
            ? alphaBlend(SETTING_BTN_ACTIVE_COL, open, 255)
            : alphaBlend(0xFFFFFFFF, open, (int)(255 * lockAlpha));
        ((BuiltRectangle) Builder.rectangle()
            .size(new SizeState(thumbR * 2, thumbR * 2))
            .color(new QuadColorState(thumbCol))
            .radius(new QuadRadiusState(thumbR))
            .smoothness(1.15F)
            .build()).render(matrix, thumbCX - thumbR, thumbCY - thumbR);
    }

    private void renderModeCell(Matrix4f matrix, float cellX, float cellY, float cellW, Module mod, ModeSetting ms, float lockAlpha, float open, Theme theme, MsdfFont medium, float lx, float ly) {
        String current = ms.getValue();
        String[] allModes = ms.getModes();
        int n = allModes.length;

        // Try to fit all mode buttons in a row
        float modeGap = 4f;
        float modePad = 8f;
        float totalTextW = 0;
        for (String m : allModes) {
            totalTextW += medium.getWidth(m, 5.5F);
        }
        float totalBtnW = totalTextW + n * modePad * 2 + (n - 1) * modeGap;
        boolean asRow = totalBtnW <= cellW;

        if (asRow) {
            float curBx = cellX;
            float btnH = SETTING_BTN_H;
            for (int i = 0; i < n; i++) {
                String mode = allModes[i];
                float btnW = medium.getWidth(mode, 5.5F) + modePad * 2;
                boolean sel = mode.equals(current);
                int btnBg = sel
                    ? alphaBlend(SETTING_BTN_ACTIVE_COL, open, 255)
                    : alphaBlend(SETTING_BTN_BG, open, (int)(255 * lockAlpha));
                ((BuiltRectangle) Builder.rectangle()
                    .size(new SizeState(btnW, btnH))
                    .color(new QuadColorState(btnBg))
                    .radius(new QuadRadiusState(SETTING_BTN_RADIUS))
                    .smoothness(1.15F)
                    .build()).render(matrix, curBx, cellY);

                if (!sel) {
                    int btnBorderCol = alphaBlend(SETTING_BTN_BORDER_COL, open, (int)(180 * lockAlpha));
                    ((BuiltBorder) Builder.border()
                        .size(new SizeState(btnW, btnH))
                        .color(new QuadColorState(btnBorderCol))
                        .radius(new QuadRadiusState(SETTING_BTN_RADIUS))
                        .thickness(0.022F)
                        .smoothness(0.65F, 0.65F)
                        .build()).render(matrix, curBx, cellY);
                }

                int btnTxtCol = sel
                    ? alphaBlend(0xFFFFFFFF, open, 255)
                    : alphaBlend(theme.textPrimary(), open, (int)(220 * lockAlpha));
                ((BuiltText) Builder.text()
                    .font(medium).text(mode)
                    .color(btnTxtCol).size(5.5F).thickness(0.04F)
                    .build()).render(matrix, curBx + modePad, cellY + (btnH - 5.5F) / 2f);

                curBx += btnW + modeGap;
            }
        } else {
            // Dropdown fallback
            String ddKey = mod.getName() + "|" + ms.getName();
            if (!modeDropdownAnims.containsKey(ddKey)) {
                modeDropdownAnims.put(ddKey, new AnimatedFloat(0f));
            }
            AnimatedFloat ddAnim = modeDropdownAnims.get(ddKey);
            float ddProgress = ddAnim.update();

            int ddBgCol = alphaBlend(SETTING_BTN_BG, open, (int)(255 * lockAlpha));
            ((BuiltRectangle) Builder.rectangle()
                .size(new SizeState(cellW, SETTING_BTN_H))
                .color(new QuadColorState(ddBgCol))
                .radius(new QuadRadiusState(SETTING_BTN_RADIUS))
                .smoothness(1.15F)
                .build()).render(matrix, cellX, cellY);
            int ddBorderCol = alphaBlend(SETTING_BTN_BORDER_COL, open, (int)(180 * lockAlpha));
            ((BuiltBorder) Builder.border()
                .size(new SizeState(cellW, SETTING_BTN_H))
                .color(new QuadColorState(ddBorderCol))
                .radius(new QuadRadiusState(SETTING_BTN_RADIUS))
                .thickness(0.022F)
                .smoothness(0.65F, 0.65F)
                .build()).render(matrix, cellX, cellY);

            int modeCol = alphaBlend(theme.textPrimary(), open, (int)(220 * lockAlpha));
            ((BuiltText) Builder.text()
                .font(medium).text(current)
                .color(modeCol).size(5.5F).thickness(0.04F)
                .build()).render(matrix, cellX + 8, cellY + (SETTING_BTN_H - 5.5F) / 2f);

            String arrow = ddProgress > 0.5f ? "\u25B2" : "\u25BC";
            float arrowW = medium.getWidth(arrow, 5.0F);
            int arrCol = alphaBlend(0xFF888896, open, (int)(200 * lockAlpha));
            ((BuiltText) Builder.text()
                .font(medium).text(arrow)
                .color(arrCol).size(5.0F).thickness(0.04F)
                .build()).render(matrix, cellX + cellW - arrowW - 8, cellY + (SETTING_BTN_H - 5.0F) / 2f);

            float ddItemH = 14;
            float ddFullH = n * ddItemH;
            float ddH = ddFullH * ddProgress;
            if (ddH > 1) {
                float ddY = cellY + SETTING_BTN_H + 2;
                int ddBgC = alphaBlend(CARD_BODY_BG, open, 255);
                ((BuiltRectangle) Builder.rectangle()
                    .size(new SizeState(cellW, ddH))
                    .color(new QuadColorState(ddBgC))
                    .radius(new QuadRadiusState(SETTING_BTN_RADIUS))
                    .smoothness(1.15F)
                    .build()).render(matrix, cellX, ddY);
                int ddBorderC = alphaBlend(SETTING_BTN_BORDER_COL, open, 200);
                ((BuiltBorder) Builder.border()
                    .size(new SizeState(cellW, ddH))
                    .color(new QuadColorState(ddBorderC))
                    .radius(new QuadRadiusState(SETTING_BTN_RADIUS))
                    .thickness(0.022F)
                    .smoothness(0.65F, 0.65F)
                    .build()).render(matrix, cellX, ddY);

                for (int mi = 0; mi < n; mi++) {
                    float itemAlpha = (ddProgress * n - mi) / n * 2f;
                    itemAlpha = Math.max(0, Math.min(1, itemAlpha * 2f));
                    if (itemAlpha < 0.01f) continue;
                    float iy = ddY + mi * ddItemH;
                    if (iy - ddY > ddH) continue;
                    boolean selM = allModes[mi].equals(current);
                    if (selM) {
                        int itemBg = alphaBlend((theme.accent() & 0x00FFFFFF) | 0x40000000, open, 255);
                        double tlr = mi == 0 ? SETTING_BTN_RADIUS : 0.0;
                        double brr = mi == n - 1 ? SETTING_BTN_RADIUS : 0.0;
                        ((BuiltRectangle) Builder.rectangle()
                            .size(new SizeState(cellW, ddItemH))
                            .color(new QuadColorState(itemBg))
                            .radius(new QuadRadiusState(tlr, 0.0, 0.0, brr))
                            .smoothness(1.15F)
                            .build()).render(matrix, cellX, iy);
                    }
                    int itemAlphaI = (int)(255 * itemAlpha);
                    String check = selM ? "\u2713" : "  ";
                    float cw = medium.getWidth(check, 5.0F);
                    int cCol = selM ? (theme.accent() & 0x00FFFFFF) | (itemAlphaI << 24) : 0;
                    if (selM) {
                        ((BuiltText) Builder.text()
                            .font(medium).text(check)
                            .color(cCol).size(5.0F).thickness(0.04F)
                            .build()).render(matrix, cellX + 4, iy + (ddItemH - 5.0F) / 2);
                    }
                    float txOff = selM ? 4 + cw + 4 : 4;
                    int tCol = alphaBlend(selM ? 0xFFFFFFFF : 0xFF999999, open, (int)(255 * itemAlpha));
                    ((BuiltText) Builder.text()
                        .font(medium).text(allModes[mi])
                        .color(tCol).size(5.0F).thickness(0.04F)
                        .build()).render(matrix, cellX + txOff, iy + (ddItemH - 5.0F) / 2);
                }
            }
        }
    }

    private void renderTypeCell(Matrix4f matrix, float cellX, float cellY, float cellW, float cellH, Module mod, BooleanSetting ts, float lockAlpha, float open, Theme theme, MsdfFont medium, float lx, float ly) {
        boolean tVal = ts.getValue();
        String tLabel = ts.getName().substring(5);
        String sKey = ts.getName();
        ModuleAnimState mst = moduleAnims.get(mod.getName());
        if (mst != null && !mst.settingAnims.containsKey(sKey)) {
            mst.settingAnims.put(sKey, new AnimatedFloat(tVal ? 1f : 0f));
        }
        AnimatedFloat sAnim = mst != null ? mst.settingAnims.get(sKey) : null;
        float animVal = sAnim != null ? sAnim.update() : (tVal ? 1f : 0f);

        int bgCol = lerpColor(SETTING_BTN_BG, SETTING_BTN_ACTIVE_COL, animVal);
        bgCol = alphaBlend(bgCol, open, 255);
        ((BuiltRectangle) Builder.rectangle()
            .size(new SizeState(cellW, cellH))
            .color(new QuadColorState(bgCol))
            .radius(new QuadRadiusState(SETTING_BTN_RADIUS))
            .smoothness(1.15F)
            .build()).render(matrix, cellX, cellY);

        if (animVal < 0.5f) {
            int borderCol = alphaBlend(SETTING_BTN_BORDER_COL, open, 160);
            ((BuiltBorder) Builder.border()
                .size(new SizeState(cellW, cellH))
                .color(new QuadColorState(borderCol))
                .radius(new QuadRadiusState(SETTING_BTN_RADIUS))
                .thickness(0.022F)
                .smoothness(0.65F, 0.65F)
                .build()).render(matrix, cellX, cellY);
        }

        int txtCol = alphaBlend(theme.textPrimary(), open, (int)(220 * lockAlpha));
        ((BuiltText) Builder.text()
            .font(medium).text(tLabel)
            .color(txtCol).size(5.0F).thickness(0.04F)
            .build()).render(matrix, cellX + 8, cellY + (cellH - 5.0F) / 2f);

        if (animVal > 0.5f) {
            int checkCol = alphaBlend(0xFFFFFFFF, open, (int)(220 * animVal));
            ((BuiltText) Builder.text()
                .font(medium).text("\u2713")
                .color(checkCol).size(5.0F).thickness(0.04F)
                .build()).render(matrix, cellX + cellW - 12, cellY + (cellH - 5.0F) / 2f);
        }
    }

    private void renderThemesTab(Matrix4f matrix, int ox, int oy, int w, int h, float lx, float ly, float open, float scrollOffset) {
        ThemeManager mgr = MeoRayClient.INSTANCE.getThemeManager();
        Theme[] themes = mgr.getThemes();

        int cols = 4;
        int gapX = 8;
        int gapY = 10;
        int cardW = (w - (cols - 1) * gapX) / cols;
        int maxRows = (h - gapY) / 50;
        int rows = (themes.length + cols - 1) / cols;
        int cardH = rows <= maxRows ? 100 : Math.max(60, (h - (rows - 1) * gapY) / rows);
        int yEnd = oy + h;

        for (int i = 0; i < themes.length; i++) {
            int col = i % cols;
            int row = i / cols;
            int cx = ox + col * (cardW + gapX);
            int cy = (int)(oy + row * (cardH + gapY) - scrollOffset);

            if (cy + cardH < oy || cy > yEnd) continue;

            boolean hover = (int) lx >= cx && (int) lx <= cx + cardW
                && (int) ly >= cy && (int) ly <= cy + cardH;
            boolean sel = i == mgr.getSelectedIndex();

            themeHoverAnims.putIfAbsent(i, new AnimatedFloat(0f));
            AnimatedFloat hAnim = themeHoverAnims.get(i);
            hAnim.animate(hover || sel ? 1f : 0f, 200L, Easing.EASE_OUT_CUBIC);
            float hoverProgress = hAnim.update();

            drawThemeCard(matrix, cx, cy, cardW, cardH, themes[i], sel, hoverProgress, open);
        }
    }

    private void drawThemeCard(Matrix4f matrix, int x, int y, int w, int h, Theme theme, boolean selected, float hoverProgress, float open) {
        int bg = alphaBlend(0x640F0F14, open, 255);
        ((BuiltRectangle) Builder.rectangle()
            .size(new SizeState(w, h))
            .color(new QuadColorState(bg))
            .radius(new QuadRadiusState(6.0))
            .smoothness(1.15F)
            .build()).render(matrix, x, y);

        if (hoverProgress > 0.05f) {
            int innerGlow = (theme.accent() & 0x00FFFFFF) | ((int)(0x18 * hoverProgress) << 24);
            innerGlow = alphaBlend(innerGlow, open, 255);
            float inset = 2f * hoverProgress;
            ((BuiltRectangle) Builder.rectangle()
                .size(new SizeState(w - inset * 2, h - inset * 2))
                .color(new QuadColorState(innerGlow))
                .radius(new QuadRadiusState(5.0))
                .smoothness(1.15F)
                .build()).render(matrix, x + inset, y + inset);
        }

        int borderBase = alphaBlend(theme.border(), open, 180);
        int borderAccent = alphaBlend(theme.accent(), open, 255);
        int borderCol = selected ? borderAccent : lerpColor(borderBase, borderAccent, hoverProgress);
        float borderThick = selected ? 0.028f : 0.015f + 0.012f * hoverProgress;
        ((BuiltBorder) Builder.border()
            .size(new SizeState(w, h))
            .color(new QuadColorState(borderCol))
            .radius(new QuadRadiusState(6.0))
            .thickness(borderThick)
            .smoothness(0.65F, 0.65F)
            .build()).render(matrix, x, y);

        if (selected) {
            int outerGlow = (theme.accent() & 0x00FFFFFF) | 0xB0000000;
            outerGlow = alphaBlend(outerGlow, open, 255);
            GlowRenderer.drawGlow(matrix, x, y, w, h, outerGlow, 12f, 6.0f);
        }

        drawThemePreview(matrix, x + 5, y + 5, w - 10, 65, theme, open);

        MsdfFont medium = FontManager.SUISSEINTMEDIUM.get();
        int nameColor = alphaBlend(0xFFDDDDDD, open, 255);
        ((BuiltText) Builder.text()
            .font(medium).text(theme.name())
            .color(nameColor).size(5.5F).thickness(0.05F)
            .build()).render(matrix, x + 8, y + 75);

        float chkSize = 9;
        float chkX = x + w - chkSize - 9;
        float chkY = y + 73;

        if (selected) {
            int chkBg = alphaBlend(theme.accent(), open, 255);
            ((BuiltRectangle) Builder.rectangle()
                .size(new SizeState(chkSize, chkSize))
                .color(new QuadColorState(chkBg))
                .radius(new QuadRadiusState(chkSize / 2f))
                .smoothness(1.15F)
                .build()).render(matrix, chkX, chkY);
        } else if (hoverProgress > 0.05f) {
            int glowCol = (theme.accent() & 0x00FFFFFF) | ((int)(0x30 * hoverProgress) << 24);
            glowCol = alphaBlend(glowCol, open, 255);
            float glowExpand = 2f * hoverProgress;
            ((BuiltRectangle) Builder.rectangle()
                .size(new SizeState(chkSize + glowExpand * 2, chkSize + glowExpand * 2))
                .color(new QuadColorState(glowCol))
                .radius(new QuadRadiusState((chkSize + glowExpand * 2) / 2f))
                .smoothness(1.15F)
                .build()).render(matrix, chkX - glowExpand, chkY - glowExpand);
        }

        if (selected) {
            int chkTextColor = alphaBlend(0xFF111111, open, 255);
            ((BuiltText) Builder.text()
                .font(medium).text("\u2713")
                .color(chkTextColor).size(5.5F).thickness(0.04F)
                .build()).render(matrix, chkX + 1.5f, chkY + 1f);
        }
    }
    private void drawThemePreview(Matrix4f matrix, int x, int y, int w, int h, Theme theme, float open) {
        int accent = theme.accent();
        int textPrimary = theme.textPrimary();
        int textSecondary = theme.textSecondary();
        int border = theme.border();
        int bgMain = theme.bgMain();

        int panelBg = (bgMain & 0x00FFFFFF) | 0xCC000000;
        panelBg = alphaBlend(panelBg, open, 255);
        ((BuiltRectangle) Builder.rectangle()
            .size(new SizeState(w, h))
            .color(new QuadColorState(panelBg))
            .radius(new QuadRadiusState(4.0))
            .smoothness(1.15F)
            .build()).render(matrix, x, y);

        float lx = x + 8;
        float rx = x + w - 8;
        float topY = y + 8;

        int accentCol = alphaBlend(accent, open, 255);
        ((BuiltRectangle) Builder.rectangle()
            .size(new SizeState(18, 3))
            .color(new QuadColorState(accentCol))
            .radius(new QuadRadiusState(1.5))
            .smoothness(1.15F)
            .build()).render(matrix, lx, topY);
        ((BuiltRectangle) Builder.rectangle()
            .size(new SizeState(8, 3))
            .color(new QuadColorState(accentCol))
            .radius(new QuadRadiusState(1.5))
            .smoothness(1.15F)
            .build()).render(matrix, rx - 12, topY);

        int pillOnBg = (accent & 0x00FFFFFF) | 0xAA000000;
        pillOnBg = alphaBlend(pillOnBg, open, 255);
        ((BuiltRectangle) Builder.rectangle()
            .size(new SizeState(10, 4))
            .color(new QuadColorState(pillOnBg))
            .radius(new QuadRadiusState(2.0))
            .smoothness(1.15F)
            .build()).render(matrix, rx - 4, topY - 0.5f);
        int pillKnob = alphaBlend(0xFFFFFFFF, open, 255);
        ((BuiltRectangle) Builder.rectangle()
            .size(new SizeState(3, 3))
            .color(new QuadColorState(pillKnob))
            .radius(new QuadRadiusState(1.5))
            .smoothness(1.15F)
            .build()).render(matrix, rx + 1.5f, topY);

        float sliderY = topY + 10;
        int trackCol = (border & 0x00FFFFFF) | 0x80000000;
        trackCol = alphaBlend(trackCol, open, 255);
        ((BuiltRectangle) Builder.rectangle()
            .size(new SizeState(w - 20, 2))
            .color(new QuadColorState(trackCol))
            .radius(new QuadRadiusState(1.0))
            .smoothness(1.15F)
            .build()).render(matrix, lx, sliderY);
        float fillW = (w - 20) * 0.6f;
        int fillCol = alphaBlend(accent, open, 255);
        ((BuiltRectangle) Builder.rectangle()
            .size(new SizeState(fillW, 2))
            .color(new QuadColorState(fillCol))
            .radius(new QuadRadiusState(1.0))
            .smoothness(1.15F)
            .build()).render(matrix, lx, sliderY);
        ((BuiltRectangle) Builder.rectangle()
            .size(new SizeState(4, 4))
            .color(new QuadColorState(accentCol))
            .radius(new QuadRadiusState(2.0))
            .smoothness(1.15F)
            .build()).render(matrix, lx + fillW - 2, sliderY - 1);

        float textY = sliderY + 7;
        int textCol1 = alphaBlend(textPrimary, open, 160);
        ((BuiltRectangle) Builder.rectangle()
            .size(new SizeState(w - 20, 1.5f))
            .color(new QuadColorState(textCol1))
            .radius(new QuadRadiusState(0.75f))
            .smoothness(1.15F)
            .build()).render(matrix, lx, textY);
        int textCol2 = alphaBlend(textSecondary, open, 120);
        ((BuiltRectangle) Builder.rectangle()
            .size(new SizeState(w - 30, 1.5f))
            .color(new QuadColorState(textCol2))
            .radius(new QuadRadiusState(0.75f))
            .smoothness(1.15F)
            .build()).render(matrix, lx, textY + 5);
        ((BuiltRectangle) Builder.rectangle()
            .size(new SizeState(w - 25, 1.5f))
            .color(new QuadColorState(textCol2))
            .radius(new QuadRadiusState(0.75f))
            .smoothness(1.15F)
            .build()).render(matrix, lx, textY + 10);

        float row3Y = textY + 18;
        ((BuiltRectangle) Builder.rectangle()
            .size(new SizeState(16, 3))
            .color(new QuadColorState(accentCol))
            .radius(new QuadRadiusState(1.5))
            .smoothness(1.15F)
            .build()).render(matrix, lx, row3Y);
        ((BuiltRectangle) Builder.rectangle()
            .size(new SizeState(10, 3))
            .color(new QuadColorState(accentCol))
            .radius(new QuadRadiusState(1.5))
            .smoothness(1.15F)
            .build()).render(matrix, lx + 22, row3Y);

        float row4Y = row3Y + 8;
        ((BuiltRectangle) Builder.rectangle()
            .size(new SizeState(w - 20, 1.5f))
            .color(new QuadColorState(textCol1))
            .radius(new QuadRadiusState(0.75f))
            .smoothness(1.15F)
            .build()).render(matrix, lx, row4Y);
        ((BuiltRectangle) Builder.rectangle()
            .size(new SizeState(w - 35, 1.5f))
            .color(new QuadColorState(textCol2))
            .radius(new QuadRadiusState(0.75f))
            .smoothness(1.15F)
            .build()).render(matrix, lx, row4Y + 5);

        float row5Y = row4Y + 11;
        ((BuiltRectangle) Builder.rectangle()
            .size(new SizeState(20, 2))
            .color(new QuadColorState(trackCol))
            .radius(new QuadRadiusState(1.0))
            .smoothness(1.15F)
            .build()).render(matrix, lx, row5Y);
        float fill2W = 20 * 0.35f;
        int fillCol2 = alphaBlend(accent, open, 200);
        ((BuiltRectangle) Builder.rectangle()
            .size(new SizeState(fill2W, 2))
            .color(new QuadColorState(fillCol2))
            .radius(new QuadRadiusState(1.0))
            .smoothness(1.15F)
            .build()).render(matrix, lx, row5Y);
        ((BuiltRectangle) Builder.rectangle()
            .size(new SizeState(4, 4))
            .color(new QuadColorState(accentCol))
            .radius(new QuadRadiusState(2.0))
            .smoothness(1.15F)
            .build()).render(matrix, lx + fill2W - 2, row5Y - 1);
    }

    private boolean tooltipHoverPrev;

    private void drawTooltip(DrawContext ctx, float gx, float gy, float lx, float ly, float open, Theme theme) {
        boolean hasTarget = hoveredModule != null;
        if (hasTarget != tooltipHoverPrev) {
            tooltipHoverPrev = hasTarget;
            if (hasTarget) {
                tooltipAnim.animate(1f, TOOLTIP_DURATION, Easing.EASE_OUT_CUBIC);
            } else {
                tooltipAnim.animate(0f, TOOLTIP_DURATION, Easing.EASE_OUT_CUBIC);
            }
        }
        float tt = tooltipAnim.update();
        if (tt < 0.01f || hoveredModule == null) return;

        MsdfFont medium = FontManager.SUISSEINTMEDIUM.get();
        String desc = RU_DESCRIPTIONS.getOrDefault(hoveredModule.getName(), hoveredModule.getDescription());
        float textSize = 6.0F;
        float textWidth = medium.getWidth(desc, textSize);
        float tooltipWidth = textWidth + 20f;
        float tooltipHeight = textSize + 10f;

        float animAlpha = open * tt;

        float tooltipX = gx + W / 2f - tooltipWidth / 2f;
        float tooltipY = gy - tooltipHeight - 6f;

        float scale = 0.85f + 0.15f * tt;
        float sOffX = tooltipWidth * (1f - scale) / 2f;
        float sOffY = tooltipHeight * (1f - scale) / 2f;

        if (tt > 0.05f) {
            ctx.getMatrices().push();
            ctx.getMatrices().translate(tooltipX + tooltipWidth / 2f, tooltipY + tooltipHeight / 2f, 0);
            ctx.getMatrices().scale(scale, scale, 1);
            ctx.getMatrices().translate(-tooltipWidth / 2f, -tooltipHeight / 2f, 0);
            Matrix4f tMat = ctx.getMatrices().peek().getPositionMatrix();

            int ttBg = alphaBlend(0x880A0A0F, animAlpha, 255);
            ((BuiltRectangle) Builder.rectangle()
                .size(new SizeState(tooltipWidth, tooltipHeight))
                .color(new QuadColorState(ttBg))
                .radius(new QuadRadiusState(4.0))
                .smoothness(1.15F)
                .build()).render(tMat, 0, 0);

            int ttBorder = alphaBlend(theme.accent(), animAlpha, 60);
            ((BuiltBorder) Builder.border()
                .size(new SizeState(tooltipWidth, tooltipHeight))
                .color(new QuadColorState(ttBorder))
                .radius(new QuadRadiusState(4.0))
                .thickness(0.015F)
                .smoothness(0.65F, 0.65F)
                .build()).render(tMat, 0, 0);

            float textX = (tooltipWidth - textWidth) / 2f - 2f;
            float baselineH = medium.getMetrics().baselineHeight();
            float asc = medium.getMetrics().ascender();
            float textY = tooltipHeight / 2f - (baselineH - asc * 0.5f) * textSize - 1f;
            int ttText = alphaBlend(0xFFCCCCCC, animAlpha, 255);
            ((BuiltText) Builder.text()
                .font(medium).text(desc)
                .color(ttText).size(textSize).thickness(0.04F)
                .build()).render(tMat, textX, textY);

            ctx.getMatrices().pop();
        }
    }

    private ModuleAnimState getOrCreateState(Module mod) {
        return moduleAnims.computeIfAbsent(mod.getName(), k -> {
            ModuleAnimState s = new ModuleAnimState();
            boolean on = mod.isEnabled();
            s.knob.snapTo(on ? 1f : 0f);
            s.highlight.snapTo(on ? 1f : 0f);
            s.expand.snapTo(0f);
            return s;
        });
    }

    private void startToggleAnim(Module mod, boolean nowOn) {
        ModuleAnimState state = moduleAnims.get(mod.getName());
        if (state == null) {
            state = new ModuleAnimState();
            moduleAnims.put(mod.getName(), state);
        }
        float target = nowOn ? 1f : 0f;
        state.knob.animate(target, TOGGLE_DURATION, Easing.EASE_OUT_CUBIC);
        state.highlight.animate(target, TOGGLE_DURATION, Easing.EASE_OUT_CUBIC);
    }

    private void startExpandAnim(Module mod, boolean expand) {
        ModuleAnimState state = moduleAnims.get(mod.getName());
        if (state == null) {
            state = new ModuleAnimState();
            moduleAnims.put(mod.getName(), state);
        }
        float target = expand ? 1f : 0f;
        state.expand.animate(target, EXPAND_DURATION, Easing.EASE_OUT_CUBIC);
    }

    private float getSettingsFullHeight(Module mod) {
        List<Setting<?>> settings = mod.getSettings();
        if (settings.isEmpty()) return 0;
        float h = SETTING_PAD;
        h += KEYBIND_H + SETTING_GAP;
        int boolCount = 0;
        int numCount = 0;
        int modeCount = 0;
        for (Setting<?> s : settings) {
            if (mod.getName().equals("Particles") && s instanceof BooleanSetting && s.getName().startsWith("Type ")) continue;
            if (s instanceof NumberSetting) numCount++;
            else if (s instanceof BooleanSetting) boolCount++;
            else if (s instanceof ModeSetting) modeCount++;
        }
        int boolRows = (boolCount + 1) / 2;
        h += boolRows * (SETTING_BTN_H + SETTING_GAP);
        h += numCount * (26f + SETTING_GAP);
        h += modeCount * (SETTING_BTN_H + SETTING_GAP);

        int typeCount = 0;
        for (Setting<?> s : settings) {
            if (mod.getName().equals("Particles") && s instanceof BooleanSetting && s.getName().startsWith("Type ")) {
                typeCount++;
            }
        }
        if (typeCount > 0) {
            int cols = 3;
            float cellH = SETTING_BTN_H;
            int rows = (typeCount + cols - 1) / cols;
            h += 2 + SETTING_BTN_H + SETTING_GAP + rows * (cellH + SETTING_GAP) + 2;
        }
        h += SETTING_PAD;
        float maxBody = H - BOTTOM_BAR - 32 - HEADER_H - 6;
        return Math.min(h, maxBody);
    }

    private float getTargetCardHeight(Module mod) {
        float expandTarget = expandedModules.getOrDefault(mod.getName(), false) ? 1f : 0f;
        return 30 + getSettingsFullHeight(mod) * expandTarget;
    }

    private float getCardAnimatedHeight(Module mod) {
        ModuleAnimState state = moduleAnims.get(mod.getName());
        float expand = state != null ? state.expand.getValue() : 0f;
        return 30 + getSettingsFullHeight(mod) * expand;
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button != 0 && button != 1) return super.mouseClicked(mx, my, button);
        if (closing) return super.mouseClicked(mx, my, button);

        float open = openAnim.getValue();
        float gx = (width - W) / 2f + guiOffsetX;
        float gy = (height - H) / 2f + guiOffsetY;

        // GUI renders 1:1, local coords = screen coords
        float lx = (float) mx;
        float ly = (float) my;

        // GUI drag handle — top 10px strip across the GUI
        if (button == 0 && lx >= gx && lx <= gx + W && ly >= gy && ly <= gy + 10) {
            draggingGui = true;
            dragGuiOffX = lx - gx;
            dragGuiOffY = ly - gy;
            return true;
        }

        // Search bar click (centered below GUI)
        if (button == 0) {
            float searchW = 150f;
            float searchH = 14f;
            float searchX = gx + W / 2f - searchW / 2f;
            float searchY = gy + H + 6;
            if (lx >= searchX && lx <= searchX + searchW && ly >= searchY && ly <= searchY + searchH) {
                if (searchElement.handleClick(lx - searchX, ly - searchY, 0, 0)) {
                    return true;
                }
                return true;
            }
            if (button == 0) searchElement.setSelect(false);
        }

        if (ly >= gy + H - BOTTOM_BAR && ly <= gy + H) {
            Category[] cats = Category.ALL;
            float cubeW = 30f;
            float cubeH = 30f;
            float gap = 4f;
            float panelPadX = 6f;
            float panelPadY = 5f;
            float totalCubesW = cats.length * cubeW + (cats.length - 1) * gap;
            float panelW = totalCubesW + panelPadX * 2;
            float panelH = cubeH + panelPadY * 2;
            float panelX = gx + (W - panelW) / 2f;
            float panelY = gy + H - BOTTOM_BAR + (BOTTOM_BAR - panelH) / 2f - 5f;
            float startX = panelX + panelPadX;
            float cubeY = panelY + panelPadY;
            for (int i = 0; i < cats.length; i++) {
                float bx = startX + i * (cubeW + gap);
                if (lx >= bx && lx <= bx + cubeW && ly >= cubeY && ly <= cubeY + cubeH) {
                if (i != selectedCategory) {
                    if (catPhase == 0) {
                        pendingCategory = i;
                        prevCategory = selectedCategory;
                        catFadeOut.snapTo(1f);
                        catFadeOut.animate(0f, 180, Easing.EASE_IN_OUT_CUBIC);
                        catPhase = 1;
                    }
                }
                    return true;
                }
            }
        }

        if (Category.ALL[selectedCategory] == Category.THEMES) {
            if (button == 0) return handleThemesClick(lx, ly, gx, gy);
            return super.mouseClicked(mx, my, button);
        }

        if (Category.ALL[selectedCategory] == Category.SETTINGS) {
            if (button != 0) return super.mouseClicked(mx, my, button);
            int ox = (int) (gx + 14);
            int oy = (int) (gy + 14 + 20);
            int w = W - 28;

            float sliderY = oy + 8 - contentScroll;
            float trackH = 18;
            if (ly >= sliderY && ly <= sliderY + trackH && lx >= ox && lx <= ox + w) {
                float prog = (lx - ox) / w;
                prog = Math.max(0, Math.min(1, prog));
                mainScaleSetting = 0.5f + prog * 1.0f;
                MeoRayClient.mainScale = mainScaleSetting;
                draggingMainScale = true;
                return true;
            }

            float btnH = 14;

            float dropdownX = ox;
            float dropdownY = oy + 52 - contentScroll;
            float dropdownItemH = 13;
            float dropdownW = effectsDropdownCompactWidth;

            if (ly >= dropdownY && ly <= dropdownY + btnH) {
                effectsDropdownOpen = !effectsDropdownOpen;
                float target = effectsDropdownOpen ? 1f : 0f;
                effectsDropdownAnim.animate(target, 200L, Easing.EASE_OUT_CUBIC);
                return true;
            }
            if (effectsDropdownOpen) {
                for (int mi = 0; mi < 3; mi++) {
                    float diy = dropdownY + btnH + 2 + mi * dropdownItemH;
                    if (ly >= diy && ly <= diy + dropdownItemH && lx >= dropdownX && lx <= dropdownX + dropdownW) {
                        if (mi == 0) { snowOn = !snowOn; MeoRayClient.snowOn = snowOn; }
                        else if (mi == 1) { starsOn = !starsOn; MeoRayClient.starsOn = starsOn; }
                        else if (mi == 2) { bubblesOn = !bubblesOn; MeoRayClient.bubblesOn = bubblesOn; }
                        particlesInit = false;
                        return true;
                    }
                }
                effectsDropdownOpen = false;
            }

            return super.mouseClicked(mx, my, button);
        }

        int cxx = (int) (gx + 14);
        int cyy = (int) (gy + 14);
        int cw = W - 28;
        List<Module> allMods = MeoRayClient.INSTANCE.moduleManager
            .getByCategory(Category.ALL[selectedCategory]);
        String searchText = searchElement.getValue();
        List<Module> modules = searchText.isEmpty() ? allMods
            : allMods.stream().filter(m -> m.getName().toLowerCase().contains(searchText.toLowerCase())).collect(java.util.stream.Collectors.toList());

        int gap = 5;
        int colW = (cw - gap) / 2;

        int[] moduleCol = new int[modules.size()];
        for (int i = 0; i < modules.size(); i++) {
            moduleCol[i] = i % 2;
        }

        float[] colYs = { cyy + 20, cyy + 20 };

        for (int i = 0; i < modules.size(); i++) {
            Module mod = modules.get(i);
            int col = moduleCol[i];
            float cardH = getCardAnimatedHeight(mod);
            float cardX = cxx + col * (colW + gap);
            float cardY = colYs[col] - contentScroll;

            colYs[col] = colYs[col] + cardH + gap;

            if (lx < cardX || lx > cardX + colW || ly < cardY || ly > cardY + cardH) continue;

            float headerH = 30;
            float tw = 24;
            float tx = cardX + colW - tw - 8;
            float ty = cardY + (headerH - 10) / 2;

            float gearS = 7.0F;
            float gearX = tx - 4 - gearS * 2;
            float gearY = cardY + (headerH - gearS * 1.5f) / 2;

            boolean inGear = lx >= gearX - 2 && lx <= gearX + gearS * 2 + 2
                && ly >= gearY - 2 && ly <= gearY + gearS * 1.5f + 2;

            boolean inHeader = ly >= cardY && ly < cardY + headerH;

            if (button == 1) {
                if (inHeader && !inGear) {
                    boolean wasExpanded = expandedModules.getOrDefault(mod.getName(), false);
                    expandedModules.put(mod.getName(), !wasExpanded);
                    mod.setExtended(!wasExpanded);
                    startExpandAnim(mod, !wasExpanded);
                    ModuleAnimState gs = moduleAnims.get(mod.getName());
                    if (gs != null) {
                        gs.gearAngle.animate(gs.gearAngle.getValue() + (float) Math.PI * 2, 400, Easing.EASE_OUT_CUBIC);
                    }
                    return true;
                }
                return super.mouseClicked(mx, my, button);
            }

            if (inGear) {
                boolean wasExpanded = expandedModules.getOrDefault(mod.getName(), false);
                expandedModules.put(mod.getName(), !wasExpanded);
                startExpandAnim(mod, !wasExpanded);
                ModuleAnimState gs = moduleAnims.get(mod.getName());
                if (gs != null) {
                    gs.gearAngle.animate(gs.gearAngle.getValue() + (float) Math.PI * 2, 400, Easing.EASE_OUT_CUBIC);
                }
                return true;
            }

            boolean expanded = expandedModules.getOrDefault(mod.getName(), false);
            float settingsH = expanded ? getSettingsFullHeight(mod) : 0;
            float settingsStartY = cardY + headerH;

            if (expanded && settingsH > 0 && ly > settingsStartY) {
                float bodyScroll = bodyScrollOffsets.getOrDefault(mod.getName(), 0f);
                if (handleSettingsClick(mod, lx, ly + bodyScroll, cardX, cardY, colW, open)) {
                    return true;
                }
            }

            int toggleX = (int) tx;
            int toggleY = (int) ty;
            boolean inToggle = lx >= toggleX && lx <= toggleX + tw
                && ly >= toggleY && ly <= toggleY + 10;

            if (inToggle) {
                mod.toggle();
                startToggleAnim(mod, mod.isEnabled());
                return true;
            }

            if (inHeader) {
                mod.toggle();
                startToggleAnim(mod, mod.isEnabled());
                return true;
            }

            return true;
        }

        return super.mouseClicked(mx, my, button);
    }

    private boolean handleSettingsClick(Module mod, float lx, float ly, float cardX, float cardY, float cardW, float open) {
        if (!mod.isEnabled()) return false;
        List<Setting<?>> settings = mod.getSettings();
        float sx = cardX + SETTING_PAD;
        float settingW = cardW - SETTING_PAD * 2f;
        float curY = cardY + HEADER_H + SETTING_PAD;

        boolean isParticles = mod.getName().equals("Particles");
        MsdfFont medium = FontManager.SUISSEINTMEDIUM.get();

        // Keybind
        if (ly >= curY && ly <= curY + KEYBIND_H && lx >= sx && lx <= sx + settingW) {
            boolean binding = keybindTarget != null && keybindTarget.equals(mod.getName());
            if (binding) {
                keybindTarget = null;
                mod.setKey(0);
            } else {
                keybindTarget = mod.getName();
            }
            return true;
        }
        curY += KEYBIND_H + SETTING_GAP;

        // Group settings
        java.util.List<BooleanSetting> typeSettings = new java.util.ArrayList<>();
        java.util.List<BooleanSetting> bools = new java.util.ArrayList<>();
        java.util.List<NumberSetting> nums = new java.util.ArrayList<>();
        java.util.List<ModeSetting> modes = new java.util.ArrayList<>();
        for (Setting<?> s : settings) {
            if (s instanceof BooleanSetting bs) {
                if (isParticles && bs.getName().startsWith("Type ")) {
                    typeSettings.add(bs);
                } else {
                    bools.add(bs);
                }
            } else if (s instanceof NumberSetting ns) {
                nums.add(ns);
            } else if (s instanceof ModeSetting ms) {
                modes.add(ms);
            }
        }

        // Booleans 2-col
        float boolCellW = (settingW - SETTING_GAP) / 2f;
        for (int i = 0; i < bools.size(); i += 2) {
            if (ly >= curY && ly <= curY + SETTING_BTN_H) {
                if (lx >= sx && lx <= sx + boolCellW) {
                    toggleBoolean(mod, bools.get(i));
                    return true;
                }
                if (i + 1 < bools.size() && lx >= sx + boolCellW + SETTING_GAP && lx <= sx + settingW) {
                    toggleBoolean(mod, bools.get(i + 1));
                    return true;
                }
            }
            curY += SETTING_BTN_H + SETTING_GAP;
        }

        // Sliders full-width
        float sliderH = 26f;
        for (NumberSetting ns : nums) {
            float labelY = curY;
            float trackY = curY + 5.5F + 7;
            float trackH = 3.5f;
            if (ly >= trackY - 5 && ly <= trackY + trackH + 5 && lx >= sx && lx <= sx + settingW) {
                float progress = (lx - sx) / settingW;
                progress = Math.max(0, Math.min(1, progress));
                double range = ns.getMax() - ns.getMin();
                double raw = ns.getMin() + range * progress;
                double stepped = Math.round(raw / ns.getStep()) * ns.getStep();
                ns.setValue(Math.max(ns.getMin(), Math.min(ns.getMax(), stepped)));
                draggingSlider = true;
                draggingSliderMod = mod.getName();
                draggingSliderName = ns.getName();
                return true;
            }
            curY += sliderH + SETTING_GAP;
        }

        // Modes: row of buttons or dropdown
        for (ModeSetting ms : modes) {
            if (ly >= curY && ly <= curY + SETTING_BTN_H && lx >= sx && lx <= sx + settingW) {
                String[] allModes = ms.getModes();
                int n = allModes.length;
                float modeGap = 4f;
                float modePad = 8f;
                float totalTextW = 0;
                for (String m : allModes) totalTextW += medium.getWidth(m, 5.5F);
                float totalBtnW = totalTextW + n * modePad * 2 + (n - 1) * modeGap;
                boolean asRow = totalBtnW <= settingW;
                if (asRow) {
                    float curBx = sx;
                    for (int i = 0; i < n; i++) {
                        String mode = allModes[i];
                        float btnW = medium.getWidth(mode, 5.5F) + modePad * 2;
                        if (lx >= curBx && lx <= curBx + btnW) {
                            ms.setValue(mode);
                            return true;
                        }
                        curBx += btnW + modeGap;
                    }
                } else {
                    String ddKey = mod.getName() + "|" + ms.getName();
                    AnimatedFloat ddAnim = modeDropdownAnims.get(ddKey);
                    float ddProgress = ddAnim != null ? ddAnim.getValue() : 0f;
                    float target = ddProgress > 0.5f ? 0f : 1f;
                    if (ddAnim == null) {
                        AnimatedFloat newAnim = new AnimatedFloat(target);
                        modeDropdownAnims.put(ddKey, newAnim);
                        newAnim.animate(target, 200L, Easing.EASE_OUT_CUBIC);
                    } else {
                        ddAnim.snapTo(ddProgress > 0.5f ? 1f : 0f);
                        ddAnim.animate(target, 200L, Easing.EASE_OUT_CUBIC);
                    }
                    return true;
                }
            }
            // Check dropdown items
            String ddKey2 = mod.getName() + "|" + ms.getName();
            AnimatedFloat ddAnim2 = modeDropdownAnims.get(ddKey2);
            float ddProgress2 = ddAnim2 != null ? ddAnim2.getValue() : 0f;
            if (ddProgress2 > 0.1f) {
                float ddY = curY + SETTING_BTN_H + 2;
                float ddItemH = 14;
                int n = ms.getModes().length;
                for (int mi = 0; mi < n; mi++) {
                    float iy = ddY + mi * ddItemH;
                    if (ly >= iy && ly <= iy + ddItemH && lx >= sx && lx <= sx + settingW) {
                        ms.setValue(ms.getModes()[mi]);
                        if (ddAnim2 != null) {
                            ddAnim2.snapTo(ddProgress2);
                            ddAnim2.animate(0f, 200L, Easing.EASE_OUT_CUBIC);
                        }
                        return true;
                    }
                }
            }
            curY += SETTING_BTN_H + SETTING_GAP;
        }

        // Particles type grid
        if (!typeSettings.isEmpty()) {
            curY += 2;
            float headerH = SETTING_BTN_H;
            if (lx >= sx && lx <= sx + settingW && ly >= curY && ly <= curY + headerH) {
                typeGridExpanded = !typeGridExpanded;
                return true;
            }
            curY += headerH + SETTING_GAP;

            if (typeGridExpanded) {
                int cols = 3;
                float cellGap = SETTING_GAP;
                float cellW = (settingW - (cols - 1) * cellGap) / cols;
                float cellH = SETTING_BTN_H;
                for (int ti = 0; ti < typeSettings.size(); ti++) {
                    int tCol = ti % cols;
                    int tRow = ti / cols;
                    float cellX = sx + tCol * (cellW + cellGap);
                    float cellY = curY + tRow * (cellH + cellGap);
                    if (lx >= cellX && lx <= cellX + cellW && ly >= cellY && ly <= cellY + cellH) {
                        toggleBoolean(mod, typeSettings.get(ti));
                        return true;
                    }
                }
            }
        }

        return false;
    }

    private void toggleBoolean(Module mod, BooleanSetting bs) {
        boolean oldVal = bs.getValue();
        bs.toggle();
        boolean newVal = !oldVal;
        ModuleAnimState mst = moduleAnims.get(mod.getName());
        if (mst != null) {
            AnimatedFloat sa = mst.settingAnims.get(bs.getName());
            if (sa == null) {
                sa = new AnimatedFloat(oldVal ? 1f : 0f);
                mst.settingAnims.put(bs.getName(), sa);
            } else {
                sa.snapTo(oldVal ? 1f : 0f);
            }
            sa.animate(newVal ? 1f : 0f, 200, Easing.EASE_OUT_CUBIC);
        }
    }

    private void handleSliderDrag(float lx, float ly) {
        if (!draggingSlider || draggingSliderMod == null) return;

        Module mod = MeoRayClient.INSTANCE.moduleManager.getByName(draggingSliderMod);
        if (mod == null) {
            draggingSlider = false;
            return;
        }

        float gx = (width - W) / 2f;
        float gy = (height - H) / 2f;
        int cxx = (int) (gx + 14);
        int cyy = (int) (gy + 14);
        int cw = W - 28;
        List<Module> modules = MeoRayClient.INSTANCE.moduleManager.getByCategory(Category.ALL[selectedCategory]);

        int gap = 5;
        int colW = (cw - gap) / 2;

        int[] moduleCol = new int[modules.size()];
        for (int i = 0; i < modules.size(); i++) {
            moduleCol[i] = i % 2;
        }

        float[] colYs = { cyy + 20, cyy + 20 };

        boolean isParticles = mod.getName().equals("Particles");
        int nBools = 0, nNums = 0, nModes = 0;
        for (Setting<?> s : mod.getSettings()) {
            if (s instanceof BooleanSetting) nBools++;
            else if (s instanceof NumberSetting) nNums++;
            else if (s instanceof ModeSetting) nModes++;
        }
        int boolRows = (nBools + 1) / 2;
        float cardStartY = cardY_of(mod, modules, moduleCol, colYs, colW, cxx, cyy, gap);
        float settingW = colW - SETTING_PAD * 2f;
        float curY = cardStartY + HEADER_H + SETTING_PAD + KEYBIND_H + SETTING_GAP;
        curY += boolRows * (SETTING_BTN_H + SETTING_GAP);
        for (int i = 0; i < nNums; i++) {
            if (i == 0) {
                // dragged slider is in this position
            }
            curY += 26f + SETTING_GAP;
        }
        // We don't really need to find the slider here — use the dragged name and just apply progress
        for (Setting<?> s : mod.getSettings()) {
            if (s instanceof NumberSetting ns && s.getName().equals(draggingSliderName)) {
                float sx = cardX_of(mod, modules, moduleCol, colYs, colW, cxx, cyy, gap) + SETTING_PAD;
                float progress = (lx - sx) / settingW;
                progress = Math.max(0, Math.min(1, progress));
                double range = ns.getMax() - ns.getMin();
                double raw = ns.getMin() + range * progress;
                double stepped = Math.round(raw / ns.getStep()) * ns.getStep();
                ns.setValue(Math.max(ns.getMin(), Math.min(ns.getMax(), stepped)));
                break;
            }
        }
    }

    private float cardX_of(Module target, List<Module> modules, int[] moduleCol, float[] colYs, int colW, int cxx, int cyy, int gap) {
        for (int i = 0; i < modules.size(); i++) {
            Module m = modules.get(i);
            int col = moduleCol[i];
            if (!m.getName().equals(target.getName())) {
                float h = getCardAnimatedHeight(m);
                colYs[col] = colYs[col] + h + gap;
                continue;
            }
            return cxx + col * (colW + gap);
        }
        return cxx;
    }

    private float cardY_of(Module target, List<Module> modules, int[] moduleCol, float[] colYs, int colW, int cxx, int cyy, int gap) {
        for (int i = 0; i < modules.size(); i++) {
            Module m = modules.get(i);
            int col = moduleCol[i];
            if (!m.getName().equals(target.getName())) {
                float h = getCardAnimatedHeight(m);
                colYs[col] = colYs[col] + h + gap;
                continue;
            }
            return colYs[col];
        }
        return cyy;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        float gx = (width - W) / 2f + guiOffsetX;
        float gy = (height - H) / 2f + guiOffsetY;
        float scrollAreaTop = gy + 14 + 20;
        float scrollAreaBottom = gy + H - BOTTOM_BAR;

        float ly = (float) mouseY;
        float lx = (float) mouseX;

        if (ly < scrollAreaTop || ly > scrollAreaBottom) {
            return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
        }

        // === Проверяем находится ли курсор над развёрнутой карточкой ===
        Category cat = Category.ALL[selectedCategory];
        if (cat != Category.THEMES && cat != Category.SETTINGS) {
            List<Module> allMods = MeoRayClient.INSTANCE.moduleManager.getByCategory(cat);
            String searchText = searchElement.getValue();
            List<Module> modules = searchText.isEmpty() ? allMods
                : allMods.stream().filter(m -> m.getName().toLowerCase().contains(searchText.toLowerCase()))
                    .collect(java.util.stream.Collectors.toList());

            int cxx = (int) (gx + 14);
            int cyy = (int) (gy + 14);
            int cw = W - 28;
            int gap = 5;
            int colW = (cw - gap) / 2;

            int[] moduleCol = new int[modules.size()];
            for (int i = 0; i < modules.size(); i++) moduleCol[i] = i % 2;

            float[] colYs = { cyy + 20, cyy + 20 };

            for (int i = 0; i < modules.size(); i++) {
                Module mod = modules.get(i);
                int col = moduleCol[i];
                float cardH = getCardAnimatedHeight(mod);
                float cardX = cxx + col * (colW + gap);
                float cardY = colYs[col] - contentScroll;
                colYs[col] = colYs[col] + cardH + gap;

                // Проверяем находится ли курсор над ТЕЛОМ (не header) развёрнутой карточки
                boolean expanded = expandedModules.getOrDefault(mod.getName(), false);
                if (!expanded) continue;

                float bodyY = cardY + HEADER_H;
                float bodyH = cardH - HEADER_H;
                if (bodyH <= 0) continue;

                if (lx >= cardX && lx <= cardX + colW && ly >= bodyY && ly <= bodyY + bodyH) {
                    // Курсор над телом — скроллим тело
                    String mn = mod.getName();
                    float maxScroll = bodyScrollMax.getOrDefault(mn, 0f);
                    if (maxScroll > 0f) {
                        float cur = bodyScrollOffsets.getOrDefault(mn, 0f);
                        cur -= (float) verticalAmount * 16f;
                        if (cur < 0) cur = 0;
                        if (cur > maxScroll) cur = maxScroll;
                        bodyScrollOffsets.put(mn, cur);
                        return true;
                    }
                    // если в теле нечего скроллить — глотаем событие, чтобы не скроллить контейнер
                    return true;
                }
            }
        }

        // === Иначе скроллим основной контент ===
        contentScroll -= (float) verticalAmount * 16f;
        return true;
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        if (button == 0) {
            draggingSlider = false;
            draggingSliderMod = null;
            draggingSliderName = null;
            draggingMainScale = false;
            draggingGui = false;
        }
        return super.mouseReleased(mx, my, button);
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double deltaX, double deltaY) {
        if (button == 0 && draggingGui) {
            float newGx = (float) mx - dragGuiOffX;
            float newGy = (float) my - dragGuiOffY;
            int minX = -W + 50;
            int maxX = width - 50;
            int minY = -(height - H) / 2;
            int maxY = height - 30;
            guiOffsetXTarget = Math.max(minX, Math.min(maxX, newGx - (width - W) / 2));
            guiOffsetYTarget = Math.max(minY, Math.min(maxY, newGy - (height - H) / 2));
            return true;
        }
        if (button == 0 && draggingMainScale) {
            float open = openAnim.getValue();
            float gx = (width - W) / 2f + guiOffsetX;
            float lx = (float) mx;
            int ox = (int) (gx + 14);
            int w = W - 28;
            float prog = (lx - ox) / w;
            prog = Math.max(0, Math.min(1, prog));
            mainScaleSetting = 0.5f + prog * 1.0f;
            MeoRayClient.mainScale = mainScaleSetting;
            return true;
        }
        return super.mouseDragged(mx, my, button, deltaX, deltaY);
    }

    private boolean handleThemesClick(float lx, float ly, float gx, float gy) {
        int ox = (int) (gx + 14);
        int oy = (int) (gy + 14 + 20);
        int w = W - 28;

        int cols = 4;
        int gapX = 8;
        int gapY = 10;
        int cardW = (w - (cols - 1) * gapX) / cols;
        int cardH = 100;

        ThemeManager mgr = MeoRayClient.INSTANCE.getThemeManager();
        Theme[] themes = mgr.getThemes();

        for (int i = 0; i < themes.length; i++) {
            int col = i % cols;
            int row = i / cols;
            int cx = ox + col * (cardW + gapX);
            int cy = (int)(oy + row * (cardH + gapY) - contentScroll);

            if ((int) lx >= cx && (int) lx <= cx + cardW
                && (int) ly >= cy && (int) ly <= cy + cardH) {
                if (i != mgr.getSelectedIndex()) {
                    mgr.select(i);
                }
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        searchElement.tick(0, String.valueOf(chr));
        return super.charTyped(chr, modifiers);
    }

    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keybindTarget != null) {
            for (Module mod : MeoRayClient.INSTANCE.moduleManager.getModules()) {
                if (mod.getName().equals(keybindTarget)) {
                    if (keyCode == GLFW.GLFW_KEY_ESCAPE || keyCode == GLFW.GLFW_KEY_ENTER) {
                        mod.setKey(0);
                    } else {
                        mod.setKey(keyCode);
                    }
                    break;
                }
            }
            keybindTarget = null;
            return true;
        }
        searchElement.tick(keyCode, "");

        if (keyCode == GLFW.GLFW_KEY_RIGHT_SHIFT || keyCode == GLFW.GLFW_KEY_ESCAPE) {
            if (!closing) {
                closing = true;
                openAnim.animate(0f, CLOSE_DURATION, Easing.EASE_IN_OUT_CUBIC);
            }
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    private static String getKeyName(int key) {
        if (key <= 0) return "NONE";
        String name = GLFW.glfwGetKeyName(key, 0);
        return name != null ? name.toUpperCase() : "KEY_" + key;
    }

    private void initParticles() {
        bgParticles.clear();
        int countPerType = 25;
        if (snowOn) {
            for (int i = 0; i < countPerType; i++) {
                Particle p = new Particle();
                p.type = 0;
                p.x = rand.nextFloat() * (W - 20);
                p.y = rand.nextFloat() * H;
                p.size = 1.5f + rand.nextFloat() * 2.0f;
                p.alpha = 0.4f + rand.nextFloat() * 0.4f;
                p.baseAlpha = p.alpha;
                p.phase = rand.nextFloat() * 100;
                p.vy = -(0.3f + rand.nextFloat() * 0.6f);
                p.vx = (rand.nextFloat() - 0.5f) * 0.15f;
                bgParticles.add(p);
            }
        }
        if (starsOn) {
            for (int i = 0; i < countPerType; i++) {
                Particle p = new Particle();
                p.type = 1;
                p.x = rand.nextFloat() * (W - 20);
                p.y = rand.nextFloat() * H;
                p.size = 1.5f + rand.nextFloat() * 1.5f;
                p.alpha = 0.3f + rand.nextFloat() * 0.4f;
                p.baseAlpha = p.alpha;
                p.phase = rand.nextFloat() * 100;
                p.vy = -(0.2f + rand.nextFloat() * 0.3f);
                p.vx = (rand.nextFloat() - 0.5f) * 0.1f;
                p.speed = 0.02f + rand.nextFloat() * 0.03f;
                bgParticles.add(p);
            }
        }
        if (bubblesOn) {
            for (int i = 0; i < countPerType; i++) {
                Particle p = new Particle();
                p.type = 2;
                p.x = rand.nextFloat() * (W - 20);
                p.y = rand.nextFloat() * H;
                p.size = 3 + rand.nextFloat() * 6;
                p.alpha = 0.25f + rand.nextFloat() * 0.3f;
                p.baseAlpha = p.alpha;
                p.phase = rand.nextFloat() * 100;
                p.vy = -(0.15f + rand.nextFloat() * 0.25f);
                p.vx = (rand.nextFloat() - 0.5f) * 0.08f;
                bgParticles.add(p);
            }
        }
        particlesInit = true;
    }

    private void updateParticles() {
        if (!particlesInit) initParticles();
        float fadeZone = H * 0.3f;
        for (Particle p : bgParticles) {
            p.phase += 0.05f;
            p.y += p.vy;
            p.x += p.vx;

            if (p.y < -p.size) {
                p.x = rand.nextFloat() * (W - 20);
                p.y = H + p.size;
                p.life = 0f;
                continue;
            }

            if (p.life < 1f) p.life = Math.min(1f, p.life + 0.018f);

            float fadeFactor = Math.min(1, p.y / fadeZone);
            p.alpha = p.baseAlpha * fadeFactor * p.life;
            if (p.alpha < 0) p.alpha = 0;

            if (p.type == 0) { // snow
                p.x += (float) Math.sin(p.phase * 0.5) * 0.2f;
            } else if (p.type == 1) { // stars - shimmer
                p.alpha *= 0.6f + 0.4f * (float) Math.sin(p.phase * 0.3f);
            }
        }
    }

    private void renderBackgroundEffects(Matrix4f matrix, int ox, int oy) {
        if (!particlesInit) initParticles();
        updateParticles();

        enableWindowScissor(ox, oy);

        MsdfFont icons = FontManager.ICONS.get();
        for (Particle p : bgParticles) {
            int fadeAlpha = (int)(255 * p.alpha);
            if (fadeAlpha < 2) continue;
            int col = 0x00FFFFFF | (fadeAlpha << 24);
            float px = ox + p.x;
            float py = oy + p.y;

            if (p.type == 0) { // snow - filled circle
                ((BuiltRectangle) Builder.rectangle()
                    .size(new SizeState(p.size, p.size))
                    .color(new QuadColorState(col))
                    .radius(new QuadRadiusState(p.size / 2f))
                    .smoothness(1.15F)
                    .build()).render(matrix, px, py);

            } else if (p.type == 1) { // stars - 4-point cross
                float s = p.size;
                int starCol = col;
                ((BuiltRectangle) Builder.rectangle()
                    .size(new SizeState(s * 0.25f, s * 1.5f))
                    .color(new QuadColorState(starCol))
                    .radius(new QuadRadiusState(s * 0.12f))
                    .smoothness(1.15F)
                    .build()).render(matrix, px - s * 0.125f, py - s * 0.75f);
                ((BuiltRectangle) Builder.rectangle()
                    .size(new SizeState(s * 1.5f, s * 0.25f))
                    .color(new QuadColorState(starCol))
                    .radius(new QuadRadiusState(s * 0.12f))
                    .smoothness(1.15F)
                    .build()).render(matrix, px - s * 0.75f, py - s * 0.125f);

            } else if (p.type == 2) { // bubbles - hollow outline
                float s = p.size;
                int borderCol = col;
                ((BuiltBorder) Builder.border()
                    .size(new SizeState(s, s))
                    .color(new QuadColorState(borderCol))
                    .radius(new QuadRadiusState(s / 2f))
                    .thickness(0.06f)
                    .smoothness(0.65f, 0.65f)
                    .build()).render(matrix, px, py);
            }
        }

        RenderSystem.disableScissor();
    }

    private void renderSettingsTab(Matrix4f matrix, int ox, int oy, int w, int h, float lx, float ly, float open, Theme theme, float scrollOffset) {
        MsdfFont medium = FontManager.SUISSEINTMEDIUM.get();
        float curY = oy - scrollOffset;

        int labelColor = alphaBlend(0xFFCCCCCC, open, 255);
        ((BuiltText) Builder.text()
            .font(medium).text("Main Scale")
            .color(labelColor).size(5.5F).thickness(0.04F)
            .build()).render(matrix, ox, curY);
        curY += 10;

        float sliderY = curY;
        float sliderH = 3;
        float trackW = w;
        float knobR = 5;
        float progress = (mainScaleSetting - 0.5f) / 1.0f;

        int trackBg = 0x60000000;
        trackBg = alphaBlend(trackBg, open, 255);
        ((BuiltRectangle) Builder.rectangle()
            .size(new SizeState(trackW, sliderH))
            .color(new QuadColorState(trackBg))
            .radius(new QuadRadiusState(1.5f))
            .smoothness(1.15F)
            .build()).render(matrix, ox, sliderY + (knobR - sliderH / 2f));

        float fillW = trackW * progress;
        if (fillW > 1) {
            int fillCol = alphaBlend(0xFFCCCCCC, open, 180);
            ((BuiltRectangle) Builder.rectangle()
                .size(new SizeState(fillW, sliderH))
                .color(new QuadColorState(fillCol))
                .radius(new QuadRadiusState(1.5f, 0, 0, 1.5f))
                .smoothness(1.15F)
                .build()).render(matrix, ox, sliderY + (knobR - sliderH / 2f));
        }

        float knobCX = ox + trackW * progress;
        float knobCY = sliderY + knobR;
        int knobCol = alphaBlend(0xFFCCCCCC, open, 255);
        ((BuiltRectangle) Builder.rectangle()
            .size(new SizeState(knobR * 2, knobR * 2))
            .color(new QuadColorState(knobCol))
            .radius(new QuadRadiusState(knobR))
            .smoothness(1.15F)
            .build()).render(matrix, knobCX - knobR, knobCY - knobR);

        String valStr = String.format("%.2f", mainScaleSetting);
        float valW = medium.getWidth(valStr, 5.5F);
        int valCol = alphaBlend(theme.accent(), open, 255);
        ((BuiltText) Builder.text()
            .font(medium).text(valStr)
            .color(valCol).size(5.5F).thickness(0.04F)
            .build()).render(matrix, ox + w - valW, oy);

        curY = sliderY + knobR * 2 + 10;

        int sepColor = (theme.border() & 0x00FFFFFF) | 0x32000000;
        sepColor = alphaBlend(sepColor, open, 255);
        ((BuiltRectangle) Builder.rectangle()
            .size(new SizeState(w, 1.0f))
            .color(new QuadColorState(sepColor))
            .radius(new QuadRadiusState(0.0))
            .smoothness(1.15F)
            .build()).render(matrix, ox, curY);
        curY += 10;

        int bgLabelColor = alphaBlend(0xFFCCCCCC, open, 255);
        ((BuiltText) Builder.text()
            .font(medium).text("Background Effects")
            .color(bgLabelColor).size(5.5F).thickness(0.04F)
            .build()).render(matrix, ox, curY);
        curY += 12;

        float dropdownX = ox;
        float dropdownY = curY;
        float btnH = 14;

        String[][] effectData = {{"Snow", "\u2744"}, {"Stars", "\u2605"}, {"Bubbles", "\u25CB"}};
        boolean[] states = {snowOn, starsOn, bubblesOn};

        // Compact width: widest effect name + icon + padding
        float maxLabelW = 0;
        for (int mi = 0; mi < 3; mi++) {
            float lw = medium.getWidth(effectData[mi][0], 5.5F);
            if (lw > maxLabelW) maxLabelW = lw;
        }
        effectsDropdownCompactWidth = Math.max(maxLabelW + 18, 50);

        // === Trigger button (compact) ===
        float trigW = effectsDropdownCompactWidth;
        int anyActive = (snowOn ? 1 : 0) + (starsOn ? 1 : 0) + (bubblesOn ? 1 : 0);
        int trigBg = anyActive > 0
            ? alphaBlend((theme.accent() & 0x00FFFFFF) | 0x70000000, open, 255)
            : alphaBlend(SETTING_BTN_BG, open, 255);
        int trigBorder = anyActive > 0
            ? alphaBlend(theme.accent(), open, 180)
            : alphaBlend(SETTING_BTN_BORDER_COL, open, 255);
        ((BuiltRectangle) Builder.rectangle()
            .size(new SizeState(trigW, btnH))
            .color(new QuadColorState(trigBg))
            .radius(new QuadRadiusState(SETTING_BTN_RADIUS))
            .smoothness(1.15F)
            .build()).render(matrix, dropdownX, dropdownY);
        ((BuiltBorder) Builder.border()
            .size(new SizeState(trigW, btnH))
            .color(new QuadColorState(trigBorder))
            .radius(new QuadRadiusState(SETTING_BTN_RADIUS))
            .thickness(0.018F)
            .smoothness(0.65F, 0.65F)
            .build()).render(matrix, dropdownX, dropdownY);

        String trigLabel = anyActive == 0 ? "None" : (anyActive == 1
            ? (snowOn ? "Snow" : (starsOn ? "Stars" : "Bubbles"))
            : (anyActive + " active"));
        int trigTextCol = anyActive > 0
            ? alphaBlend(0xFFFFFFFF, open, 255)
            : alphaBlend(0xFF999999, open, 255);
        ((BuiltText) Builder.text()
            .font(medium).text(trigLabel)
            .color(trigTextCol).size(5.5F).thickness(0.04F)
            .build()).render(matrix, dropdownX + 6, dropdownY + (btnH - 5.5F) / 2);

        // Animated dropdown list of cubes
        float ddAnim = effectsDropdownAnim.update();
        float cubeH = 16;
        float cubeGap = 4;
        float ddFullH = 3 * cubeH + 2 * cubeGap;
        float ddH = ddFullH * ddAnim;

        if (ddH > 0.5f) {
            // Background panel for the list
            int ddBg = 0xF0141418;
            ddBg = alphaBlend(ddBg, open, 255);
            ((BuiltRectangle) Builder.rectangle()
                .size(new SizeState(trigW, ddH))
                .color(new QuadColorState(ddBg))
                .radius(new QuadRadiusState(SETTING_BTN_RADIUS))
                .smoothness(1.15F)
                .build()).render(matrix, dropdownX, dropdownY + btnH + 3);

            for (int mi = 0; mi < 3; mi++) {
                float itemAlpha = (ddAnim * 3 - mi * 0.7f);
                itemAlpha = Math.max(0, Math.min(1, itemAlpha * 2.5f));
                if (itemAlpha < 0.01f) continue;
                float iy = dropdownY + btnH + 3 + mi * (cubeH + cubeGap);
                if (iy - (dropdownY + btnH + 3) > ddH) continue;
                boolean sel = states[mi];
                // Cube background
                int cubeBg = sel
                    ? alphaBlend((theme.accent() & 0x00FFFFFF) | 0x80000000, open, 255)
                    : alphaBlend(SETTING_BTN_BG, open, 255);
                ((BuiltRectangle) Builder.rectangle()
                    .size(new SizeState(trigW, cubeH))
                    .color(new QuadColorState(cubeBg))
                    .radius(new QuadRadiusState(SETTING_BTN_RADIUS))
                    .smoothness(1.15F)
                    .build()).render(matrix, dropdownX, iy);
                // Cube border
                int cubeBorder = sel
                    ? alphaBlend(theme.accent(), open, 220)
                    : alphaBlend(SETTING_BTN_BORDER_COL, open, 255);
                ((BuiltBorder) Builder.border()
                    .size(new SizeState(trigW, cubeH))
                    .color(new QuadColorState(cubeBorder))
                    .radius(new QuadRadiusState(SETTING_BTN_RADIUS))
                    .thickness(0.018F)
                    .smoothness(0.65F, 0.65F)
                    .build()).render(matrix, dropdownX, iy);

                // Icon
                int iconCol = alphaBlend(sel ? 0xFFFFFFFF : 0xFFB0B0B8, open, (int)(255 * itemAlpha));
                ((BuiltText) Builder.text()
                    .font(medium).text(effectData[mi][1])
                    .color(iconCol).size(7.0F).thickness(0.05F)
                    .build()).render(matrix, dropdownX + 6, iy + (cubeH - 7.0F) / 2);

                // Label
                int labelCol = alphaBlend(sel ? 0xFFFFFFFF : 0xFFCCCCCC, open, (int)(255 * itemAlpha));
                ((BuiltText) Builder.text()
                    .font(medium).text(effectData[mi][0])
                    .color(labelCol).size(5.5F).thickness(0.04F)
                    .build()).render(matrix, dropdownX + 18, iy + (cubeH - 5.5F) / 2);

                // Check mark on right
                if (sel) {
                    int checkCol = alphaBlend(theme.accent(), open, (int)(255 * itemAlpha));
                    ((BuiltText) Builder.text()
                        .font(medium).text("\u2713")
                        .color(checkCol).size(7.0F).thickness(0.05F)
                        .build()).render(matrix, dropdownX + trigW - 12, iy + (cubeH - 7.0F) / 2);
                }
            }
        }
    }

    private static int lerpColor(int c1, int c2, float t) {
        int a1 = (c1 >> 24) & 0xFF;
        int r1 = (c1 >> 16) & 0xFF;
        int g1 = (c1 >> 8) & 0xFF;
        int b1 = c1 & 0xFF;
        int a2 = (c2 >> 24) & 0xFF;
        int r2 = (c2 >> 16) & 0xFF;
        int g2 = (c2 >> 8) & 0xFF;
        int b2 = c2 & 0xFF;
        int a = (int) (a1 + (a2 - a1) * t);
        int r = (int) (r1 + (r2 - r1) * t);
        int g = (int) (g1 + (g2 - g1) * t);
        int b = (int) (b1 + (b2 - b1) * t);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private static int alphaBlend(int color, float factor, int maxAlpha) {
        int a = Math.min(maxAlpha, Math.max(0, (int) (factor * maxAlpha)));
        return (color & 0x00FFFFFF) | (a << 24);
    }

}
