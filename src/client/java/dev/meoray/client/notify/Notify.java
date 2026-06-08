package dev.meoray.client.notify;

public class Notify {
    public final String text;
    public final String bind;
    public final Status status;
    public final long createdMs;
    public final long durationMs;
    public float progress;
    public boolean dead;

    public Notify(String text, String bind, Status status) {
        this(text, bind, status, 2500L);
    }

    public Notify(String text, String bind, Status status, long durationMs) {
        this.text = text;
        this.bind = bind;
        this.status = status;
        this.durationMs = durationMs;
        this.createdMs = System.currentTimeMillis();
        this.progress = 0f;
        this.dead = false;
    }

    public float getFadeIn() {
        long elapsed = System.currentTimeMillis() - createdMs;
        return Math.min(1f, elapsed / 200f);
    }

    public float getFadeOut() {
        long elapsed = System.currentTimeMillis() - createdMs;
        long remaining = durationMs - elapsed;
        if (remaining > 400) return 1f;
        return Math.max(0f, remaining / 400f);
    }

    public boolean isExpired() {
        return System.currentTimeMillis() - createdMs > durationMs;
    }
}
