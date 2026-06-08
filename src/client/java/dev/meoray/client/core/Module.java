package dev.meoray.client.core;

import dev.meoray.client.core.setting.Setting;
import dev.meoray.client.notify.NotifyManager;
import dev.meoray.client.notify.Status;
import net.minecraft.client.MinecraftClient;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class Module {
    protected static final MinecraftClient mc = MinecraftClient.getInstance();

    private final String name;
    private final String description;
    private final Category category;
    private final List<Setting<?>> settings = new ArrayList<>();
    private int key;
    private boolean enabled;
    private boolean extended;
    private boolean silent;

    protected Module(String name, String description, Category category) {
        this.name = name;
        this.description = description;
        this.category = category;
    }

    protected <T extends Setting<?>> T add(T s) {
        settings.add(s);
        return s;
    }

    public void addSettings(Setting<?>... settings) {
        Collections.addAll(this.settings, settings);
    }

    public final void toggle() {
        setEnabled(!enabled);
    }

    public final void setEnabled(boolean enabled) {
        if (this.enabled == enabled) return;
        this.enabled = enabled;
        if (enabled) {
            onEnable();
            postEnableNotify();
        } else {
            onDisable();
            postDisableNotify();
        }
    }

    protected void onEnable() {}
    protected void onDisable() {}
    public void onTick() {}
    public void onTickMovement() {}
    public void eventRotate() {}
    public void onMoveInput() {}

    public String getName() { return name; }
    public String getDescription() { return description; }
    public Category getCategory() { return category; }
    public List<Setting<?>> getSettings() { return Collections.unmodifiableList(settings); }
    public int getKey() { return key; }
    public void setKey(int key) { this.key = key; }
    public boolean isEnabled() { return enabled; }
    public boolean isExtended() { return extended; }
    public void setExtended(boolean extended) { this.extended = extended; }
    public String getHudSuffix() {
        return settings.stream()
                .filter(s -> s instanceof dev.meoray.client.core.setting.ModeSetting)
                .map(s -> ((dev.meoray.client.core.setting.ModeSetting) s).getValue())
                .findFirst()
                .orElse("");
    }

    public void setSilent(boolean s) { this.silent = s; }
    public boolean isSilent() { return silent; }

    private String getKeyDisplay() {
        if (key <= 0) return "";
        return getKeyName(key);
    }

    public static String getKeyName(int keyCode) {
        String name = org.lwjgl.glfw.GLFW.glfwGetKeyName(keyCode, 0);
        if (name != null) return name.toUpperCase();
        switch (keyCode) {
            case org.lwjgl.glfw.GLFW.GLFW_KEY_SPACE: return "SPACE";
            case org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT_SHIFT: return "LSHIFT";
            case org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT_SHIFT: return "RSHIFT";
            case org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT_CONTROL: return "LCTRL";
            case org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT_CONTROL: return "RCTRL";
            case org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT_ALT: return "LALT";
            case org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT_ALT: return "RALT";
            case org.lwjgl.glfw.GLFW.GLFW_KEY_TAB: return "TAB";
            case org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE: return "ESC";
            case org.lwjgl.glfw.GLFW.GLFW_KEY_ENTER: return "ENTER";
            case org.lwjgl.glfw.GLFW.GLFW_KEY_BACKSPACE: return "BKSP";
            case org.lwjgl.glfw.GLFW.GLFW_KEY_INSERT: return "INS";
            case org.lwjgl.glfw.GLFW.GLFW_KEY_DELETE: return "DEL";
            case org.lwjgl.glfw.GLFW.GLFW_KEY_HOME: return "HOME";
            case org.lwjgl.glfw.GLFW.GLFW_KEY_END: return "END";
            case org.lwjgl.glfw.GLFW.GLFW_KEY_PAGE_UP: return "PGUP";
            case org.lwjgl.glfw.GLFW.GLFW_KEY_PAGE_DOWN: return "PGDN";
            case org.lwjgl.glfw.GLFW.GLFW_KEY_UP: return "UP";
            case org.lwjgl.glfw.GLFW.GLFW_KEY_DOWN: return "DOWN";
            case org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT: return "LEFT";
            case org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT: return "RIGHT";
            case org.lwjgl.glfw.GLFW.GLFW_KEY_F1: return "F1";
            case org.lwjgl.glfw.GLFW.GLFW_KEY_F2: return "F2";
            case org.lwjgl.glfw.GLFW.GLFW_KEY_F3: return "F3";
            case org.lwjgl.glfw.GLFW.GLFW_KEY_F4: return "F4";
            case org.lwjgl.glfw.GLFW.GLFW_KEY_F5: return "F5";
            case org.lwjgl.glfw.GLFW.GLFW_KEY_F6: return "F6";
            case org.lwjgl.glfw.GLFW.GLFW_KEY_F7: return "F7";
            case org.lwjgl.glfw.GLFW.GLFW_KEY_F8: return "F8";
            case org.lwjgl.glfw.GLFW.GLFW_KEY_F9: return "F9";
            case org.lwjgl.glfw.GLFW.GLFW_KEY_F10: return "F10";
            case org.lwjgl.glfw.GLFW.GLFW_KEY_F11: return "F11";
            case org.lwjgl.glfw.GLFW.GLFW_KEY_F12: return "F12";
            case org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT_BRACKET: return "[";
            case org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT_BRACKET: return "]";
            case org.lwjgl.glfw.GLFW.GLFW_KEY_SEMICOLON: return ";";
            case org.lwjgl.glfw.GLFW.GLFW_KEY_APOSTROPHE: return "'";
            case org.lwjgl.glfw.GLFW.GLFW_KEY_COMMA: return ",";
            case org.lwjgl.glfw.GLFW.GLFW_KEY_PERIOD: return ".";
            case org.lwjgl.glfw.GLFW.GLFW_KEY_SLASH: return "/";
            case org.lwjgl.glfw.GLFW.GLFW_KEY_BACKSLASH: return "\\";
            case org.lwjgl.glfw.GLFW.GLFW_KEY_GRAVE_ACCENT: return "`";
            case org.lwjgl.glfw.GLFW.GLFW_KEY_MINUS: return "-";
            case org.lwjgl.glfw.GLFW.GLFW_KEY_EQUAL: return "=";
            default: return "KEY_" + keyCode;
        }
    }

    private void postEnableNotify() {
        String bind = getKeyDisplay();
        NotifyManager.get().addSuccess(name, bind);
    }

    private void postDisableNotify() {
        String bind = getKeyDisplay();
        NotifyManager.get().add(name + " disabled", bind, Status.WARNING);
    }
}
