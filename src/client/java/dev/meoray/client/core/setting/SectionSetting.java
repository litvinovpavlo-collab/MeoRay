package dev.meoray.client.core.setting;

import java.util.ArrayList;
import java.util.List;

public class SectionSetting extends Setting<Boolean> {
    private final List<Setting<?>> children = new ArrayList<>();
    private final String icon;

    public SectionSetting(String name) {
        super(name, false);
        this.icon = null;
    }

    public SectionSetting(String name, String icon) {
        super(name, false);
        this.icon = icon;
    }

    public <T extends Setting<?>> T add(T setting) {
        children.add(setting);
        return setting;
    }

    public List<Setting<?>> getChildren() {
        return children;
    }

    public String getIcon() {
        return icon;
    }

    public boolean isExpanded() {
        return value;
    }

    public void setExpanded(boolean expanded) {
        this.value = expanded;
    }

    public String getPreview() {
        int active = 0;
        int total = 0;
        for (Setting<?> s : children) {
            if (s instanceof BooleanSetting bs) {
                total++;
                if (bs.getValue()) active++;
            }
        }
        if (total > 0 && active != total) return active + "/" + total;
        if (total > 0) return "All";
        return "";
    }
}
