package dev.meoray.client.util.render;

import dev.meoray.client.util.render.builders.Builder;
import dev.meoray.client.util.render.builders.states.QuadColorState;
import dev.meoray.client.util.render.builders.states.QuadRadiusState;
import dev.meoray.client.util.render.builders.states.SizeState;
import dev.meoray.client.util.render.renderers.impl.BuiltRectangle;
import org.joml.Matrix4f;

public final class GlowRenderer {

    public static void drawGlow(Matrix4f matrix, float x, float y, float w, float h, int color, float radius, float cornerR) {
        int baseAlpha = (color >>> 24) & 0xFF;
        if (baseAlpha == 0) return;

        int rgb = color & 0x00FFFFFF;

        int passes = 7;
        for (int i = passes; i >= 1; i--) {
            float t = (float) i / (float) passes;
            float expand = radius * t * t * 0.85f;
            float falloff = (1f - t);
            falloff = falloff * falloff;
            int passAlpha = (int) (baseAlpha * 0.35f * falloff);
            if (passAlpha <= 0) continue;

            int passColor = rgb | (passAlpha << 24);
            float passCorner = cornerR + expand * 0.3f;
            if (passCorner < 0) passCorner = 0;

            ((BuiltRectangle) Builder.rectangle()
                .size(new SizeState(w + expand * 2, h + expand * 2))
                .color(new QuadColorState(passColor))
                .radius(new QuadRadiusState(passCorner))
                .smoothness(2.0F)
                .build()).render(matrix, x - expand, y - expand);
        }

        int coreAlpha = (int) (baseAlpha * 0.5f);
        int coreColor = rgb | (coreAlpha << 24);
        ((BuiltRectangle) Builder.rectangle()
            .size(new SizeState(w, h))
            .color(new QuadColorState(coreColor))
            .radius(new QuadRadiusState(cornerR))
            .smoothness(1.15F)
            .build()).render(matrix, x, y);
    }
}
