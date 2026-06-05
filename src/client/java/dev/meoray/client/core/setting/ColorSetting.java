package dev.meoray.client.core.setting;

import java.awt.Color;

public class ColorSetting extends Setting<Color> {
    public ColorSetting(String name, Color defaultValue) {
        super(name, defaultValue);
    }

    public int getRGB() {
        return getValue().getRGB();
    }

    public int getAlpha() {
        return getValue().getAlpha();
    }
}
