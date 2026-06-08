package dev.meoray.client.util.render.builders.impl;

import dev.meoray.client.util.render.builders.AbstractBuilder;
import dev.meoray.client.util.render.builders.states.QuadColorState;
import dev.meoray.client.util.render.builders.states.QuadRadiusState;
import dev.meoray.client.util.render.builders.states.SizeState;
import dev.meoray.client.util.render.renderers.impl.BuiltTexture;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.texture.AbstractTexture;

@Environment(EnvType.CLIENT)
public final class TextureBuilder extends AbstractBuilder<BuiltTexture> {
    private SizeState size;
    private QuadRadiusState radius;
    private QuadColorState color;
    private float smoothness;
    private float u;
    private float v;
    private float texWidth;
    private float texHeight;
    private int textureId;

    public TextureBuilder size(SizeState size) {
        this.size = size;
        return this;
    }

    public TextureBuilder radius(QuadRadiusState radius) {
        this.radius = radius;
        return this;
    }

    public TextureBuilder color(QuadColorState color) {
        this.color = color;
        return this;
    }

    public TextureBuilder smoothness(float smoothness) {
        this.smoothness = smoothness;
        return this;
    }

    public TextureBuilder texture(float u, float v, float texWidth, float texHeight, AbstractTexture texture) {
        return this.texture(u, v, texWidth, texHeight, texture.getGlId());
    }

    public TextureBuilder texture(float u, float v, float texWidth, float texHeight, int textureId) {
        this.u = u;
        this.v = v;
        this.texWidth = texWidth;
        this.texHeight = texHeight;
        this.textureId = textureId;
        return this;
    }

    protected BuiltTexture _build() {
        return new BuiltTexture(this.size, this.radius, this.color, this.smoothness, this.u, this.v, this.texWidth, this.texHeight, this.textureId);
    }

    protected void reset() {
        this.size = SizeState.NONE;
        this.radius = QuadRadiusState.NO_ROUND;
        this.color = QuadColorState.WHITE;
        this.smoothness = 1.0F;
        this.u = 0.0F;
        this.v = 0.0F;
        this.texWidth = 0.0F;
        this.texHeight = 0.0F;
        this.textureId = 0;
    }
}
