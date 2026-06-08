package dev.meoray.client.account.altmanager.impl;

import dev.meoray.client.account.altmanager.Type;
import dev.meoray.client.managers.FontManager;
import dev.meoray.client.util.animations.Animation;
import dev.meoray.client.util.animations.Direction;
import dev.meoray.client.util.animations.EaseBackIn;
import dev.meoray.client.util.math.MathUtil;
import dev.meoray.client.util.other.UtfUtil;
import dev.meoray.client.util.render.builders.Builder;
import dev.meoray.client.util.render.builders.states.QuadColorState;
import dev.meoray.client.util.render.builders.states.QuadRadiusState;
import dev.meoray.client.util.render.builders.states.SizeState;
import dev.meoray.client.util.render.renderers.impl.BuiltBorder;
import dev.meoray.client.util.render.renderers.impl.BuiltRectangle;
import dev.meoray.client.util.render.renderers.impl.BuiltText;
import java.awt.Color;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.DrawContext;
import org.joml.Matrix4f;

@Environment(EnvType.CLIENT)
public class StringElement {
    private boolean select = false;
    private String value = "";
    private int key = 0;
    private String type;
    private final Type typeElement;
    private final Animation animation;
    private StringElement sibling;

    public StringElement(Type typeElement) {
        this.typeElement = typeElement;
        this.animation = new EaseBackIn(400, 1.0, 0.1f, Direction.BACKWARDS);
    }

    public void setSibling(StringElement sibling) {
        this.sibling = sibling;
    }

    public void mouseClick(int click) {
        if (click == 0) {
            if (this.sibling != null) {
                this.sibling.resetSelect();
            }
            this.select = true;
            this.animation.setDirection(Direction.FORWARDS);
        }
    }

    public void render(double x, double y, double width, double height, double mouseX, double mouseY, DrawContext drawContext) {
        Color color = new Color(12961746);
        Matrix4f matrix = drawContext.getMatrices().peek().getPositionMatrix();
        String defaultText = this.typeElement == Type.NAME ? "Name" : "Tag";
        String endValue = this.value.equalsIgnoreCase("") && !this.select ? defaultText : this.value;
        ((BuiltRectangle)Builder.rectangle().size(new SizeState(width, height))
            .color(new QuadColorState(new Color(-14342350, true)))
            .radius(new QuadRadiusState(5.0F)).smoothness(1.15F).build())
            .render(matrix, (float)x, (float)y);
        ((BuiltBorder)Builder.border().size(new SizeState(width, height))
            .color(new QuadColorState(new Color(3092796)))
            .radius(new QuadRadiusState(5.0F)).thickness(0.01F)
            .smoothness(0.6F, 0.6F).build())
            .render(matrix, (float)x, (float)y);
        ((BuiltText)Builder.text().font(FontManager.MAINMENU.get())
            .text(this.typeElement == Type.NAME ? "L" : "X")
            .color(new Color(-11710630, true)).size(8.0F).thickness(0.05F).build())
            .render(matrix, (float)(x + 7.0), (float)(y + 5.9));
        if (!this.animation.finished(Direction.BACKWARDS)) {
            ((BuiltText)Builder.text().font(FontManager.MAINMENU.get())
                .text(this.typeElement == Type.NAME ? "L" : "X")
                .color(new Color(4241151)).size(8.0F).thickness(0.05F).build())
                .render(matrix, (float)(x + 7.0), (float)(y + 5.9));
        }
        if (!endValue.isEmpty()) {
            BuiltText text = (BuiltText)Builder.text().font(FontManager.SUISSEINTMEDIUM.get())
                .text(endValue).color(color).size(8.0F).thickness(0.05F).build();
            text.render(matrix, (float)(x + 20.0), (float)(y + 5.0));
            if (!this.animation.finished(Direction.BACKWARDS)) {
                text = (BuiltText)Builder.text().font(FontManager.SUISSEINTMEDIUM.get())
                    .text(endValue).color(Color.WHITE).size(8.0F).thickness(0.05F).build();
                text.render(matrix, (float)(x + 20.0), (float)(y + 5.0));
            }
        }
    }

    public void button(double x, double y, double width, double height, double mouseX, double mouseY,
                       double xStart, double yStart, double xEnd, double yEnd,
                       DrawContext drawContext, int click) {
        this.render(x, y, width, height, mouseX, mouseY, drawContext);
        if (MathUtil.isHovered((int)x, (int)y, (int)width, (int)height, (int)mouseX, (int)mouseY)) {
            this.mouseClick(click);
        }
        if (this.select) {
            if (!this.type.isEmpty() && this.value.toCharArray().length < 23 && !UtfUtil.containsRussianLetter(this.type)) {
                this.value = this.value + this.type;
            }
            if (this.key == 259) {
                StringBuilder textEnd = new StringBuilder();
                char[] text2 = this.value.toCharArray();
                for (int i = 0; i < text2.length; i++) {
                    if (i < text2.length - 1) {
                        textEnd.append(text2[i]);
                    }
                }
                this.value = textEnd.toString();
            }
            if (this.key == 32) {
                this.value = this.value + " ";
            }
            if (this.key == 257) {
                this.select = false;
                this.animation.setDirection(Direction.BACKWARDS);
            }
        }
    }

    public void setType(String type) { this.type = type; }
    public void setKey(int key) { this.key = key; }
    public void setSelect(boolean select) { this.select = select; }
    public String getValue() { return this.value; }
    public void setValue(String value) { this.value = value; }

    public void resetSelect() {
        this.setSelect(false);
        this.animation.setDirection(Direction.BACKWARDS);
    }
}
