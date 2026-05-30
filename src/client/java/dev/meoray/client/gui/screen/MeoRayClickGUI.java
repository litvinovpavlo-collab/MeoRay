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

public class MeoRayClickGUI extends Screen {

    private static final int W = 470;
    private static final int H = 290;
    private static final int SIDEBAR = 110;

    private static final long OPEN_DURATION = 450;
    private static final long CLOSE_DURATION = 350;
    private static final long TOGGLE_DURATION = 300;
    private static final long EXPAND_DURATION = 250;

    private static int persistedCategory = 0;
    private int selectedCategory;
    private boolean closing = false;
    private final AnimatedFloat openAnim = new AnimatedFloat(0f);
    private String keybindTarget;

    private static class Particle {
        float x, y, vx, vy, size, alpha, baseAlpha, speed;
        int type; // 0=snow, 1=star, 2=bubble
        float phase;
        float life = 0f;
        boolean dead;
    }

    private final java.util.List<Particle> bgParticles = new java.util.ArrayList<>();
    private boolean particlesInit;

    private boolean snowOn = true;
    private boolean starsOn;
    private boolean bubblesOn;

    private boolean effectsDropdownOpen;

    private final AnimatedFloat effectsDropdownAnim = new AnimatedFloat(0f);
    private float mainScaleSetting = 1.0f;
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
    private final Map<Integer, AnimatedFloat> catHoverAnims = new HashMap<>();
    private AnimatedFloat sidebarBounceAnim = new AnimatedFloat(0f);
    private Module hoveredModule;
    private float hoveredModuleCardX;
    private float hoveredModuleCardY;
    private float hoveredModuleCardW;
    private final AnimatedFloat tooltipAnim = new AnimatedFloat(0f);
    private static final long TOOLTIP_DURATION = 200;

    private final SearchElement searchElement = new SearchElement();
    private final Map<String, Float> sliderAnimatedProgress = new HashMap<>();
    private String hoveredSliderKey;
    private String draggingSliderMod;
    private String draggingSliderName;
    private boolean draggingSlider;
    private float contentScroll = 0f;
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

        if (closing && openAnim.isFinished()) {
            MinecraftClient.getInstance().setScreen(null);
            return;
        }

        float gx = (width - W) / 2f;
        float gy = (height - H) / 2f;

        ctx.getMatrices().push();
        float scale = 0.82f + 0.18f * open;
        float ms = MeoRayClient.mainScale;
        scale *= ms;
        float cx = width / 2f;
        float cy = height / 2f;
        ctx.getMatrices().translate(cx * (1 - scale), cy * (1 - scale), 0);
        ctx.getMatrices().scale(scale, scale, 1);

        float lx = (mx - cx * (1 - scale)) / scale;
        float ly = (my - cy * (1 - scale)) / scale;

        Matrix4f matrix = ctx.getMatrices().peek().getPositionMatrix();
        int gxi = (int) gx;
        int gyi = (int) gy;

        Theme theme = MeoRayClient.INSTANCE.getThemeManager().getRenderTheme();

        renderWindowBg(matrix, gxi, gyi, open, theme);
        renderBackgroundEffects(matrix, gxi, gyi);
        renderSidebar(matrix, gxi, gyi, lx, ly, open, theme);
        renderWindowOutline(matrix, gxi, gyi, open, theme);
        renderContent(matrix, gxi, gyi, lx, ly, open, theme);

        renderWhiteOutline(matrix, gxi, gyi, open);

        drawTooltip(matrix, gx, gy, lx, ly, open, theme);

        ctx.getMatrices().pop();

