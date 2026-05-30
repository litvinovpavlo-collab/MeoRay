package dev.meoray.client.util.render.builders.impl;

import dev.meoray.client.util.render.builders.AbstractBuilder;
import dev.meoray.client.util.render.builders.states.QuadColorState;
import dev.meoray.client.util.render.builders.states.QuadRadiusState;
import dev.meoray.client.util.render.builders.states.SizeState;
import dev.meoray.client.util.render.renderers.impl.BuiltBlur;

public final class BlurBuilder extends AbstractBuilder<BuiltBlur> {
    private SizeState size;
    private QuadRadiusState radius;
    private QuadColorState color;
    private float blurRadius;
    private float smoothness;

    public BlurBuilder size(SizeState size) {
        this.size = size;
        return this;
    }

    public BlurBuilder radius(QuadRadiusState radius) {
        this.radius = radius;
        return this;
    }

    public BlurBuilder color(QuadColorState color) {
        this.color = color;
        return this;
    }

    public BlurBuilder blurRadius(float blurRadius) {
        this.blurRadius = blurRadius;
        return this;
    }

    public BlurBuilder smoothness(float smoothness) {
        this.smoothness = smoothness;
        return this;
    }

    protected BuiltBlur _build() {
        return new BuiltBlur(this.size, this.radius, this.color, this.blurRadius, this.smoothness);
    }

    protected void reset() {
        this.size = SizeState.NONE;
        this.radius = QuadRadiusState.NO_ROUND;
        this.color = QuadColorState.TRANSPARENT;
        this.blurRadius = 0.0F;
        this.smoothness = 1.0F;
    }
}
