package dev.meoray.client.core.setting;

public class ModeSetting extends Setting<String> {
    private final String[] modes;

    public ModeSetting(String name, String defaultValue, String... modes) {
        super(name, defaultValue);
        this.modes = modes;
    }

    public String[] getModes() {
        return modes;
    }

    public void cycle() {
        int i = 0;
        for (; i < modes.length; i++) {
            if (modes[i].equals(value)) break;
        }
        value = modes[(i + 1) % modes.length];
    }
}