        if (draggingSlider) {
            handleSliderDrag(lx, ly);
        }
    }

    private void renderWindowBg(Matrix4f matrix, int gx, int gy, float open, Theme theme) {
        int accent = theme.accent();
        int darkTop = darkenColor(accent, 0.2f);
        int gradientTop = alphaBlend(darkTop, open, 255);
        int gradientBottom = alphaBlend(theme.gradientBottom(), open, 255);
        ((BuiltRectangle) Builder.rectangle()
            .size(new SizeState(W, H))
            .color(new QuadColorState(gradientTop, gradientTop, gradientBottom, gradientBottom))
            .radius(new QuadRadiusState(5.0))
            .smoothness(1.15F)
            .build()).render(matrix, gx, gy);

        int glowColor = (theme.accent() & 0x00FFFFFF) | 0x0A000000;
        glowColor = alphaBlend(glowColor, open, 255);
        int glowClear = alphaBlend(0x00000000, open, 0);
        ((BuiltRectangle) Builder.rectangle()
            .size(new SizeState(W, H))
            .color(new QuadColorState(glowColor, glowClear, glowClear, glowColor))
            .radius(new QuadRadiusState(5.0))
            .smoothness(1.15F)
            .build()).render(matrix, gx, gy);

        int vDiv = (theme.bgMain() & 0x00FFFFFF) | 0x1E000000;
        vDiv = alphaBlend(vDiv, open, 255);
        ((BuiltRectangle) Builder.rectangle()
            .size(new SizeState(1.0, H - 12.0))
            .color(new QuadColorState(vDiv))
            .radius(new QuadRadiusState(0.0))
            .smoothness(1.15F)
            .build()).render(matrix, gx + SIDEBAR, gy + 6);
    }

    private void renderWindowOutline(Matrix4f matrix, int gx, int gy, float open, Theme theme) {
        int border = alphaBlend(theme.border(), open, 255);
        ((BuiltBorder) Builder.border()
            .size(new SizeState(W, H))
            .color(new QuadColorState(border))
            .radius(new QuadRadiusState(5.0))
            .thickness(0.035F)
            .smoothness(0.65F, 0.65F)
            .build()).render(matrix, gx, gy);
    }

    private void renderWhiteOutline(Matrix4f matrix, int gx, int gy, float open) {
        int white = alphaBlend(0xFFFFFFFF, open, 255);
        ((BuiltBorder) Builder.border()
            .size(new SizeState(W, H))
            .color(new QuadColorState(white))
            .radius(new QuadRadiusState(5.0))
            .thickness(0.06F)
            .smoothness(0.65F, 0.65F)
            .build()).render(matrix, gx, gy);
    }

    private void renderSidebar(Matrix4f matrix, int gx, int gy, float lx, float ly, float open, Theme theme) {
        int bgSidebar = alphaBlend(theme.bgSidebar(), open, 255);
        ((BuiltRectangle) Builder.rectangle()
            .size(new SizeState(SIDEBAR, H))
            .color(new QuadColorState(bgSidebar))
            .radius(new QuadRadiusState(5.0, 0.0, 0.0, 5.0))
            .smoothness(1.15F)
            .build()).render(matrix, gx, gy);

        MsdfFont medium = FontManager.SUISSEINTMEDIUM.get();

        int pawColorSidebar = alphaBlend(theme.accent(), open, 255);
        ((BuiltText) Builder.text()
            .font(FontManager.ICONS.get()).text("D")
            .color(pawColorSidebar).size(9.0F).thickness(0.05F)
            .build()).render(matrix, gx + 12, gy + 10);

        int meorayColor = alphaBlend(theme.textPrimary(), open, 255);
        ((BuiltText) Builder.text()
            .font(medium).text("MEORAY")
            .color(meorayColor).size(8.0F).thickness(0.05F)
            .build()).render(matrix, gx + 26, gy + 10);

        int subtitleColor = alphaBlend(theme.textSecondary(), open, 180);
        ((BuiltText) Builder.text()
            .font(medium).text("\u2022 purring execution")
            .color(subtitleColor).size(4.5F).thickness(0.04F)
            .build()).render(matrix, gx + 12, gy + 22);

        int fadeY = gy + 31;
        int fadeW = SIDEBAR - 20;
        int segs = 12;
        int segW = fadeW / segs;
        for (int fi = 0; fi < segs; fi++) {
            float fp = 1f - (float) fi / segs;
            int fa = Math.min(255, (int) (fp * fp * 240));
            int fc = (theme.border() & 0x00FFFFFF) | (fa << 24);
            ((BuiltRectangle) Builder.rectangle()
                .size(new SizeState(segW, 1.0))
                .color(new QuadColorState(fc))
                .radius(new QuadRadiusState(0.0))
                .smoothness(1.15F)
                .build()).render(matrix, gx + 10 + fi * segW, fadeY);
        }

        int sepY = fadeY;

        Category[] cats = Category.ALL;
        int itemH = 28;
        int startY = sepY + 8;

        for (AnimatedFloat af : catHoverAnims.values()) {
            af.update();
        }

        for (int i = 0; i < cats.length; i++) {
            int iy = startY + i * itemH;
            int ix = gx + 6;
            boolean sel = i == selectedCategory;
            int lxi = (int) lx;
            int lyi = (int) ly;
            boolean hover = lxi >= ix && lxi <= gx + SIDEBAR
                && lyi >= iy && lyi <= iy + itemH;

            AnimatedFloat ha = catHoverAnims.computeIfAbsent(i, k -> new AnimatedFloat(0f));
            ha.animate(hover ? 1f : 0f, 150, Easing.EASE_OUT_CUBIC);
            float hp = ha.getValue();

            if (hp > 0.01f || sel) {
                int hoverBg = (theme.bgMain() & 0x00FFFFFF) | ((int)(hp * 0x33) << 24);
                hoverBg = alphaBlend(hoverBg, open, 255);
                ((BuiltRectangle) Builder.rectangle()
                    .size(new SizeState(SIDEBAR - 10, itemH))
                    .color(new QuadColorState(hoverBg))
                    .radius(new QuadRadiusState(3.0))
                    .smoothness(1.15F)
                    .build()).render(matrix, gx + 5, iy);
            }

            if (sel) {
                int indicator = (theme.accent() & 0x00FFFFFF) | 0x88000000;
                indicator = alphaBlend(indicator, open, 255);
                ((BuiltRectangle) Builder.rectangle()
                    .size(new SizeState(2.0, itemH - 10.0))
                    .color(new QuadColorState(indicator))
                    .radius(new QuadRadiusState(1.0))
                    .smoothness(1.15F)
                    .build()).render(matrix, gx, iy + 5);
            }

            float iconColor = alphaBlend(theme.accent(), open, (int)(220 * hp + 35 * (1 - hp)));
            if (sel) iconColor = alphaBlend(theme.textPrimary(), open, 255);
            int textColor = sel
                ? alphaBlend(theme.textPrimary(), open, 255)
                : alphaBlend(hover ? theme.textPrimary() : theme.textSecondary(), open, 255);

            float iconX = gx + 12f;
            float textX = iconX + 13f;
            float centerY = iy + (itemH - 8.0f) / 2f;

            ((BuiltText) Builder.text()
                .font(FontManager.getFont(cats[i].getFont())).text(cats[i].getIcon())
                .color((int)iconColor).size(10.0F).thickness(0.05F)
                .build()).render(matrix, iconX, centerY);

            ((BuiltText) Builder.text()
                .font(medium).text(cats[i].getName())
                .color(textColor).size(9.0F).thickness(0.05F)
                .build()).render(matrix, textX, centerY);
        }
    }

    private void renderContent(Matrix4f matrix, int gx, int gy, float lx, float ly, float open, Theme theme) {
        int cx = gx + SIDEBAR + 14;
        int cy = gy + 14;
        int cw = W - SIDEBAR - 28;
        int ch = H - 28;

        Category cat = Category.ALL[selectedCategory];

        int catColor = alphaBlend(theme.textPrimary(), open, 255);
        MsdfFont medium = FontManager.SUISSEINTMEDIUM.get();
        ((BuiltText) Builder.text()
            .font(medium).text(cat.getName())
            .color(catColor).size(7.5F).thickness(0.05F)
            .build()).render(matrix, cx, cy);

        int headerLine = (theme.accent() & 0x00FFFFFF) | 0x66000000;
        headerLine = alphaBlend(headerLine, open, 255);
        ((BuiltRectangle) Builder.rectangle()
            .size(new SizeState(12.0, 1.0))
            .color(new QuadColorState(headerLine))
            .radius(new QuadRadiusState(0.0))
            .smoothness(1.15F)
            .build()).render(matrix, cx, cy + 10);

        int headerBottom = cy + 20;

        // Search bar
        int searchX = cx + (cw - 150);
        int searchY = cy - 1;
        int searchW = 150;
        int searchH = 14;
        int searchBg = alphaBlend(0x33000000, open, 255);
        ((BuiltRectangle) Builder.rectangle()
            .size(new SizeState(searchW, searchH))
            .color(new QuadColorState(searchBg))
            .radius(new QuadRadiusState(3.0))
            .smoothness(1.15F)
            .build()).render(matrix, searchX, searchY);
        int searchBorder = alphaBlend((theme.accent() & 0x00FFFFFF) | 0x44000000, open, 255);
        ((BuiltBorder) Builder.border()
            .size(new SizeState(searchW, searchH))
            .color(new QuadColorState(searchBorder))
            .radius(new QuadRadiusState(3.0))
            .thickness(0.015F)
            .smoothness(0.65F, 0.65F)
            .build()).render(matrix, searchX, searchY);
        searchElement.setAnimation(open);
        searchElement.render(matrix, searchX, searchY);

        boolean hasSearch = !searchElement.getValue().isEmpty();

        boolean foundHover = false;
        float contentEnd = headerBottom;

        enableContentScissor(cx, headerBottom, cw, gy + H - 8 - headerBottom);

        if (cat == Category.THEMES) {
            renderThemesTab(matrix, cx, headerBottom, cw, ch - 20, lx, ly, open, contentScroll);
            hoveredModule = null;
            Theme[] themes = MeoRayClient.INSTANCE.getThemeManager().getThemes();
            int cols = 4, gapX = 6, gapY = 8, cardH = 52, cardW = (cw - (cols - 1) * gapX) / cols;
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
                    float visualY = cardY + offsetY;
                    boolean hover = (int) lx >= cardX && (int) lx <= cardX + colW
                        && (int) ly >= visualY && (int) ly <= visualY + cardH;
                    if (hover) { hoveredModule = mod; hoveredModuleCardX = cardX; hoveredModuleCardY = visualY; hoveredModuleCardW = colW; foundHover = true; }

                    float targetOff = hover ? -1f : 0f;
                    if (Math.abs(st.hoverOffset.getValue() - targetOff) > 0.005f)
                        st.hoverOffset.animate(targetOff, 120, Easing.EASE_OUT_CUBIC);

                    renderCard(matrix, cardX, visualY, colW, cardH, mod, hover, open, theme, lx, ly);
                    enableContentScissor(cx, headerBottom, cw, gy + H - 8 - headerBottom);
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

        float contentH = contentEnd - headerBottom;
        float visibleH = gy + H - 8 - headerBottom;
        float maxScroll = Math.max(0, contentH - visibleH);
        float excess = contentScroll < 0 ? contentScroll : contentScroll > maxScroll ? contentScroll - maxScroll : 0;
        if (Math.abs(excess) > 0.5f) {
            contentScroll -= excess * 0.06f;
        } else if (Math.abs(excess) > 0.01f) {
            contentScroll = Math.max(0, Math.min(contentScroll, maxScroll));
        }

        if (maxScroll > 1) {
            float sbX = cx + cw + 4, sbTrackH = ch - 20;
            float sbThumbH = Math.max(12, sbTrackH * (visibleH / contentH));
            float sbThumbY = headerBottom + (contentScroll / maxScroll) * (sbTrackH - sbThumbH);

            int trackCol = alphaBlend(0x30FFFFFF, open, 255);
            ((BuiltRectangle) Builder.rectangle().size(new SizeState(3.0, sbTrackH)).color(new QuadColorState(trackCol)).radius(new QuadRadiusState(1.5)).smoothness(1.15F).build()).render(matrix, sbX, headerBottom);

            int thumbCol = alphaBlend(theme.accent(), open, 180);
            ((BuiltRectangle) Builder.rectangle().size(new SizeState(3.0, sbThumbH)).color(new QuadColorState(thumbCol)).radius(new QuadRadiusState(1.5)).smoothness(1.15F).build()).render(matrix, sbX, sbThumbY);
        }
    }

    private void renderCard(Matrix4f matrix, float x, float y, float w, float h, Module mod, boolean hover, float open, Theme theme, float lx, float ly) {
        ModuleAnimState state = getOrCreateState(mod);
        float highlight = state.highlight.getValue();
        float knobPos = state.knob.getValue();
        float expand = state.expand.getValue();

        int cardOnTop = (theme.accent() & 0x00FFFFFF) | (int)(0x50 * highlight) << 24;
        int cardOnBottom = (theme.accent() & 0x00FFFFFF) | (int)(0x30 * highlight) << 24;
        int cardBgTop = lerpColor(theme.panel(), cardOnTop, 1f);
        int cardBgBottom = lerpColor(theme.panel(), cardOnBottom, 1f);
        if (hover) {
            int hoverTop = (theme.panel() & 0x00FFFFFF) | 0x4A000000;
            int hoverBottom = (theme.panel() & 0x00FFFFFF) | 0x30000000;
            cardBgTop = lerpColor(cardBgTop, hoverTop, 0.5f);
            cardBgBottom = lerpColor(cardBgBottom, hoverBottom, 0.5f);
        }
        cardBgTop = alphaBlend(cardBgTop, open, 255);
        cardBgBottom = alphaBlend(cardBgBottom, open, 255);

        ((BuiltRectangle) Builder.rectangle()
            .size(new SizeState(w, h))
            .color(new QuadColorState(cardBgTop, cardBgTop, cardBgBottom, cardBgBottom))
            .radius(new QuadRadiusState(4.0))
            .smoothness(1.15F)
            .build()).render(matrix, x, y);

        int shadowColor = (theme.bgMain() & 0x00FFFFFF) | 0x28000000;
        shadowColor = alphaBlend(shadowColor, open, 255);
        ((BuiltRectangle) Builder.rectangle()
            .size(new SizeState(w - 2, 1.0))
            .color(new QuadColorState(shadowColor))
            .radius(new QuadRadiusState(0.0))
            .smoothness(1.15F)
            .build()).render(matrix, x + 1, y + 1);
        ((BuiltRectangle) Builder.rectangle()
            .size(new SizeState(w - 2, 1.0))
            .color(new QuadColorState(shadowColor))
            .radius(new QuadRadiusState(0.0))
            .smoothness(1.15F)
            .build()).render(matrix, x + 1, y + h - 1);
        ((BuiltRectangle) Builder.rectangle()
            .size(new SizeState(1.0, h - 2))
            .color(new QuadColorState(shadowColor))
            .radius(new QuadRadiusState(0.0))
            .smoothness(1.15F)
            .build()).render(matrix, x + 1, y + 1);
        ((BuiltRectangle) Builder.rectangle()
            .size(new SizeState(1.0, h - 2))
            .color(new QuadColorState(shadowColor))
            .radius(new QuadRadiusState(0.0))
            .smoothness(1.15F)
            .build()).render(matrix, x + w - 1, y + 1);

        int black = alphaBlend(0xFF000000, open, 255);
        ((BuiltBorder) Builder.border()
            .size(new SizeState(w, h))
            .color(new QuadColorState(black))
            .radius(new QuadRadiusState(4.0))
            .smoothness(0.65F, 0.65F)
            .thickness(0.035F)
            .build()).render(matrix, x, y);

        float headerH = 30;
        MsdfFont medium = FontManager.SUISSEINTMEDIUM.get();
        int textC = alphaBlend(theme.textPrimary(), open, 255);
        ((BuiltText) Builder.text()
            .font(medium).text(mod.getName())
            .color(textC).size(8.0F).thickness(0.05F)
            .build()).render(matrix, x + 8, y + 7);

        float tw = 24;
        float th = 10;
        float tx = x + w - tw - 8;
        float ty = y + (headerH - th) / 2;

        float gearS = 7.0F;
        float gearX = tx - 4 - gearS * 2;
        float gearY = y + (headerH - gearS * 1.5f) / 2;

        boolean isExpanded = expandedModules.getOrDefault(mod.getName(), false);
        int gearColor = isExpanded || expand > 0.5f
            ? alphaBlend(theme.accent(), open, 255)
            : alphaBlend(theme.textPrimary(), open, 200);

        Matrix4f gearMatrix = new Matrix4f(matrix);
        float gcx = gearX + gearS;
        float gcy = gearY + gearS * 0.75f;
        gearMatrix.translate(gcx, gcy, 0);
        gearMatrix.rotate(state.gearAngle.getValue(), 0, 0, 1);
        gearMatrix.translate(-gcx, -gcy, 0);
        ((BuiltText) Builder.text()
            .font(FontManager.ICONS.get()).text("F")
            .color(gearColor).size(7.0F).thickness(0.05F)
            .build()).render(gearMatrix, gearX, gearY);

        int toggleOff = (theme.border() & 0x00FFFFFF) | 0xFF000000;
        int toggleTrack = lerpColor(toggleOff, 0xFF222232, knobPos);
        toggleTrack = alphaBlend(toggleTrack, open, 255);

        ((BuiltRectangle) Builder.rectangle()
            .size(new SizeState(tw, th))
            .color(new QuadColorState(toggleTrack))
            .radius(new QuadRadiusState(4.0))
            .smoothness(1.15F)
            .build()).render(matrix, tx, ty);

        int ks = 7;
        float kx = tx + 2 + ((tw - ks - 4) * knobPos);
        float ky = ty + (th - ks) / 2f;

        int knob = knobPos > 0.5f
            ? alphaBlend(0xFFFFFFFF, open, 255)
            : alphaBlend(0xFFDDDDDD, open, 255);
        int knobBorder = knobPos > 0.5f
            ? alphaBlend((theme.bgMain() & 0x00FFFFFF) | 0xAA000000, open, 255)
            : 0x00000000;

        if (knobPos > 0.5f) {
            ((BuiltRectangle) Builder.rectangle()
                .size(new SizeState(ks + 2, ks + 2))
                .color(new QuadColorState(knobBorder))
                .radius(new QuadRadiusState(3.0))
                .smoothness(1.15F)
                .build()).render(matrix, kx - 1, ky - 1);
        }

        ((BuiltRectangle) Builder.rectangle()
            .size(new SizeState(ks, ks))
            .color(new QuadColorState(knob))
            .radius(new QuadRadiusState(2.5))
            .smoothness(1.15F)
            .build()).render(matrix, kx, ky);

        if (h > 30.5f) {
            renderSettingsContent(matrix, x, y + headerH, w, h - headerH, mod, expand, open, theme, lx, ly);
        }

        float borderHighlight = Math.max(highlight, hover ? 1f : 0f);
        int borderColor = lerpColor(theme.border(), theme.accent(), borderHighlight);
        int cardBorder = alphaBlend(borderColor, open, hover ? 255 : 200);
        ((BuiltBorder) Builder.border()
            .size(new SizeState(w, h))
            .color(new QuadColorState(cardBorder))
            .radius(new QuadRadiusState(4.0))
            .thickness(0.035F)
            .smoothness(0.65F, 0.65F)
            .build()).render(matrix, x, y);

        int blackOut = alphaBlend(0xFF000000, open, 255);
        ((BuiltBorder) Builder.border()
            .size(new SizeState(w, h))
            .color(new QuadColorState(blackOut))
            .radius(new QuadRadiusState(4.0))
            .thickness(0.035F)
            .smoothness(0.65F, 0.65F)
            .build()).render(matrix, x, y);
    }

    private void enableCardScissor(float x, float y, float w, float h) {
        float s = (0.82f + 0.18f * openAnim.getValue()) * MeoRayClient.mainScale;
        float scrX = (width / 2f) * (1f - s) + x * s;
        float scrY = (height / 2f) * (1f - s) + y * s;
        float scrW = w * s;
        float scrH = h * s;
        var window = MinecraftClient.getInstance().getWindow();
        double sf = window.getScaleFactor();
        int fbH = window.getFramebufferHeight();
        int scX = (int) (scrX * sf);
        int scY = fbH - (int) ((scrY + scrH) * sf);
        int scW = Math.max(0, (int) (scrW * sf));
        int scH = Math.max(0, (int) (scrH * sf));
        RenderSystem.enableScissor(scX, scY, scW, scH);
    }

    private void enableWindowScissor(int gx, int gy) {
        float s = (0.82f + 0.18f * openAnim.getValue()) * MeoRayClient.mainScale;
        float scrX = (width / 2f) * (1f - s) + gx * s;
        float scrY = (height / 2f) * (1f - s) + gy * s;
        float scrW = W * s;
        float scrH = H * s;
        var window = MinecraftClient.getInstance().getWindow();
        double sf = window.getScaleFactor();
        int fbH = window.getFramebufferHeight();
        int scX = (int) (scrX * sf);
        int scY = fbH - (int) ((scrY + scrH) * sf);
        int scW = Math.max(0, (int) (scrW * sf));
        int scH = Math.max(0, (int) (scrH * sf));
        RenderSystem.enableScissor(scX, scY, scW, scH);
    }

    private void enableContentScissor(float cx, float cy, float cw, float ch) {
        float s = (0.82f + 0.18f * openAnim.getValue()) * MeoRayClient.mainScale;
        float scrX = (width / 2f) * (1f - s) + cx * s;
        float scrY = (height / 2f) * (1f - s) + cy * s;
        float scrW = cw * s;
        float scrH = ch * s;
        var window = MinecraftClient.getInstance().getWindow();
        double sf = window.getScaleFactor();
        int fbH = window.getFramebufferHeight();
        int scX = (int) (scrX * sf);
        int scY = fbH - (int) ((scrY + scrH) * sf);
        int scW = Math.max(0, (int) (scrW * sf));
        int scH = Math.max(0, (int) (scrH * sf));
        RenderSystem.enableScissor(scX, scY, scW, scH);
    }

    private void renderSettingsContent(Matrix4f matrix, float sx, float sy, float sw, float sh, Module mod, float expand, float open, Theme theme, float lx, float ly) {
        List<Setting<?>> settings = mod.getSettings();
        if (settings.isEmpty()) return;

        boolean locked = !mod.isEnabled();
        float lockAlpha = locked ? 0.275f : 1f;

        boolean isParticles = mod.getName().equals("Particles");
        java.util.List<BooleanSetting> typeSettings = null;
        if (isParticles) {
            typeSettings = new java.util.ArrayList<>();
        }

        int settingsBg = 0xCC08080A;
        ((BuiltRectangle) Builder.rectangle()
            .size(new SizeState(sw, sh))
            .color(new QuadColorState(settingsBg))
            .radius(new QuadRadiusState(0.0, 0.0, 4.0, 4.0))
            .smoothness(1.15F)
            .build()).render(matrix, sx, sy);

        int black = alphaBlend(0xFF000000, open, 255);
        ((BuiltBorder) Builder.border()
            .size(new SizeState(sw, sh))
            .color(new QuadColorState(black))
            .radius(new QuadRadiusState(0.0, 0.0, 4.0, 4.0))
            .smoothness(0.65F, 0.65F)
            .thickness(0.035F)
            .build()).render(matrix, sx, sy);

        int sepColor = alphaBlend(0x00000000, open, 0);
        int sepAlphaMax = (int)(45 * lockAlpha);
        int sepSegs = 20;
        float sepSegW = sw / sepSegs;
        for (int si = 0; si < sepSegs; si++) {
            float sf = (float) si / (sepSegs - 1);
            float sa = sf < 0.5f ? sf * 2 : (1 - sf) * 2;
            int sc = (theme.border() & 0x00FFFFFF) | ((int)(sa * sepAlphaMax) << 24);
            sc = alphaBlend(sc, open, 255);
            ((BuiltRectangle) Builder.rectangle()
                .size(new SizeState(sepSegW + 1, 1.0f))
                .color(new QuadColorState(sc))
                .radius(new QuadRadiusState(0.0))
                .smoothness(1.15F)
                .build()).render(matrix, sx + si * sepSegW, sy);
        }

        MsdfFont medium = FontManager.SUISSEINTMEDIUM.get();
        float padL = 10;
        float padR = 10;
        float settingX = sx + padL;
        float settingW = sw - padL - padR;
        float curY = sy + 4;
        float visibleEnd = sy + sh;

        boolean binding = keybindTarget != null && keybindTarget.equals(mod.getName());
        String keyName = getKeyName(mod.getKey());
        String keyText = binding ? "Keybind: [ ... ]" : "Keybind: [ " + keyName + " ]";
        float keyTextW = medium.getWidth(keyText, 5.5F);
        float keyW = Math.min(keyTextW + 14, settingW);
        float keybindH = 14;

        int keyBg = (theme.border() & 0x00FFFFFF) | (int)(0x40 * lockAlpha) << 24;
        keyBg = alphaBlend(keyBg, open, 255);
        ((BuiltRectangle) Builder.rectangle()
            .size(new SizeState(keyW, keybindH))
            .color(new QuadColorState(keyBg))
            .radius(new QuadRadiusState(3.0))
            .smoothness(1.15F)
            .build()).render(matrix, settingX, curY);

        int keyColor = alphaBlend(binding ? theme.accent() : theme.textPrimary(), open, (int)(220 * lockAlpha));
        ((BuiltText) Builder.text()
            .font(medium).text(keyText)
            .color(keyColor).size(5.5F).thickness(0.04F)
            .build()).render(matrix, settingX + 5, curY + 2.5f);

        curY += keybindH + 4;

        for (Setting<?> setting : settings) {
            if (curY >= visibleEnd) break;

            String name = setting.getName();

            if (setting instanceof NumberSetting ns) {
                double val = ns.getValue();
                double min = ns.getMin();
                double max = ns.getMax();
                float labelSize = 5.5F;
                float sliderH = 3;
                float settingH = labelSize + 3 + sliderH + 6;
                String sliderKey = mod.getName() + "|" + name;

                float targetProgress = (float) Math.min(1, Math.max(0, (val - min) / (max - min)));
                float animProg = sliderAnimatedProgress.getOrDefault(sliderKey, targetProgress);
                animProg += (targetProgress - animProg) * 0.15f;
                sliderAnimatedProgress.put(sliderKey, animProg);

                boolean sliderHovered = !locked && hoveredSliderKey != null && hoveredSliderKey.equals(sliderKey);

                if (curY + settingH >= sy) {
                    int labelColor = alphaBlend(theme.textPrimary(), open, (int)(220 * lockAlpha));
                    ((BuiltText) Builder.text()
                        .font(medium).text(name)
                        .color(labelColor).size(labelSize).thickness(0.04F)
                        .build()).render(matrix, settingX, curY);

                    String valStr = ns.formatValue();
                    float valW = medium.getWidth(valStr, labelSize);
                    int valColor = alphaBlend(theme.accent(), open, (int)(220 * lockAlpha));
                    ((BuiltText) Builder.text()
                        .font(medium).text(valStr)
                        .color(valColor).size(labelSize).thickness(0.04F)
                        .build()).render(matrix, settingX + settingW - valW, curY);

                    float sliderY = curY + labelSize + 3;
                    float trackW = settingW;
                    float trackY = sliderY + (sliderH - 3) / 2f;

                    if (!locked && lx >= settingX && lx <= settingX + trackW
                        && ly >= trackY - 4 && ly <= trackY + 3 + 4) {
                        hoveredSliderKey = sliderKey;
                    }

                    int trackBg = (theme.border() & 0x00FFFFFF) | 0x60000000;
                    trackBg = alphaBlend(trackBg, open, (int)(255 * lockAlpha));
                    ((BuiltRectangle) Builder.rectangle()
                        .size(new SizeState(trackW, 3))
                        .color(new QuadColorState(trackBg))
                        .radius(new QuadRadiusState(1.5f))
                        .smoothness(1.15F)
                        .build()).render(matrix, settingX, trackY);

                    float fillW = trackW * animProg;

                    if (fillW > 1) {
                        int fillColor1 = alphaBlend(theme.accent(), open, (int)(220 * lockAlpha));
                        int fillColor2 = alphaBlend(lerpColor(theme.accent(), 0xFFDDDDDD, 0.3f), open, (int)(200 * lockAlpha));
                        float halfW = fillW * 0.5f;
                        ((BuiltRectangle) Builder.rectangle()
                            .size(new SizeState(halfW, 3))
                            .color(new QuadColorState(fillColor1, fillColor1, fillColor2, fillColor2))
                            .radius(new QuadRadiusState(1.5f, 0, 0, 1.5f))
                            .smoothness(1.15F)
                            .build()).render(matrix, settingX, trackY);
                        if (fillW - halfW > 1) {
                            ((BuiltRectangle) Builder.rectangle()
                                .size(new SizeState(fillW - halfW, 3))
                                .color(new QuadColorState(fillColor2))
                                .radius(new QuadRadiusState(0.0f, 1.5f, 1.5f, 0.0f))
                                .smoothness(1.15F)
                                .build()).render(matrix, settingX + halfW, trackY);
                        }
                    }

                    float knobR = 5;
                    float knobCX = settingX + trackW * animProg;
                    float knobCY = trackY + 1.5f;

                    if (sliderHovered) {
                        int glow = (theme.accent() & 0x00FFFFFF) | 0x32000000;
                        glow = alphaBlend(glow, open, 255);
                        ((BuiltRectangle) Builder.rectangle()
                            .size(new SizeState((knobR + 1.5f) * 2, (knobR + 1.5f) * 2))
                            .color(new QuadColorState(glow))
                            .radius(new QuadRadiusState(knobR + 1.5f))
                            .smoothness(1.15F)
                            .build()).render(matrix, knobCX - knobR - 1.5f, knobCY - knobR - 1.5f);
                    }

                    int knobBg = alphaBlend(theme.accent(), open, (int)(255 * lockAlpha));
                    ((BuiltRectangle) Builder.rectangle()
                        .size(new SizeState(knobR * 2, knobR * 2))
                        .color(new QuadColorState(knobBg))
                        .radius(new QuadRadiusState(knobR))
                        .smoothness(1.15F)
                        .build()).render(matrix, knobCX - knobR, knobCY - knobR);
                }
                curY += settingH + 2;

            } else if (setting instanceof BooleanSetting bs) {
                boolean val = bs.getValue();
                String bsName = setting.getName();

                if (isParticles && bsName.startsWith("Type ")) {
                    typeSettings.add(bs);
                    continue;
                }

                float textW = medium.getWidth(bsName, 5.5F);
                float pillH = 16;
                float pillW = Math.min(textW + 20, settingW);
                float settingH = pillH + 6;

                if (curY + settingH >= sy) {
                    ModuleAnimState mst = moduleAnims.get(mod.getName());
                    String sKey = setting.getName();
                    if (mst != null && !mst.settingAnims.containsKey(sKey)) {
                        mst.settingAnims.put(sKey, new AnimatedFloat(val ? 1f : 0f));
                    }
                    AnimatedFloat sAnim = mst != null ? mst.settingAnims.get(sKey) : null;
                    float animVal = sAnim != null ? sAnim.update() : (val ? 1f : 0f);

                    int offBg = 0xFF202024;
                    int onBg = (theme.accent() & 0x00FFFFFF) | 0xB4000000;
                    int fillBg = lerpColor(offBg, onBg, animVal);
                    fillBg = alphaBlend(fillBg, open, 255);

                    ((BuiltRectangle) Builder.rectangle()
                        .size(new SizeState(pillW, pillH))
                        .color(new QuadColorState(fillBg))
                        .radius(new QuadRadiusState(4.0))
                        .smoothness(1.15F)
                        .build()).render(matrix, settingX, curY);

                    if (animVal > 0.01f) {
                        float checkScale = 0.3f + 0.7f * animVal;
                        float checkSize = 5.5f * checkScale;
                        int checkColor = alphaBlend(0xFF111111, open, (int)(255 * animVal));
                        ((BuiltText) Builder.text()
                            .font(medium).text("\u2713")
                            .color(checkColor).size(checkSize).thickness(0.04F)
                            .build()).render(matrix, settingX + 10, curY + (pillH - checkSize) / 2f);
                    }

                    int pillTextColor = alphaBlend(theme.textPrimary(), open, (int)(220 * lockAlpha));
                    float textOffset = 8f;
                    ((BuiltText) Builder.text()
                        .font(medium).text(bsName)
                        .color(pillTextColor).size(5.5F).thickness(0.04F)
                        .build()).render(matrix, settingX + textOffset, curY + (pillH - 5.5F) / 2f);
                }
                curY += settingH + 2;

            } else if (setting instanceof ModeSetting ms) {
                String current = ms.getValue();
                float settingH = 14 + 6;

                if (curY + settingH >= sy) {
                    String modeText = name + ": " + current;
                    float modeTextW = medium.getWidth(modeText, 5.5F);
                    float modeW = Math.min(modeTextW + 14, settingW);

                    int modeBg = (theme.border() & 0x00FFFFFF) | (int)(0x40 * lockAlpha) << 24;
                    modeBg = alphaBlend(modeBg, open, 255);
                    ((BuiltRectangle) Builder.rectangle()
                        .size(new SizeState(modeW, 14))
                        .color(new QuadColorState(modeBg))
                        .radius(new QuadRadiusState(3.0))
                        .smoothness(1.15F)
                        .build()).render(matrix, settingX, curY);

                    int modeColor = alphaBlend(theme.textPrimary(), open, (int)(220 * lockAlpha));
                    ((BuiltText) Builder.text()
                        .font(medium).text(modeText)
                        .color(modeColor).size(5.5F).thickness(0.04F)
                        .build()).render(matrix, settingX + 5, curY + 2.5f);
                }
                curY += settingH + 2;
            }
        }

        if (typeSettings != null && !typeSettings.isEmpty()) {
            int cols = 3;
            float gridW = settingW;
            float cellW = gridW / cols;
            float cellH = 20;
            float gridStartY = Math.max(curY + 4, sy + 4);
            float gridH = ((typeSettings.size() + cols - 1) / cols) * cellH + 4;

            int gridBg = (theme.border() & 0x00FFFFFF) | 0x24000000;
            gridBg = alphaBlend(gridBg, open, 255);
            ((BuiltRectangle) Builder.rectangle()
                .size(new SizeState(gridW, gridH))
                .color(new QuadColorState(gridBg))
                .radius(new QuadRadiusState(4.0))
                .smoothness(1.15F)
                .build()).render(matrix, settingX, gridStartY);

            for (int ti = 0; ti < typeSettings.size(); ti++) {
                BooleanSetting ts = typeSettings.get(ti);
                int tCol = ti % cols;
                int tRow = ti / cols;
                float cellX = settingX + tCol * cellW + 2;
                float cellY = gridStartY + 2 + tRow * cellH;
                float cellDrawW = cellW - 4;
                float cellDrawH = cellH - 2;

                boolean tVal = ts.getValue();
                String tLabel = ts.getName().substring(5);

                ModuleAnimState mst = moduleAnims.get(mod.getName());
                String sKey = ts.getName();
                if (mst != null && !mst.settingAnims.containsKey(sKey)) {
                    mst.settingAnims.put(sKey, new AnimatedFloat(tVal ? 1f : 0f));
                }
                AnimatedFloat sAnim = mst != null ? mst.settingAnims.get(sKey) : null;
                float animVal = sAnim != null ? sAnim.update() : (tVal ? 1f : 0f);

                int offBg = 0xFF202024;
                int onBg = (theme.accent() & 0x00FFFFFF) | 0xB4000000;
                int fillBg = lerpColor(offBg, onBg, animVal);
                fillBg = alphaBlend(fillBg, open, 255);

                ((BuiltRectangle) Builder.rectangle()
                    .size(new SizeState(cellDrawW, cellDrawH))
                    .color(new QuadColorState(fillBg))
                    .radius(new QuadRadiusState(3.0))
                    .smoothness(1.15F)
                    .build()).render(matrix, cellX, cellY);

                if (animVal > 0.01f) {
                    float checkScale = 0.3f + 0.7f * animVal;
                    float checkSize = 5.5f * checkScale;
                    int checkColor = alphaBlend(0xFF111111, open, (int)(255 * animVal));
                    ((BuiltText) Builder.text()
                        .font(medium).text("\u2713")
                        .color(checkColor).size(checkSize).thickness(0.04F)
                        .build()).render(matrix, cellX + 4, cellY + (cellDrawH - checkSize) / 2f);
                }

                int txtCol = alphaBlend(theme.textPrimary(), open, (int)(220 * lockAlpha));
                float labelOffsetX = animVal > 0.01f ? 10f : 4f;
                ((BuiltText) Builder.text()
                    .font(medium).text(tLabel)
                    .color(txtCol).size(5.5F).thickness(0.04F)
                    .build()).render(matrix, cellX + labelOffsetX, cellY + (cellDrawH - 5.5F) / 2f);
            }
        }

        curY += 8;
    }

    private void renderThemesTab(Matrix4f matrix, int ox, int oy, int w, int h, float lx, float ly, float open, float scrollOffset) {
        ThemeManager mgr = MeoRayClient.INSTANCE.getThemeManager();
        Theme[] themes = mgr.getThemes();
        int selected = mgr.getSelectedIndex();

        int cols = 4;
        int gapX = 6;
        int gapY = 8;
        int cardW = (w - (cols - 1) * gapX) / cols;
        int cardH = 52;

        for (int i = 0; i < themes.length; i++) {
            int col = i % cols;
            int row = i / cols;
            int cx = ox + col * (cardW + gapX);
            int cy = (int)(oy + row * (cardH + gapY) - scrollOffset);

            boolean hover = (int) lx >= cx && (int) lx <= cx + cardW
                && (int) ly >= cy && (int) ly <= cy + cardH;
            boolean sel = i == selected;

            drawThemeCard(matrix, cx, cy, cardW, cardH, themes[i], sel, hover, open);
        }
    }

    private void drawThemeCard(Matrix4f matrix, int x, int y, int w, int h, Theme theme, boolean selected, boolean hover, float open) {
        int bg = alphaBlend(0x640F0F14, open, 255);
        if (hover) {
            int hoverBg = (theme.accent() & 0x00FFFFFF) | 0x22000000;
            bg = alphaBlend(hoverBg, open, 255);
        }

        ((BuiltRectangle) Builder.rectangle()
            .size(new SizeState(w, h))
            .color(new QuadColorState(bg))
            .radius(new QuadRadiusState(4.0))
            .smoothness(1.15F)
            .build()).render(matrix, x, y);

        int cardBorder = selected
            ? alphaBlend(theme.accent(), open, 200)
            : alphaBlend(theme.border(), open, 160);
        ((BuiltBorder) Builder.border()
            .size(new SizeState(w, h))
            .color(new QuadColorState(cardBorder))
            .radius(new QuadRadiusState(4.0))
            .thickness(0.02F)
            .smoothness(0.65F, 0.65F)
            .build()).render(matrix, x, y);

        int previewH = 28;
        int previewY = y + 4;
        int previewX = x + 4;
        int previewW = w - 8;

        int previewBg = alphaBlend((theme.bgMain() & 0x00FFFFFF) | 0xCC000000, open, 255);
        ((BuiltRectangle) Builder.rectangle()
            .size(new SizeState(previewW, previewH))
            .color(new QuadColorState(previewBg))
            .radius(new QuadRadiusState(3.0))
            .smoothness(1.15F)
            .build()).render(matrix, previewX, previewY);

        int accentLine = alphaBlend(theme.accent(), open, 255);
        ((BuiltRectangle) Builder.rectangle()
            .size(new SizeState(3, previewH - 6))
            .color(new QuadColorState(accentLine))
            .radius(new QuadRadiusState(1.0))
            .smoothness(1.15F)
            .build()).render(matrix, previewX + 4, previewY + 3);

        int barY = previewY + 6;
        int barX = previewX + 11;
        int barH = 3;
        int barGap = 4;

        int textPrimaryColor = alphaBlend(theme.textPrimary(), open, 200);
        ((BuiltRectangle) Builder.rectangle()
            .size(new SizeState(previewW - 18, barH))
            .color(new QuadColorState(textPrimaryColor))
            .radius(new QuadRadiusState(1.0))
            .smoothness(1.15F)
            .build()).render(matrix, barX, barY);

        int textSecondaryColor = alphaBlend(theme.textSecondary(), open, 160);
        ((BuiltRectangle) Builder.rectangle()
            .size(new SizeState(previewW - 24, barH))
            .color(new QuadColorState(textSecondaryColor))
            .radius(new QuadRadiusState(1.0))
            .smoothness(1.15F)
            .build()).render(matrix, barX, barY + barH + barGap);

        int accentBarColor = alphaBlend(theme.accent(), open, 255);
        float accentBarW = (previewW - 18) * 0.55f;
        ((BuiltRectangle) Builder.rectangle()
            .size(new SizeState(accentBarW, barH))
            .color(new QuadColorState(accentBarColor))
            .radius(new QuadRadiusState(1.0))
            .smoothness(1.15F)
            .build()).render(matrix, barX, barY + (barH + barGap) * 2);

        int toggleBgColor = alphaBlend(theme.border(), open, 120);
        float toggleW = 12;
        float toggleH = 5;
        float toggleX = previewX + previewW - toggleW - 4;
        float toggleY = previewY + (previewH - toggleH) / 2f;
        ((BuiltRectangle) Builder.rectangle()
            .size(new SizeState(toggleW, toggleH))
            .color(new QuadColorState(toggleBgColor))
            .radius(new QuadRadiusState(toggleH / 2f))
            .smoothness(1.15F)
            .build()).render(matrix, toggleX, toggleY);

        int knobColor = alphaBlend(theme.accent(), open, 200);
        float knobMiniR = 3;
        ((BuiltRectangle) Builder.rectangle()
            .size(new SizeState(knobMiniR * 2, knobMiniR * 2))
            .color(new QuadColorState(knobColor))
            .radius(new QuadRadiusState(knobMiniR))
            .smoothness(1.15F)
            .build()).render(matrix, toggleX + toggleW - knobMiniR * 2, toggleY + (toggleH - knobMiniR * 2) / 2f);

        MsdfFont medium = FontManager.SUISSEINTMEDIUM.get();
        int textColor = alphaBlend(0xFFCCCCCC, open, 255);
        ((BuiltText) Builder.text()
            .font(medium).text(theme.name())
            .color(textColor).size(5.5F).thickness(0.05F)
            .build()).render(matrix, x + 5, previewY + previewH + 5);

        int rbSize = 7;
        int rbX = x + w - rbSize - 5;
        int rbY = y + 5;
        int rbColor = selected
            ? alphaBlend(theme.accent(), open, 255)
            : alphaBlend(0x664A6A8A, open, 200);

        ((BuiltRectangle) Builder.rectangle()
            .size(new SizeState(rbSize, rbSize))
            .color(new QuadColorState(rbColor))
            .radius(new QuadRadiusState(rbSize / 2f))
            .smoothness(1.15F)
            .build()).render(matrix, rbX, rbY);

        if (selected) {
            int checkColor = alphaBlend(0xFFFFFFFF, open, 255);
            ((BuiltText) Builder.text()
                .font(medium).text("\u2713")
                .color(checkColor).size(5.0F).thickness(0.04F)
                .build()).render(matrix, rbX + 1.5f, rbY - 0.5f);
        }
    }

    private void drawThemePreview(Matrix4f matrix, int x, int y, Theme theme, float open) {
        int pw = 50;
        int ph = 18;

        int panelBg = alphaBlend(0xCC0A0A0F, open, 255);
        ((BuiltRectangle) Builder.rectangle()
            .size(new SizeState(pw, ph))
            .color(new QuadColorState(panelBg))
            .radius(new QuadRadiusState(2.0))
            .smoothness(1.15F)
            .build()).render(matrix, x, y);

        int accentLine = alphaBlend(theme.accent(), open, 255);
        ((BuiltRectangle) Builder.rectangle()
            .size(new SizeState(3, ph))
            .color(new QuadColorState(accentLine))
            .radius(new QuadRadiusState(1.0))
            .smoothness(1.15F)
            .build()).render(matrix, x, y);

        int textPrimaryLine = alphaBlend(theme.textPrimary(), open, 180);
        int textSecondaryLine = alphaBlend(theme.textSecondary(), open, 180);

        int lx = x + 7;
        ((BuiltRectangle) Builder.rectangle()
            .size(new SizeState(28, 1))
            .color(new QuadColorState(accentLine))
            .radius(new QuadRadiusState(0.5))
            .smoothness(1.15F)
            .build()).render(matrix, lx, y + 3);

        ((BuiltRectangle) Builder.rectangle()
            .size(new SizeState(20, 1))
            .color(new QuadColorState(textPrimaryLine))
            .radius(new QuadRadiusState(0.5))
            .smoothness(1.15F)
            .build()).render(matrix, lx, y + 8);

        ((BuiltRectangle) Builder.rectangle()
            .size(new SizeState(34, 1))
            .color(new QuadColorState(textSecondaryLine))
            .radius(new QuadRadiusState(0.5))
            .smoothness(1.15F)
            .build()).render(matrix, lx, y + 13);
    }

    private boolean tooltipHoverPrev;

    private void drawTooltip(Matrix4f matrix, float gx, float gy, float lx, float ly, float open, Theme theme) {
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
        float tooltipWidth = textWidth + 24f;
        float tooltipHeight = textSize + 12f;

        float animAlpha = open * tt;

        float tooltipX = gx + (W / 2f) - (tooltipWidth / 2f);
        float tooltipY = gy - tooltipHeight - 6f;

        if (tt > 0.05f) {
            ((BuiltBlur) Builder.blur()
                .size(new SizeState(tooltipWidth, tooltipHeight))
                .radius(new QuadRadiusState(4.0))
                .color(new QuadColorState(0xFFFFFFFF))
                .blurRadius(6.0f)
                .smoothness(1.15f)
                .build()).render(matrix, tooltipX, tooltipY, 0);
        }

        int ttBg = alphaBlend(0xCC0A0A0F, animAlpha, 255);
        ((BuiltRectangle) Builder.rectangle()
            .size(new SizeState(tooltipWidth, tooltipHeight))
            .color(new QuadColorState(ttBg))
            .radius(new QuadRadiusState(4.0))
            .smoothness(1.15F)
            .build()).render(matrix, tooltipX, tooltipY);

        int ttBorder = alphaBlend(theme.accent(), animAlpha, 80);
        ((BuiltBorder) Builder.border()
            .size(new SizeState(tooltipWidth, tooltipHeight))
            .color(new QuadColorState(ttBorder))
            .radius(new QuadRadiusState(4.0))
            .thickness(0.02F)
            .smoothness(0.65F, 0.65F)
            .build()).render(matrix, tooltipX, tooltipY);

        float textX = tooltipX + (tooltipWidth - textWidth) / 2f - 2f;
        float baselineH = medium.getMetrics().baselineHeight();
        float asc = medium.getMetrics().ascender();
        float textY = tooltipY + tooltipHeight / 2f - (baselineH - asc * 0.5f) * textSize - 1f;
        int ttText = alphaBlend(0xFFCCCCCC, animAlpha, 255);
        ((BuiltText) Builder.text()
            .font(medium).text(desc)
            .color(ttText).size(textSize).thickness(0.04F)
            .build()).render(matrix, textX, textY);
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
        for (AnimatedFloat af : state.settingAnims.values()) {
            af.animate(target, TOGGLE_DURATION, Easing.EASE_OUT_CUBIC);
        }
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
        float h = 4;
        boolean binding = keybindTarget != null && keybindTarget.equals(mod.getName());
        String keyName = getKeyName(mod.getKey());
        String keyText = binding ? "Keybind: [ ... ]" : "Keybind: [ " + keyName + " ]";
        float keyTextW = FontManager.SUISSEINTMEDIUM.get().getWidth(keyText, 5.5F);
        h += 14 + 4; // keybind
        int typeCount = 0;
        for (Setting<?> s : settings) {
            if (mod.getName().equals("Particles") && s instanceof BooleanSetting && s.getName().startsWith("Type ")) {
                typeCount++;
                continue;
            }
            if (s instanceof NumberSetting) {
                h += 18;
            } else if (s instanceof BooleanSetting) {
                h += 22;
            } else if (s instanceof ModeSetting) {
                h += 20;
            }
            h += 2;
        }
        if (typeCount > 0) {
            int cols = 3;
            float cellH = 20;
            int rows = (typeCount + cols - 1) / cols;
            h += 4 + rows * cellH + 4;
        }
        h += 8;
        return h;
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
        float ms = MeoRayClient.mainScale;
        float scale = (0.82f + 0.18f * open) * ms;
        float cx = width / 2f;
        float cy = height / 2f;

        float lx = (float) ((mx - cx * (1 - scale)) / scale);
        float ly = (float) ((my - cy * (1 - scale)) / scale);

        float gx = (width - W) / 2f;
        float gy = (height - H) / 2f;

        // Search bar click
        if (button == 0) {
            float searchX = gx + SIDEBAR + 14 + (W - SIDEBAR - 28 - 150);
            float searchY = gy + 14 - 1;
            if (lx >= searchX && lx <= searchX + 150 && ly >= searchY && ly <= searchY + 14) {
                searchElement.handleClick();
                return true;
            }
            searchElement.setSelect(false);
        }

        if (lx >= gx && lx <= gx + SIDEBAR && ly >= gy && ly <= gy + H) {
            int sepY = (int) gy + 31;
            int itemH = 28;
            int startY = sepY + 8;
            int idx = (int) ((ly - startY) / itemH);
            if (idx >= 0 && idx < Category.ALL.length) {
                selectedCategory = idx;
                persistedCategory = idx;
                return true;
            }
        }

        if (Category.ALL[selectedCategory] == Category.THEMES) {
            if (button == 0) return handleThemesClick(lx, ly, gx, gy);
            return super.mouseClicked(mx, my, button);
        }

        if (Category.ALL[selectedCategory] == Category.SETTINGS) {
            if (button != 0) return super.mouseClicked(mx, my, button);
            int ox = (int) (gx + SIDEBAR + 14);
            int oy = (int) (gy + 14 + 20);
            int w = W - SIDEBAR - 28;

            float sliderY = oy + 10 - contentScroll;
            float trackH = 12;
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
            float dropdownW = w;

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
                        if (mi == 0) snowOn = !snowOn;
                        else if (mi == 1) starsOn = !starsOn;
                        else if (mi == 2) bubblesOn = !bubblesOn;
                        particlesInit = false;
                        return true;
                    }
                }
                effectsDropdownOpen = false;
            }

            return super.mouseClicked(mx, my, button);
        }

        int cxx = (int) (gx + SIDEBAR + 14);
        int cyy = (int) (gy + 14);
        int cw = W - SIDEBAR - 28;
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
                if (handleSettingsClick(mod, lx, ly, cardX, cardY, colW, open)) {
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
        float sx = cardX + 10;
        float sy = cardY + 34;
        float settingW = cardW - 20;
        float curY = sy;

        boolean isParticles = mod.getName().equals("Particles");

        boolean binding = keybindTarget != null && keybindTarget.equals(mod.getName());
        String keyName = getKeyName(mod.getKey());
        String keyText = binding ? "Keybind: [ ... ]" : "Keybind: [ " + keyName + " ]";
        float keyW = Math.min(FontManager.SUISSEINTMEDIUM.get().getWidth(keyText, 5.5F) + 14, settingW);
        float keybindH = 14;
        if (ly >= curY && ly <= curY + keybindH && lx >= sx && lx <= sx + keyW) {
            boolean locked = !mod.isEnabled();
            if (!locked) {
                if (binding) {
                    keybindTarget = null;
                    mod.setKey(0);
                } else {
                    keybindTarget = mod.getName();
                }
            }
            return true;
        }
        curY += keybindH + 4;

        java.util.List<BooleanSetting> typeSettings = null;
        if (isParticles) {
            typeSettings = new java.util.ArrayList<>();
        }

        for (Setting<?> setting : settings) {
            if (setting instanceof NumberSetting ns) {
                float labelH = 5.5F;
                float sliderY = curY + labelH + 3;
                float sliderHitH = 12;

                if (ly >= sliderY - 4 && ly <= sliderY - 4 + sliderHitH && lx >= sx && lx <= sx + settingW) {
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
                curY += 22 + 2;

            } else if (setting instanceof BooleanSetting bs) {
                if (isParticles && bs.getName().startsWith("Type ")) {
                    typeSettings.add(bs);
                    continue;
                }
                float pillH = 16;
                float textW = FontManager.SUISSEINTMEDIUM.get().getWidth(bs.getName(), 5.5F);
                float pillW = Math.min(textW + 20, settingW);
                if (ly >= curY && ly <= curY + pillH && lx >= sx && lx <= sx + pillW) {
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
                    return true;
                }
                curY += 22 + 2;

            } else if (setting instanceof ModeSetting ms) {
                String modeText = ms.getName() + ": " + ms.getValue();
                float modeW = Math.min(FontManager.SUISSEINTMEDIUM.get().getWidth(modeText, 5.5F) + 14, settingW);
                if (ly >= curY && ly <= curY + 14 && lx >= sx && lx <= sx + modeW) {
                    ms.cycle();
                    return true;
                }
                curY += 20 + 2;
            }
        }

        if (typeSettings != null && !typeSettings.isEmpty()) {
            int cols = 3;
            float cellW = settingW / cols;
            float cellH = 20;
            float gridStartY = Math.max(curY + 4, sy + 4);
            for (int ti = 0; ti < typeSettings.size(); ti++) {
                int tCol = ti % cols;
                int tRow = ti / cols;
                float cellX = sx + tCol * cellW + 2;
                float cellY = gridStartY + 2 + tRow * cellH;
                float cellDrawW = cellW - 4;
                float cellDrawH = cellH - 2;
                if (lx >= cellX && lx <= cellX + cellDrawW && ly >= cellY && ly <= cellY + cellDrawH) {
                    BooleanSetting ts = typeSettings.get(ti);
                    boolean oldVal = ts.getValue();
                    ts.toggle();
                    boolean newVal = !oldVal;
                    ModuleAnimState mst = moduleAnims.get(mod.getName());
                    if (mst != null) {
                        AnimatedFloat sa = mst.settingAnims.get(ts.getName());
                        if (sa == null) {
                            sa = new AnimatedFloat(oldVal ? 1f : 0f);
                            mst.settingAnims.put(ts.getName(), sa);
                        } else {
                            sa.snapTo(oldVal ? 1f : 0f);
                        }
                        sa.animate(newVal ? 1f : 0f, 200, Easing.EASE_OUT_CUBIC);
                    }
                    return true;
                }
            }
        }

        return false;
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
        int cxx = (int) (gx + SIDEBAR + 14);
        int cyy = (int) (gy + 14);
        int cw = W - SIDEBAR - 28;
        List<Module> modules = MeoRayClient.INSTANCE.moduleManager.getByCategory(Category.ALL[selectedCategory]);

        int gap = 5;
        int colW = (cw - gap) / 2;

        int[] moduleCol = new int[modules.size()];
        for (int i = 0; i < modules.size(); i++) {
            moduleCol[i] = i % 2;
        }

        float[] colYs = { cyy + 20, cyy + 20 };

        for (int i = 0; i < modules.size(); i++) {
            Module m = modules.get(i);
            int col = moduleCol[i];
            if (!m.getName().equals(draggingSliderMod)) {
                float h = getCardAnimatedHeight(m);
                colYs[col] = colYs[col] + h + gap;
                continue;
            }

            float cardH = getCardAnimatedHeight(m);
            float cardX = cxx + col * (colW + gap);
            float cardY = colYs[col];

            float sx = cardX + 10;
            float settingW = colW - 20;
            float curY = cardY + 34;

            for (Setting<?> setting : m.getSettings()) {
                if (setting instanceof NumberSetting ns && setting.getName().equals(draggingSliderName)) {
                    float progress = (lx - sx) / settingW;
                    progress = Math.max(0, Math.min(1, progress));
                    double range = ns.getMax() - ns.getMin();
                    double raw = ns.getMin() + range * progress;
                    double stepped = Math.round(raw / ns.getStep()) * ns.getStep();
                    ns.setValue(Math.max(ns.getMin(), Math.min(ns.getMax(), stepped)));
                    break;
                }
                if (setting instanceof NumberSetting) curY += 22 + 2;
                else if (setting instanceof BooleanSetting) curY += 22 + 2;
                else if (setting instanceof ModeSetting) curY += 20 + 2;
            }
            break;
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        float gx = (width - W) / 2f;
        float gy = (height - H) / 2f;
        float scrollAreaTop = gy + 14 + 20;
        float scrollAreaBottom = gy + H - 8;
        float open = openAnim.getValue();
        float ms = MeoRayClient.mainScale;
        float scale = (0.82f + 0.18f * open) * ms;
        float cx = width / 2f;
        float cy = height / 2f;

        float ly = (float) ((mouseY - cy * (1 - scale)) / scale);
        if (ly < scrollAreaTop || ly > scrollAreaBottom) {
            return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
        }
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
        }
        return super.mouseReleased(mx, my, button);
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double deltaX, double deltaY) {
        if (button == 0 && draggingMainScale) {
            float open = openAnim.getValue();
            float ms = MeoRayClient.mainScale;
            float scale = (0.82f + 0.18f * open) * ms;
            float cx = width / 2f;
            float cy = height / 2f;
            float lx = (float) ((mx - cx * (1 - scale)) / scale);
            float gx = (width - W) / 2f;
            int ox = (int) (gx + SIDEBAR + 14);
            int w = W - SIDEBAR - 28;
            float prog = (lx - ox) / w;
            prog = Math.max(0, Math.min(1, prog));
            mainScaleSetting = 0.5f + prog * 1.0f;
            MeoRayClient.mainScale = mainScaleSetting;
            return true;
        }
        return super.mouseDragged(mx, my, button, deltaX, deltaY);
    }

    private boolean handleThemesClick(float lx, float ly, float gx, float gy) {
        int ox = (int) (gx + SIDEBAR + 14);
        int oy = (int) (gy + 14 + 20);
        int w = W - SIDEBAR - 28;

        int cols = 4;
        int gapX = 6;
        int gapY = 6;
        int cardW = (w - (cols - 1) * gapX) / cols;
        int cardH = 48;

        ThemeManager mgr = MeoRayClient.INSTANCE.getThemeManager();
        Theme[] themes = mgr.getThemes();
        for (int i = 0; i < themes.length; i++) {
            int col = i % cols;
            int row = i / cols;
            int cx = ox + col * (cardW + gapX);
            int cy = (int)(oy + row * (cardH + gapY) - contentScroll);

            if ((int) lx >= cx && (int) lx <= cx + cardW
                && (int) ly >= cy && (int) ly <= cy + cardH) {
                MeoRayClient.INSTANCE.getThemeManager().select(i);
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
                openAnim.animate(0f, CLOSE_DURATION, Easing.EASE_IN_OUT_QUINT);
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
                p.x = rand.nextFloat() * (W - SIDEBAR);
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
                p.x = rand.nextFloat() * (W - SIDEBAR);
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
                p.x = rand.nextFloat() * (W - SIDEBAR);
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
                p.x = rand.nextFloat() * (W - SIDEBAR);
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

        StringBuilder labelBuilder = new StringBuilder();
        String[][] effectData = {{"Snow", "\u2744"}, {"Stars", "\u2605"}, {"Bubbles", "\u25CB"}};
        boolean[] states = {snowOn, starsOn, bubblesOn};
        for (int mi = 0; mi < 3; mi++) {
            if (states[mi]) {
                if (labelBuilder.length() > 0) labelBuilder.append(", ");
                labelBuilder.append(effectData[mi][0]);
            }
        }
        String dropdownLabel = labelBuilder.length() > 0 ? labelBuilder.toString() : "None";

        int dropBg = 0xCC18181C;
        dropBg = alphaBlend(dropBg, open, 255);
        ((BuiltRectangle) Builder.rectangle()
            .size(new SizeState(w, btnH))
            .color(new QuadColorState(dropBg))
            .radius(new QuadRadiusState(4.0))
            .smoothness(1.15F)
            .build()).render(matrix, dropdownX, dropdownY);

        float dl = medium.getWidth(dropdownLabel, 5.5F);
        int dlCol = alphaBlend(0xFFCCCCCC, open, 255);
        ((BuiltText) Builder.text()
            .font(medium).text(dropdownLabel)
            .color(dlCol).size(5.5F).thickness(0.04F)
            .build()).render(matrix, dropdownX + 5, dropdownY + (btnH - 5.5F) / 2);

        String arrow = effectsDropdownOpen ? "\u25B2" : "\u25BC";
        float aw = medium.getWidth(arrow, 5.0F);
        int arrCol = alphaBlend(0xFF888896, open, 255);
        ((BuiltText) Builder.text()
            .font(medium).text(arrow)
            .color(arrCol).size(5.0F).thickness(0.04F)
            .build()).render(matrix, dropdownX + w - aw - 5, dropdownY + (btnH - 5.0F) / 2);

        float ddAnim = effectsDropdownAnim.update();
        float ddItemH = 13;
        float ddFullH = 3 * ddItemH;
        float ddH = ddFullH * ddAnim;
        int ddBg = 0xCC202024;
        ddBg = alphaBlend(ddBg, open, 255);

        ((BuiltRectangle) Builder.rectangle()
            .size(new SizeState(w, ddH))
            .color(new QuadColorState(ddBg))
            .radius(new QuadRadiusState(4.0))
            .smoothness(1.15F)
            .build()).render(matrix, dropdownX, dropdownY + btnH + 2);

        if (ddH > 1) {
            int ddBorder = alphaBlend(0xFF000000, open, 255);
            ((BuiltBorder) Builder.border()
                .size(new SizeState(w, ddH))
                .color(new QuadColorState(ddBorder))
                .radius(new QuadRadiusState(4.0))
                .thickness(0.025F)
                .smoothness(0.65F, 0.65F)
                .build()).render(matrix, dropdownX, dropdownY + btnH + 2);
        }

        for (int mi = 0; mi < 3; mi++) {
            float itemAlpha = (ddAnim * 3 - mi) / 3f;
            itemAlpha = Math.max(0, Math.min(1, itemAlpha * 3));
            if (itemAlpha < 0.01f) continue;
            float iy = dropdownY + btnH + 2 + mi * ddItemH;
            if (iy - (dropdownY + btnH + 2) > ddH) continue;
            boolean sel = states[mi];
            int itemBg = sel
                ? alphaBlend((theme.accent() & 0x00FFFFFF) | 0x30000000, open, 255)
                : 0;
            if (sel) {
                double tlr = mi == 0 ? 4.0 : 0.0;
                double brr = mi == 2 ? 4.0 : 0.0;
                ((BuiltRectangle) Builder.rectangle()
                    .size(new SizeState(w, ddItemH))
                    .color(new QuadColorState(itemBg))
                    .radius(new QuadRadiusState(tlr, 0.0, 0.0, brr))
                    .smoothness(1.15F)
                    .build()).render(matrix, dropdownX, iy);
            }

            int itemAlphaI = (int)(255 * itemAlpha);
            String check = sel ? "\u2713" : "  ";
            float cw2 = medium.getWidth(check, 5.0F);
            int cCol = sel ? (theme.accent() & 0x00FFFFFF) | (itemAlphaI << 24) : 0;
            if (sel) {
                ((BuiltText) Builder.text()
                    .font(medium).text(check)
                    .color(cCol).size(5.0F).thickness(0.04F)
                    .build()).render(matrix, dropdownX + 4, iy + (ddItemH - 5.0F) / 2);
            }

            float txOff = sel ? 4 + cw2 + 4 : 4;
            int tCol = alphaBlend(sel ? 0xFFFFFFFF : 0xFF999999, open, (int)(255 * itemAlpha));
            ((BuiltText) Builder.text()
                .font(medium).text(effectData[mi][0])
                .color(tCol).size(5.0F).thickness(0.04F)
                .build()).render(matrix, dropdownX + txOff, iy + (ddItemH - 5.0F) / 2);
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

    private static int darkenColor(int color, float factor) {
        int r = (int) (((color >> 16) & 0xFF) * factor);
        int g = (int) (((color >> 8) & 0xFF) * factor);
        int b = (int) ((color & 0xFF) * factor);
        return (color & 0xFF000000) | (r << 16) | (g << 8) | b;
    }
}
