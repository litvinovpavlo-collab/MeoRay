package dev.meoray.client.util.render.builders;

import dev.meoray.client.util.render.builders.impl.BlurBuilder;
import dev.meoray.client.util.render.builders.impl.BorderBuilder;
import dev.meoray.client.util.render.builders.impl.RectangleBuilder;
import dev.meoray.client.util.render.builders.impl.TextBuilder;
import dev.meoray.client.util.render.builders.impl.TextureBuilder;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public final class Builder {
    private static final RectangleBuilder RECTANGLE_BUILDER = new RectangleBuilder();
    private static final BorderBuilder BORDER_BUILDER = new BorderBuilder();

    public static RectangleBuilder rectangle() {
        return RECTANGLE_BUILDER;
    }

    public static BorderBuilder border() {
        return BORDER_BUILDER;
    }

    public static BlurBuilder blur() {
        return new BlurBuilder();
    }

    public static TextureBuilder texture() {
        return new TextureBuilder();
    }

    public static TextBuilder text() {
        return new TextBuilder();
    }
}
