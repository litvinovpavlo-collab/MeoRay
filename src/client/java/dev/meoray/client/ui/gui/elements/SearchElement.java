package dev.meoray.client.ui.gui.elements;

import dev.meoray.client.managers.FontManager;
import dev.meoray.client.util.other.UtfUtil;
import dev.meoray.client.util.render.builders.Builder;
import dev.meoray.client.util.render.msdf.MsdfFont;
import dev.meoray.client.util.render.renderers.impl.BuiltText;
import org.joml.Matrix4f;

public class SearchElement {
    private boolean select = false;
    private String value = "";
    private double animation;
    private int key = 0;
    private String type;

    public void render(Matrix4f matrix, double x, double y) {
        String endValue;
        if (select && value.isEmpty()) {
            endValue = "Type...";
        } else if (!select && value.isEmpty()) {
            endValue = "Search";
        } else if (select) {
            endValue = value + "_";
        } else {
            endValue = value;
        }
        if (!endValue.isEmpty()) {
            int color = (int) (animation * 255f) << 24 | 0xCCCCCC;
            MsdfFont font = FontManager.SUISSEINTMEDIUM.get();
            ((BuiltText) Builder.text().font(font).text(endValue).color(color).size(8.0F).thickness(0.05F).build()).render(matrix, (float) x + 8.0F, (float) y + 2.5F);
        }
    }

    public void handleClick() {
        this.select = true;
    }

    public void tick(int keyCode, String typed) {
        this.key = keyCode;
        this.type = typed;
        if (select) {
            if (!type.isEmpty() && value.toCharArray().length < 23 && !UtfUtil.containsRussianLetter(type)) {
                value = value + type;
            }
            if (key == 259) {
                if (!value.isEmpty()) value = value.substring(0, value.length() - 1);
            }
            if (key == 32) value = value + " ";
            if (key == 257) select = false;
        }
    }

    public void setAnimation(double animation) { this.animation = animation; }
    public String getValue() { return value; }
    public void setSelect(boolean select) { this.select = select; }
}
