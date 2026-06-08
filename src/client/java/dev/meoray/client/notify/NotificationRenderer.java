package dev.meoray.client.notify;

import dev.meoray.client.gui.theme.Theme;
import dev.meoray.client.managers.FontManager;
import dev.meoray.client.util.render.builders.Builder;
import dev.meoray.client.util.render.builders.states.QuadColorState;
import dev.meoray.client.util.render.builders.states.QuadRadiusState;
import dev.meoray.client.util.render.builders.states.SizeState;
import dev.meoray.client.util.render.msdf.MsdfFont;
import dev.meoray.client.util.render.renderers.impl.BuiltRectangle;
import dev.meoray.client.util.render.renderers.impl.BuiltText;
import dev.meoray.client.MeoRayClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import org.joml.Matrix4f;

import java.util.List;

public class NotificationRenderer {
    private static final int BG_BASE = 0xCC14141A;
    private static final int PANEL_BASE = 0xFF1A1A1F;
    private static final int ICON_BG = 0xFF1A1A1F;
    private static final int WHITE = 0xFFFFFFFF;
    private static final int GRAY = 0xFF888888;

    public static void render(DrawContext context) {
        NotifyManager mgr = NotifyManager.get();
        mgr.update();

        List<Notify> all = mgr.getNotifies();
        if (all.isEmpty()) return;

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;

        int scW = mc.getWindow().getScaledWidth();
        int scH = mc.getWindow().getScaledHeight();
        Theme theme = MeoRayClient.INSTANCE.getThemeManager().getRenderTheme();
        MsdfFont medium = FontManager.SUISSEINTMEDIUM.get();

        Matrix4f matrix = context.getMatrices().peek().getPositionMatrix();

        int panelH = 22;
        int gap = 4;
        int padding = 8;
        int baseX = scW - 8;
        int baseY = scH - 24;

        for (int i = all.size() - 1; i >= 0; i--) {
            Notify n = all.get(i);
            float fadeIn = n.getFadeIn();
            float fadeOut = n.getFadeOut();
            float alpha = fadeIn * fadeOut;
            if (alpha <= 0.01f) continue;

            String fullText = n.text;
            if (n.bind != null && !n.bind.isEmpty() && !n.bind.equalsIgnoreCase("n/a")) {
                fullText = n.text + " [" + n.bind + "]";
            }
            float textW = medium.getWidth(fullText, 7.0F);
            int panelW = (int)(textW + 56);
            int ix = baseX - panelW;
            int iy = baseY - (i + 1) * (panelH + gap);

            int alphaByte = Math.min(255, (int)(alpha * 255));
            int accentCol = statusColor(n.status, theme, alphaByte);

            // Main panel
            int panelCol = (PANEL_BASE & 0x00FFFFFF) | (alphaByte << 24);
            ((BuiltRectangle) Builder.rectangle()
                .size(new SizeState(panelW, panelH))
                .color(new QuadColorState(panelCol))
                .radius(new QuadRadiusState(6.0))
                .smoothness(1.15F)
                .build()).render(matrix, ix, iy);

            // Border
            int borderCol = (accentCol & 0x00FFFFFF) | ((int)(alphaByte * 0.6f) << 24);
            ((BuiltRectangle) Builder.rectangle()
                .size(new SizeState(panelW, 1.5f))
                .color(new QuadColorState(borderCol))
                .radius(new QuadRadiusState(1.0))
                .smoothness(1.15F)
                .build()).render(matrix, ix, iy);

            // Icon block (left)
            int iconCol = (accentCol & 0x00FFFFFF) | (alphaByte << 24);
            ((BuiltRectangle) Builder.rectangle()
                .size(new SizeState(18, 18))
                .color(new QuadColorState(iconCol))
                .radius(new QuadRadiusState(4.0, 4.0, 2.0, 2.0))
                .smoothness(1.15F)
                .build()).render(matrix, ix + 3, iy + 2);

            // Status icon character
            String iconChar = statusIcon(n.status);
            int iconTxtCol = (0xFF000000 & 0x00FFFFFF) | (alphaByte << 24);
            ((BuiltText) Builder.text()
                .font(medium).text(iconChar)
                .color(iconTxtCol).size(11.0F).thickness(0.05F)
                .build()).render(matrix, ix + 7, iy + 4.5f);

            // Text
            int txtCol = (WHITE & 0x00FFFFFF) | (alphaByte << 24);
            ((BuiltText) Builder.text()
                .font(medium).text(fullText)
                .color(txtCol).size(6.5F).thickness(0.045F)
                .build()).render(matrix, ix + 25, iy + 8);
        }
    }

    private static int statusColor(Status s, Theme theme, int alpha) {
        int rgb;
        switch (s) {
            case SUCCESS: rgb = theme != null ? theme.accent() & 0x00FFFFFF : 0xFF66BB6A; break;
            case WARNING: rgb = 0xFFFFB74D; break;
            case ERROR: rgb = 0xFFE57373; break;
            default: rgb = 0xFF64B5F6;
        }
        return (rgb & 0x00FFFFFF) | (alpha << 24);
    }

    private static String statusIcon(Status s) {
        switch (s) {
            case SUCCESS: return "\u2713";
            case WARNING: return "!";
            case ERROR: return "\u2715";
            default: return "i";
        }
    }
}
