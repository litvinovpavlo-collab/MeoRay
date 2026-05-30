package dev.meoray.client.gui.font;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class IconFont {
    public static final Identifier ID = Identifier.of("meoray", "icomoon");

    public void drawString(DrawContext ctx, String text, float x, float y, int color) {
        var styled = Text.literal(text).setStyle(
            Style.EMPTY.withFont(ID).withColor(color)
        );
        ctx.drawText(
            MinecraftClient.getInstance().textRenderer,
            styled,
            (int) x, (int) y,
            color, false
        );
    }
}
