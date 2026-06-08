package dev.meoray.client.gui.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.meoray.client.account.altmanager.AltManagerScreen;
import dev.meoray.client.managers.FontManager;
import dev.meoray.client.util.render.builders.Builder;
import dev.meoray.client.util.render.builders.states.QuadColorState;
import dev.meoray.client.util.render.builders.states.QuadRadiusState;
import dev.meoray.client.util.render.builders.states.SizeState;
import dev.meoray.client.util.render.msdf.MsdfFont;
import dev.meoray.client.util.render.renderers.impl.BuiltBlur;
import dev.meoray.client.util.render.renderers.impl.BuiltBorder;
import dev.meoray.client.util.render.renderers.impl.BuiltRectangle;
import dev.meoray.client.util.render.renderers.impl.BuiltText;
import dev.meoray.client.util.render.renderers.impl.BuiltTexture;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.screen.option.OptionsScreen;
import net.minecraft.client.gui.screen.world.SelectWorldScreen;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.joml.Matrix4f;
import org.lwjgl.glfw.GLFW;

import java.awt.Color;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ThreadLocalRandom;
import net.fabricmc.loader.api.FabricLoader;

public class CustomTitleScreen extends Screen {

    private static final Identifier BG = Identifier.of("meoray", "textures/gui/background.png");

    private static final Color BLUR_BG = new Color(20, 20, 30, 120);
    private static final Color BORDER_COLOR = new Color(255, 255, 255, 40);
    private static final Color BORDER_HOVER = new Color(255, 255, 255, 100);
    private static final Color OVERLAY_HOVER = new Color(255, 255, 255, 30);
    private static final Color TEXT_COLOR = new Color(200, 200, 200, 200);
    private static final Color TEXT_WHITE = new Color(255, 255, 255, 255);

    private final MsdfFont regularFont;
    private final MsdfFont sfFont;

    private ButtonDef[] mainButtons;

    // Nickname panel
    private double nickX = 10;
    private double nickY = 10;
    private double nickW = 100;
    private double nickH = 22;
    private boolean draggingNick;
    private double nickDragOffX;
    private double nickDragOffY;
    private static double savedNickX = -1;
    private static double savedNickY = -1;
    private static final Path NICK_POS_FILE = FabricLoader.getInstance().getConfigDir().resolve("meoray").resolve("nickname_pos.txt");



    public CustomTitleScreen() {
        super(Text.literal("MeoRay Menu"));
        regularFont = FontManager.SUISSEINTMEDIUM.get();
        sfFont = FontManager.SF.get();
    }

    @Override
    protected void init() {
        double cX = width / 2d;
        double cY = height / 2d;
        double bw = 140;
        double bh = 28;
        double startY = cY - bh / 2d + 100;

        mainButtons = new ButtonDef[]{
            new ButtonDef("Exit", cX - bw / 2d, startY, bw, bh,
                () -> client.scheduleStop()),
            new ButtonDef("Options", cX - bw / 2d, startY - 33, bw, bh,
                () -> client.setScreen(new OptionsScreen(this, client.options))),
            new ButtonDef("Altmanager", cX - bw / 2d, startY - 66, bw, bh,
                () -> client.setScreen(new AltManagerScreen(this))),
            new ButtonDef("Multiplayer", cX - bw / 2d, startY - 99, bw, bh,
                () -> client.setScreen(new MultiplayerScreen(this))),
            new ButtonDef("Singleplayer", cX - bw / 2d, startY - 132, bw, bh,
                () -> client.setScreen(new SelectWorldScreen(this))),
        };

        String nick = client.getSession().getUsername();
        float nickTextW = regularFont.getWidth(nick + " | " + client.getCurrentFps() + " FPS", 7f);
        nickW = nickTextW + 30;

        if (savedNickX >= 0 && savedNickY >= 0) {
            nickX = Math.max(0, Math.min(savedNickX, width - nickW));
            nickY = Math.max(0, Math.min(savedNickY, height - nickH));
        } else {
            nickX = 10;
            nickY = 10;
        }

        loadNickPos();

    }

    private static void loadNickPos() {
        try {
            if (Files.exists(NICK_POS_FILE)) {
                String content = Files.readString(NICK_POS_FILE).trim();
                String[] parts = content.split(",");
                if (parts.length == 2) {
                    savedNickX = Double.parseDouble(parts[0]);
                    savedNickY = Double.parseDouble(parts[1]);
                }
            }
        } catch (IOException | NumberFormatException e) {
        }
    }

