package dev.meoray.client.util.animations;

public enum Direction {
    FORWARDS,
    BACKWARDS;

    public Direction opposite() {
        return this == FORWARDS ? BACKWARDS : FORWARDS;
    }
}
