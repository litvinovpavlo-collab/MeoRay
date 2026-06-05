package dev.meoray.client.ui.gui.elements;

import dev.meoray.client.managers.FontManager;
import dev.meoray.client.util.other.UtfUtil;
import dev.meoray.client.util.render.builders.Builder;
import dev.meoray.client.util.render.builders.states.QuadColorState;
import dev.meoray.client.util.render.builders.states.QuadRadiusState;
import dev.meoray.client.util.render.builders.states.SizeState;
import dev.meoray.client.util.render.msdf.MsdfFont;
import dev.meoray.client.util.render.renderers.impl.BuiltRectangle;
import dev.meoray.client.util.render.renderers.impl.BuiltText;
import org.joml.Matrix4f;

public class SearchElement {
    private boolean select;
    private String value = "";
    private float animation;
    private float placeholderAlpha = 1f;
    private float renderCursorX;
    private float focusGlow;
    private float clearHover;
    private float clearAlpha;
    private boolean clearHoveredLast;

    private static final float W = 150f;
    private static final float H = 14f;

    public void render(Matrix4f matrix, double x, double y, float delta) {
        MsdfFont font = FontManager.SUISSEINTMEDIUM.get();
        MsdfFont icons = FontManager.ICONS.get();

        float cursorAlpha = (float) (Math.sin(System.currentTimeMillis() * 0.005) * 0.5 + 0.5);
        if (!select) cursorAlpha *= 0.3f;

        if (select) {
            focusGlow = Math.min(1, focusGlow + delta * 8f);
        } else {
            focusGlow = Math.max(0, focusGlow - delta * 6f);
        }

        if (value.isEmpty()) {
            placeholderAlpha = Math.min(1, placeholderAlpha + delta * 4f);
        } else {
            placeholderAlpha = Math.max(0, placeholderAlpha - delta * 5f);
        }

        float targetClear = (clearHoveredLast && !value.isEmpty()) ? 1f : 0f;
        clearHover += (targetClear - clearHover) * Math.min(1, delta * 14f);
        float targetClearAlpha = value.isEmpty() ? 0f : 1f;
        clearAlpha += (targetClearAlpha - clearAlpha) * Math.min(1, delta * 10f);

        if (value.isEmpty() && placeholderAlpha > 0.005f) {
            String ph = "Search";
            int phColor = Math.round(placeholderAlpha * animation * 255f) << 24 | 0xCCCCCC;
            ((BuiltText) Builder.text().font(font).text(ph).color(phColor).size(8.0F).thickness(0.05F).build())
                .render(matrix, (float) x + 8.0F, (float) y + 2.5F);
        }

        if (!value.isEmpty()) {
            int txtColor = Math.round(animation * 255f) << 24 | 0xCCCCCC;
            ((BuiltText) Builder.text().font(font).text(value).color(txtColor).size(8.0F).thickness(0.05F).build())
                .render(matrix, (float) x + 8.0F, (float) y + 2.5F);
        }

        float targetCursorX = (float) (x + 8.0F);
        if (!value.isEmpty()) {
            targetCursorX += font.getWidth(value, 8.0F);
        }
        renderCursorX += (targetCursorX - renderCursorX) * Math.min(1, delta * 18f);

        if (select) {
            int curColor = Math.round(cursorAlpha * animation * 200f) << 24 | 0xCCCCCC;
            ((BuiltText) Builder.text().font(font).text("_").color(curColor).size(8.0F).thickness(0.05F).build())
                .render(matrix, renderCursorX, (float) y + 2.5F);
        }

        if (clearAlpha > 0.01f) {
            int xAlpha = Math.round(clearAlpha * 255f);
            int xFinal = (xAlpha << 24) | 0x00B4B4C0;
            float xSize = 6.5F;
            float xPad = 5f;
            float xx = (float) x + W - xPad - xSize;
            float xy = (float) y + (H - xSize) / 2f;
            int hoverBg = Math.round(clearAlpha * clearHover * 100f) << 24 | 0x00FFFFFF;
            if (hoverBg != 0) {
                ((BuiltRectangle) Builder.rectangle()
                    .size(new SizeState(xSize + 4, xSize + 4))
                    .color(new QuadColorState(hoverBg))
                    .radius(new QuadRadiusState(3.0))
                    .smoothness(1.15F)
                    .build()).render(matrix, xx - 2, xy - 2);
            }
            ((BuiltText) Builder.text().font(icons).text("k")
                .color(xFinal).size(xSize).thickness(0.05F).build())
                .render(matrix, xx, xy);
        }
    }

    public boolean handleClick(float mx, float my, double x, double y) {
        float xSize = 6.5F;
        float xPad = 5f;
        float xx = (float) x + W - xPad - xSize;
        float xy = (float) y + (H - xSize) / 2f;
        if (!value.isEmpty() && mx >= xx - 2 && mx <= xx + xSize + 2 && my >= xy - 2 && my <= xy + xSize + 2) {
            value = "";
            return true;
        }
        if (!select) {
            select = true;
            placeholderAlpha = 1f;
        }
        return false;
    }

    public void updateMouseHover(float mx, float my, double x, double y) {
        float xSize = 6.5F;
        float xPad = 5f;
        float xx = (float) x + W - xPad - xSize;
        float xy = (float) y + (H - xSize) / 2f;
        clearHoveredLast = !value.isEmpty() && mx >= xx - 2 && mx <= xx + xSize + 2 && my >= xy - 2 && my <= xy + xSize + 2;
    }

    public void tick(int keyCode, String typed) {
        if (select) {
            if (!typed.isEmpty() && value.length() < 23 && !UtfUtil.containsRussianLetter(typed)) {
                value = value + typed;
            }
            if (keyCode == 259 && !value.isEmpty()) {
                value = value.substring(0, value.length() - 1);
            }
            if (keyCode == 257) {
                select = false;
            }
        }
    }

    public void setAnimation(double animation) { this.animation = (float) animation; }

    public String getValue() { return value; }

    public void setSelect(boolean select) {
        if (this.select != select) {
            this.select = select;
            placeholderAlpha = 1f;
        }
    }

    public float getFocusGlow() { return focusGlow; }

    public boolean isSelect() { return select; }
}