    private static void saveNickPos() {
        try {
            Files.createDirectories(NICK_POS_FILE.getParent());
            Files.writeString(NICK_POS_FILE, savedNickX + "," + savedNickY);
        } catch (IOException e) {
        }
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        Matrix4f matrix = ctx.getMatrices().peek().getPositionMatrix();
        renderBackgroundImage(ctx, matrix);

        renderNicknamePanel(ctx, matrix, mouseX, mouseY);

        renderTimeAndDate(ctx, matrix);

        for (ButtonDef btn : mainButtons) {
            btn.hover = mouseX >= btn.x && mouseX <= btn.x + btn.w
                && mouseY >= btn.y && mouseY <= btn.y + btn.h;
            renderButton(ctx, matrix, btn);
        }

    }

    // ──────────────────────────────────────────────
    // Background
    // ──────────────────────────────────────────────

    private void renderBackgroundImage(DrawContext ctx, Matrix4f matrix) {
        RenderSystem.setShaderTexture(0, BG);
        ((BuiltTexture) Builder.texture()
            .size(new SizeState(width, height))
            .radius(new QuadRadiusState(0f))
            .color(new QuadColorState(new Color(255, 255, 255, 255)))
            .build()).render(matrix, 0, 0, 0);
    }

    // ──────────────────────────────────────────────
    // Nickname Panel (draggable)
    // ──────────────────────────────────────────────

    private void renderNicknamePanel(DrawContext ctx, Matrix4f matrix, int mx, int my) {
        String username = client.getSession().getUsername();
        int fps = client.getCurrentFps();
        String ping = "0ms";
        String info = username + " | " + fps + " FPS | " + ping;

        float infoW = regularFont.getWidth(info, 7f);
        nickW = infoW + 30;

        ((BuiltBlur) Builder.blur()
            .size(new SizeState((float) nickW, (float) nickH))
            .radius(new QuadRadiusState(6f))
            .color(new QuadColorState(new Color(255, 255, 255, 255)))
            .blurRadius(12f)
            .smoothness(1.15f)
            .build()).render(matrix, (float) nickX, (float) nickY);

        ((BuiltRectangle) Builder.rectangle()
            .size(new SizeState((float) nickW, (float) nickH))
            .color(new QuadColorState(BLUR_BG))
            .radius(new QuadRadiusState(6f))
            .smoothness(1.15f)
            .build()).render(matrix, (float) nickX, (float) nickY);

        ((BuiltBorder) Builder.border()
            .size(new SizeState((float) nickW, (float) nickH))
            .color(new QuadColorState(BORDER_COLOR))
            .radius(new QuadRadiusState(6f))
            .thickness(0.015f)
            .smoothness(0.6f, 0.6f)
            .build()).render(matrix, (float) nickX, (float) nickY);

        float tx = (float) nickX + (float) (nickW - infoW) / 2f - 2f;
        ((BuiltText) Builder.text()
            .font(regularFont).text(info)
            .color(TEXT_WHITE).size(7f).thickness(0.05f)
            .build()).render(matrix, tx, (float) nickY + 5.5f);
    }

    // ──────────────────────────────────────────────
    // Time & Date
    // ──────────────────────────────────────────────

    private void renderTimeAndDate(DrawContext ctx, Matrix4f matrix) {
        String timeText = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        String dateText = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy"));

        float centerX = width / 2f;
        float centerY = height / 2f;

        float timeSize = 30f;
        float dateSize = 17f;

        float tw = sfFont.getWidth(timeText, timeSize);
        float dw = sfFont.getWidth(dateText, dateSize);

        ((BuiltText) Builder.text().font(sfFont).text(timeText)
            .color(TEXT_WHITE).size(timeSize).thickness(0.08f).spacing(4f)
            .build()).render(matrix, centerX - tw / 2f, centerY - 125f);

        ((BuiltText) Builder.text().font(sfFont).text(dateText)
            .color(TEXT_WHITE).size(dateSize).thickness(0.08f).spacing(2.5f)
            .build()).render(matrix, centerX - dw / 2f, centerY - 87f);
    }

    // ──────────────────────────────────────────────
    // Glassmorphism Button
    // ──────────────────────────────────────────────

