package dev.meoray.client.core;

import dev.meoray.client.core.setting.Setting;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class Module {
    private final String name;
    private final String description;
    private final Category category;
    private final List<Setting<?>> settings = new ArrayList<>();
    private int key;
    private boolean enabled;
    private boolean extended;

    protected Module(String name, String description, Category category) {
        this.name = name;
        this.description = description;
        this.category = category;
    }

    protected <T extends Setting<?>> T add(T s) {
        settings.add(s);
        return s;
    }

    public final void toggle() {
        setEnabled(!enabled);
    }

    public final void setEnabled(boolean enabled) {
        if (this.enabled == enabled) return;
        this.enabled = enabled;
        if (enabled) onEnable();
        else onDisable();
    }

    protected void onEnable() {}
    protected void onDisable() {}
    public void onTick() {}

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
}
