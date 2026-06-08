package dev.meoray.client.core.setting;

public class BooleanSetting extends Setting<Boolean> {
    public BooleanSetting(String name, boolean defaultValue) {
        super(name, defaultValue);
    }

    public void toggle() {
        setValue(!getValue());
    }

    @Override
    public BooleanSetting visibleWhen(java.util.function.Supplier<Boolean> condition) {
        super.visibleWhen(condition);
        return this;
    }
}
