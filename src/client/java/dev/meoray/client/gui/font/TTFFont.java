package dev.meoray.client.gui.font;

import com.mojang.blaze3d.systems.RenderSystem;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.Identifier;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import dev.meoray.client.util.render.builders.impl.TextureBuilder;
import dev.meoray.client.util.render.builders.states.QuadColorState;
import dev.meoray.client.util.render.builders.states.QuadRadiusState;
import dev.meoray.client.util.render.builders.states.SizeState;
import dev.meoray.client.util.render.renderers.impl.BuiltTexture;

public class TTFFont {
    private final Font awtFont;
    private final Map<Character, GlyphData> cache = new HashMap<>();

    public TTFFont(Identifier resourceId, float size) {
        Font base;
        try {
            var resource = MinecraftClient.getInstance()
                .getResourceManager()
                .getResource(resourceId)
                .orElseThrow(() -> new RuntimeException("Font resource not found: " + resourceId));
            try (InputStream stream = resource.getInputStream()) {
                base = Font.createFont(Font.TRUETYPE_FONT, stream);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to load font: " + resourceId, e);
        }
        this.awtFont = base.deriveFont(size);
    }

    public void drawString(Matrix4f matrix, String text, float x, float y, int color) {
        if (text.isEmpty()) return;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();

        float curX = x;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            GlyphData g = cache.computeIfAbsent(c, this::rasterize);
            if (g == null || g.glId == -1) {
                curX += 8;
                continue;
            }

            RenderSystem.setShaderTexture(0, g.glId);
            new BuiltTexture(
                new SizeState(g.width, g.height),
                new QuadRadiusState(0, 0, 0, 0),
                new QuadColorState(color),
                1.0f
            ).render(matrix, curX, y, 0);

            curX += g.advance;
        }

        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }

    private GlyphData rasterize(char c) {
        try {
            BufferedImage tmp = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
            FontMetrics metrics = tmp.createGraphics().getFontMetrics(awtFont);
            int w = Math.max(metrics.charWidth(c), 1);
            int h = Math.max(metrics.getHeight(), 1);

            BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2d = img.createGraphics();
            g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2d.setRenderingHint(RenderingHints.KEY_RENDERING,
                RenderingHints.VALUE_RENDER_QUALITY);
            g2d.setColor(Color.WHITE);
            g2d.setFont(awtFont);
            g2d.drawString(String.valueOf(c), 0, metrics.getAscent());
            g2d.dispose();

            int[] pixels = new int[w * h];
            img.getRGB(0, 0, w, h, pixels, 0, w);

            ByteBuffer buffer = ByteBuffer.allocateDirect(w * h * 4);
            for (int py = 0; py < h; py++) {
                for (int px = 0; px < w; px++) {
                    int argb = pixels[py * w + px];
                    buffer.put((byte) ((argb >> 16) & 0xFF));
                    buffer.put((byte) ((argb >> 8) & 0xFF));
                    buffer.put((byte) (argb & 0xFF));
                    buffer.put((byte) ((argb >> 24) & 0xFF));
                }
            }
            buffer.flip();

            int glId = GL11.glGenTextures();
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, glId);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
            GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA, w, h, 0,
                GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, buffer);

            return new GlyphData(glId, w, h, metrics.charWidth(c));
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private record GlyphData(int glId, int width, int height, int advance) {}
}
