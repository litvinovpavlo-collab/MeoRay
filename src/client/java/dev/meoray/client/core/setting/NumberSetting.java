package dev.meoray.client.core.setting;

public class NumberSetting extends Setting<Double> {
    private final double min, max, step;

    public NumberSetting(String name, double defaultValue, double min, double max, double step) {
        super(name, defaultValue);
        this.min = min;
        this.max = max;
        this.step = step;
    }

    public double getMin() { return min; }
    public double getMax() { return max; }
    public double getStep() { return step; }

    @Override
    public void setValue(Double value) {
        super.setValue(Math.max(min, Math.min(max, Math.round(value / step) * step)));
    }

    public String formatValue() {
        return String.format("%.1f", value);
    }

    public void setFromSlider(float progress) {
        double range = max - min;
        double raw = min + range * progress;
        double stepped = Math.round(raw / step) * step;
        value = Math.max(min, Math.min(max, stepped));
    }
}
