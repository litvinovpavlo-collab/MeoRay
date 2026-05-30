package dev.meoray.client.util.render.builders.impl;

import dev.meoray.client.util.render.builders.AbstractBuilder;
import dev.meoray.client.util.render.msdf.MsdfFont;
import dev.meoray.client.util.render.renderers.impl.BuiltText;
import java.awt.Color;

public final class TextBuilder extends AbstractBuilder<BuiltText> {
    private MsdfFont font;
    private String text;
    private float size;
    private float thickness;
    private int color;
    private float smoothness;
    private float spacing;
    private int outlineColor;
    private float outlineThickness;
    private boolean gradient = false;
    private boolean gradientAlpha = false;
    private double alpha;

    public TextBuilder font(MsdfFont font) {
        this.font = font;
        return this;
    }

    public TextBuilder text(String text) {
        this.text = text;
        return this;
    }

    public TextBuilder size(float size) {
        this.size = size;
        return this;
    }

    public TextBuilder thickness(float thickness) {
        this.thickness = thickness;
        return this;
    }

    public TextBuilder color(Color color) {
        return this.color(color.getRGB());
    }

    public TextBuilder color(int color) {
        this.gradient = false;
        this.color = color;
        return this;
    }

    public TextBuilder alpha(double alpha) {
        this.alpha = alpha;
        this.gradientAlpha = true;
        return this;
    }

    public TextBuilder gradient(boolean value) {
        this.gradient = value;
        this.gradientAlpha = false;
        return this;
    }

    public TextBuilder smoothness(float smoothness) {
        this.smoothness = smoothness;
        return this;
    }

    public TextBuilder spacing(float spacing) {
        this.spacing = spacing;
        return this;
    }

    public TextBuilder outline(Color color, float thickness) {
        return this.outline(color.getRGB(), thickness);
    }

    public TextBuilder outline(int color, float thickness) {
        this.outlineColor = color;
        this.outlineThickness = thickness;
        return this;
    }

    protected BuiltText _build() {
        return new BuiltText(this.font, this.text, this.size, this.thickness, this.color, this.smoothness, this.spacing, this.outlineColor, this.outlineThickness, this.gradient, this.gradientAlpha, this.alpha);
    }

    protected void reset() {
        this.font = null;
        this.text = "";
        this.size = 0.0F;
        this.thickness = 0.05F;
        this.color = -1;
        this.smoothness = 0.5F;
        this.spacing = 0.0F;
        this.outlineColor = 0;
        this.outlineThickness = 0.0F;
    }
}
