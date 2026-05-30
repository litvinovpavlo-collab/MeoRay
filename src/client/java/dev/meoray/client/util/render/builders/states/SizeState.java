package dev.meoray.client.util.render.builders.states;

public record SizeState(float width, float height) {
    public static final SizeState NONE = new SizeState(0.0F, 0.0F);

    public SizeState(double width, double height) {
        this((float)width, (float)height);
    }
}
