package dev.meoray.client.ui.gui.elements;

import dev.meoray.client.managers.FontManager;
import dev.meoray.client.util.other.UtfUtil;
import dev.meoray.client.util.render.builders.Builder;
import dev.meoray.client.util.render.msdf.MsdfFont;
import dev.meoray.client.util.render.renderers.impl.BuiltText;
import org.joml.Matrix4f;

public class SearchElement {
    private boolean select;
    private String value = "";
    private float animation;
    private float placeholderAlpha = 1f;
    private float renderCursorX;
    private float focusGlow;

    public void render(Matrix4f matrix, double x, double y, float delta) {
        MsdfFont font = FontManager.SUISSEINTMEDIUM.get();

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

        float phShift = select ? 2f : 0f;
        if (value.isEmpty() && placeholderAlpha > 0.005f) {
            String ph = "Search";
                    int phColor = Math.round(placeholderAlpha * animation * 255f) << 24 | 0xCCCCCC;
            ((BuiltText) Builder.text().font(font).text(ph).color(phColor).size(8.0F).thickness(0.05F).build())
                .render(matrix, (float) x + 8.0F + phShift, (float) y + 2.5F);
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
    }

    public void handleClick() {
        if (!select) {
            select = true;
            placeholderAlpha = 1f;
        }
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
}
