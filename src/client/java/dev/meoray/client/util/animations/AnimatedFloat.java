package dev.meoray.client.util.animations;

public class AnimatedFloat {

    private float value;
    private float from;
    private float to;
    private long startTime;
    private long duration;
    private Easing easing;
    private boolean animating;

    public AnimatedFloat(float initial) {
        this.value = initial;
        this.from = initial;
        this.to = initial;
        this.animating = false;
    }

    public void animate(float target, long durationMs, Easing easingCurve) {
        this.from = value;
        this.to = target;
        this.duration = Math.max(1, durationMs);
        this.easing = easingCurve;
        this.startTime = System.currentTimeMillis();
        this.animating = true;
    }

    public void snapTo(float target) {
        this.value = target;
        this.from = target;
        this.to = target;
        this.animating = false;
    }

    public float update() {
        if (!animating) return value;

        float elapsed = (float) (System.currentTimeMillis() - startTime);
        float t = Math.min(elapsed / duration, 1f);
        float mapped = easing.apply(t);
        value = from + (to - from) * mapped;

        if (t >= 1f) {
            value = to;
            animating = false;
        }

        return value;
    }

    public boolean isFinished() {
        return !animating;
    }

    public float getValue() {
        return value;
    }

    public boolean isAnimating() {
        return animating;
    }
}
