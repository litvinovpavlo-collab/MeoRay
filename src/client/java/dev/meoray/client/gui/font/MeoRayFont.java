package dev.meoray.client.gui.font;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class MeoRayFont {
    public static final Identifier FONT_ID = Identifier.of("meoray", "default");
    public static final Identifier ICOMOON = Identifier.of("meoray", "icomoon");

    public static Text styled(String text) {
        return Text.literal(text).setStyle(Style.EMPTY.withFont(FONT_ID));
    }

    public static Text styled(String text, int color) {
        return Text.literal(text).setStyle(Style.EMPTY.withFont(FONT_ID).withColor(color));
    }

    public static int getWidth(String text) {
        return MinecraftClient.getInstance().textRenderer.getWidth(styled(text));
    }

    public static void draw(DrawContext context, String text, int x, int y, int color) {
        TextRenderer tr = MinecraftClient.getInstance().textRenderer;
        context.drawTextWithShadow(tr, styled(text), x, y, color);
    }

    public static void drawCentered(DrawContext context, String text, int x, int y, int color) {
        TextRenderer tr = MinecraftClient.getInstance().textRenderer;
        int w = tr.getWidth(styled(text));
        context.drawTextWithShadow(tr, styled(text), x - w / 2, y, color);
    }

    public static void drawNoShadow(DrawContext context, String text, int x, int y, int color) {
        TextRenderer tr = MinecraftClient.getInstance().textRenderer;
        context.drawText(tr, styled(text), x, y, color, false);
    }
}