    private void renderButton(DrawContext ctx, Matrix4f matrix, ButtonDef btn) {
        ((BuiltBlur) Builder.blur()
            .size(new SizeState((float) btn.w, (float) btn.h))
            .radius(new QuadRadiusState(8.6f))
            .color(new QuadColorState(new Color(255, 255, 255, 255)))
            .blurRadius(15f)
            .smoothness(1.15f)
            .build()).render(matrix, (float) btn.x, (float) btn.y);

        ((BuiltRectangle) Builder.rectangle()
            .size(new SizeState((float) btn.w, (float) btn.h))
            .color(new QuadColorState(BLUR_BG))
            .radius(new QuadRadiusState(8.6f))
            .smoothness(1.15f)
            .build()).render(matrix, (float) btn.x, (float) btn.y);

        ((BuiltBorder) Builder.border()
            .size(new SizeState((float) btn.w, (float) btn.h))
            .color(new QuadColorState(BORDER_COLOR))
            .radius(new QuadRadiusState(8.6f))
            .thickness(0.015f)
            .smoothness(0.6f, 0.6f)
            .build()).render(matrix, (float) btn.x, (float) btn.y);

        float tw = regularFont.getWidth(btn.label, 8f);
        ((BuiltText) Builder.text().font(regularFont).text(btn.label)
            .color(TEXT_COLOR).size(8f).thickness(0.05f)
            .build()).render(matrix,
                (float) btn.x + (float) btn.w / 2f - tw / 2f,
                (float) btn.y + 9f);

        if (btn.hover) {
            ((BuiltRectangle) Builder.rectangle()
                .size(new SizeState((float) btn.w, (float) btn.h))
                .color(new QuadColorState(OVERLAY_HOVER))
                .radius(new QuadRadiusState(8.6f))
                .smoothness(1.15f)
                .build()).render(matrix, (float) btn.x, (float) btn.y);

            ((BuiltBorder) Builder.border()
                .size(new SizeState((float) btn.w, (float) btn.h))
                .color(new QuadColorState(BORDER_HOVER))
                .radius(new QuadRadiusState(8.6f))
                .thickness(0.02f)
                .smoothness(0.6f, 0.6f)
                .build()).render(matrix, (float) btn.x, (float) btn.y);

            ((BuiltText) Builder.text().font(regularFont).text(btn.label)
                .color(TEXT_WHITE).size(8f).thickness(0.05f)
                .build()).render(matrix,
                    (float) btn.x + (float) btn.w / 2f - tw / 2f,
                    (float) btn.y + 9f);
        }
    }

    // ──────────────────────────────────────────────
    // Mouse events
    // ──────────────────────────────────────────────

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button != 0) return super.mouseClicked(mx, my, button);

        if (mx >= nickX && mx <= nickX + nickW && my >= nickY && my <= nickY + nickH) {
            draggingNick = true;
            nickDragOffX = mx - nickX;
            nickDragOffY = my - nickY;
            return true;
        }

        for (ButtonDef btn : mainButtons) {
            if (mx >= btn.x && mx <= btn.x + btn.w && my >= btn.y && my <= btn.y + btn.h) {
                btn.action.run();
                return true;
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (draggingNick && button == 0) {
            nickX = Math.max(0, Math.min(mx - nickDragOffX, width - nickW));
            nickY = Math.max(0, Math.min(my - nickDragOffY, height - nickH));
            return true;
        }
        return super.mouseDragged(mx, my, button, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        if (button == 0 && draggingNick) {
            draggingNick = false;
            savedNickX = Math.max(0, Math.min(nickX, width - nickW));
            savedNickY = Math.max(0, Math.min(nickY, height - nickH));
            saveNickPos();
        }
        return super.mouseReleased(mx, my, button);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double horiz, double vert) {
        return super.mouseScrolled(mx, my, horiz, vert);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        return super.charTyped(codePoint, modifiers);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    // ──────────────────────────────────────────────
    // Helper classes
    // ──────────────────────────────────────────────

    private static class ButtonDef {
        final String label;
        final double x, y, w, h;
        final Runnable action;
        boolean hover;

        ButtonDef(String label, double x, double y, double w, double h, Runnable action) {
            this.label = label;
            this.x = x;
            this.y = y;
            this.w = w;
            this.h = h;
            this.action = action;
        }
    }
}
