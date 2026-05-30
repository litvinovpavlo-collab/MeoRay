package dev.meoray.client.gui.common;

import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

public class TextOnlyButton extends ButtonWidget {
    public TextOnlyButton(int x, int y, int width, int height, Text message, PressAction onPress) {
        super(x, y, width, height, message, onPress, button -> Text.empty());
    }
}
