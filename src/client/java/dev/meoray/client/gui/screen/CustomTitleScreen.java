package dev.meoray.client.gui.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.meoray.client.account.AccountManager;
import dev.meoray.client.account.MeoRayAccount;
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
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CopyOnWriteArrayList;
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
    private boolean altMenuOpen;

    private String altInput = "";
    private float scrollAlt;

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
                () -> altMenuOpen = true),
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

        if (altMenuOpen) {
            renderAltPopup(ctx, matrix, mouseX, mouseY);
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
    // Alt Manager Popup
    // ──────────────────────────────────────────────

    private void renderAltPopup(DrawContext ctx, Matrix4f matrix, int mx, int my) {
        ctx.fill(0, 0, width, height, 0x96000000);

        float wW = 220;
        float wH = 280;
        float wX = width / 2f - wW / 2f;
        float wY = height / 2f - wH / 2f;

        ((BuiltRectangle) Builder.rectangle()
            .size(new SizeState(wW, wH))
            .color(new QuadColorState(new Color(20, 20, 30, 200)))
            .radius(new QuadRadiusState(8f))
            .smoothness(1.15f)
            .build()).render(matrix, wX, wY);

        ((BuiltBorder) Builder.border()
            .size(new SizeState(wW, wH))
            .color(new QuadColorState(BORDER_COLOR))
            .radius(new QuadRadiusState(8f))
            .thickness(0.01f)
            .smoothness(0.6f, 0.6f)
            .build()).render(matrix, wX, wY);

        ((BuiltText) Builder.text().font(regularFont).text("Account Manager")
            .color(TEXT_WHITE).size(8f).thickness(0.05f)
            .build()).render(matrix, wX + wW / 2f - regularFont.getWidth("Account Manager", 8f) / 2f, wY + 12f);

        String placeholder = altInput.isEmpty() ? "Nickname..." : altInput;
        int tC = altInput.isEmpty() ? 0xFF787882 : 0xFFFFFFFF;
        boolean cursor = System.currentTimeMillis() % 1000 > 500;

        ((BuiltRectangle) Builder.rectangle()
            .size(new SizeState(wW - 50, 22))
            .color(new QuadColorState(new Color(30, 33, 44, 200)))
            .radius(new QuadRadiusState(4f))
            .smoothness(1.15f)
            .build()).render(matrix, wX + 15, wY + 35);

        ((BuiltText) Builder.text().font(FontManager.SF.get()).text(placeholder + (cursor && !altInput.isEmpty() ? "_" : ""))
            .color(new Color(tC, true)).size(7f).thickness(0.05f)
            .build()).render(matrix, wX + 22, wY + 42);

        boolean addH = mx >= wX + wW - 45 && mx <= wX + wW - 15
            && my >= wY + 35 && my <= wY + 57;
        int addCol = addH ? 0xFFFF6E14 : 0xFFE65C00;

        ((BuiltRectangle) Builder.rectangle()
            .size(new SizeState(30, 22))
            .color(new QuadColorState(new Color(addCol, true)))
            .radius(new QuadRadiusState(4f))
            .smoothness(1.15f)
            .build()).render(matrix, wX + wW - 45, wY + 35);

        ((BuiltText) Builder.text().font(regularFont).text("Add")
            .color(TEXT_WHITE).size(7f).thickness(0.05f)
            .build()).render(matrix, wX + wW - 30 - regularFont.getWidth("Add", 7f) / 2f, wY + 42);

        RenderSystem.enableScissor((int) wX, (int) (height - wY - wH + 10), (int) wW, (int) (wH - 75));

        float cY = wY + 70 + scrollAlt;
        List<MeoRayAccount> alts = AccountManager.getAccounts();
        for (MeoRayAccount alt : alts) {
            boolean isCur = client.getSession().getUsername().equals(alt.getUsername());
            boolean rHov = mx >= wX + 15 && mx <= wX + wW - 15
                && my >= cY && my <= cY + 24;

            int rCol = isCur ? 0xFFE65C00 : (rHov ? 0xFF282C3A : 0xFF1E212C);
            ((BuiltRectangle) Builder.rectangle()
                .size(new SizeState(wW - 30, 24))
                .color(new QuadColorState(new Color(rCol, true)))
                .radius(new QuadRadiusState(4f))
                .smoothness(1.15f)
                .build()).render(matrix, wX + 15, cY);

            ((BuiltText) Builder.text().font(regularFont).text(alt.getUsername())
                .color(TEXT_WHITE).size(7f).thickness(0.05f)
                .build()).render(matrix, wX + 25, cY + 7);

            boolean dH = mx >= wX + wW - 35 && mx <= wX + wW - 19
                && my >= cY + 4 && my <= cY + 20;
            ((BuiltText) Builder.text().font(regularFont).text("X")
                .color(new Color(dH ? 0xFFFF3232 : 0xFFB43232, true)).size(7f).thickness(0.05f)
                .build()).render(matrix, wX + wW - 25, cY + 7);

            cY += 28;
        }

        RenderSystem.disableScissor();
    }

    // ──────────────────────────────────────────────
    // Mouse events
    // ──────────────────────────────────────────────

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button != 0) return super.mouseClicked(mx, my, button);

        if (altMenuOpen) return handleAltClick(mx, my);

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
        if (altMenuOpen) {
            scrollAlt += (float) vert * 20;
            return true;
        }
        return super.mouseScrolled(mx, my, horiz, vert);
    }

    private boolean handleAltClick(double mx, double my) {
        float wW = 220;
        float wH = 280;
        float wX = width / 2f - wW / 2f;
        float wY = height / 2f - wH / 2f;

        if (mx < wX || mx > wX + wW || my < wY || my > wY + wH) {
            altMenuOpen = false;
            return true;
        }

        if (mx >= wX + wW - 45 && mx <= wX + wW - 15 && my >= wY + 35 && my <= wY + 57) {
            if (altInput.length() >= 3) {
                AccountManager.addAccount(altInput);
                altInput = "";
            }
            return true;
        }

        float cY = wY + 70 + scrollAlt;
        MeoRayAccount toRemove = null;
        for (MeoRayAccount alt : AccountManager.getAccounts()) {
            if (my >= wY + 65 && my <= wY + wH - 10) {
                if (mx >= wX + wW - 35 && mx <= wX + wW - 19 && my >= cY + 4 && my <= cY + 20) {
                    toRemove = alt;
                } else if (mx >= wX + 15 && mx <= wX + wW - 15 && my >= cY && my <= cY + 24) {
                    AccountManager.setActiveAccount(alt);
                }
            }
            cY += 28;
        }
        if (toRemove != null) {
            AccountManager.removeAccount(toRemove);
        }
        return true;
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (altMenuOpen) {
            if (regularFont.getWidth(altInput, 7f) < 130) {
                altInput += codePoint;
            }
            return true;
        }
        return super.charTyped(codePoint, modifiers);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (altMenuOpen) {
            if (keyCode == GLFW.GLFW_KEY_BACKSPACE && !altInput.isEmpty()) {
                altInput = altInput.substring(0, altInput.length() - 1);
            } else if (keyCode == GLFW.GLFW_KEY_ENTER) {
                if (altInput.length() >= 3) {
                    AccountManager.addAccount(altInput);
                    AccountManager.save();
                    altInput = "";
                }
            } else if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                altMenuOpen = false;
            }
            return true;
        }
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
