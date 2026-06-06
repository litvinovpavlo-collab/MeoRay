package dev.meoray.client.core.setting;

public abstract class Setting<T> {
    protected final String name;
    protected T value;
    private java.util.function.Supplier<Boolean> visibility = () -> true;

    public Setting(String name, T defaultValue) {
        this.name = name;
        this.value = defaultValue;
    }

    public String getName() {
        return name;
    }

    public T getValue() {
        return value;
    }

    public void setValue(T value) {
        this.value = value;
    }

    public Setting<T> visibleWhen(java.util.function.Supplier<Boolean> condition) {
        this.visibility = condition;
        return this;
    }

    public boolean isVisible() {
        return visibility.get();
    }
}
