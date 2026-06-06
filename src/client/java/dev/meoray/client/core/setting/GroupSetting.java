package dev.meoray.client.core.setting;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Группа булеанов внутри одной строки настроек.
 * В GUI рисуется как dropdown: "Targets [Players, Mobs ▼]"
 * При клике раскрывается список чекбоксов.
 */
public class GroupSetting extends Setting<String> {
    private final String description;
    private final LinkedHashMap<String, Boolean> options = new LinkedHashMap<>();

    public GroupSetting(String name, String description, String... defaultEnabled) {
        super(name, "");
        this.description = description;
        for (String key : defaultEnabled) {
            options.put(key, true);
        }
    }

    public GroupSetting add(String key, boolean enabled) {
        options.put(key, enabled);
        return this;
    }

    public boolean get(String key) {
        return options.getOrDefault(key, false);
    }

    public void set(String key, boolean value) {
        if (options.containsKey(key)) {
            options.put(key, value);
        }
    }

    public void toggle(String key) {
        set(key, !get(key));
    }

    public LinkedHashMap<String, Boolean> getOptions() {
        return options;
    }

    public String getDescription() {
        return description;
    }

    /** Короткая строка для отображения в свёрнутом виде, типа "Players, Mobs..." */
    public String getDisplayValue() {
        var enabled = options.entrySet().stream()
                .filter(Map.Entry::getValue)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        if (enabled.isEmpty()) return "None";
        if (enabled.size() == 1) return enabled.get(0);
        if (enabled.size() == 2) return enabled.get(0) + ", " + enabled.get(1);
        return enabled.get(0) + ", " + enabled.get(1) + "...";
    }
}
