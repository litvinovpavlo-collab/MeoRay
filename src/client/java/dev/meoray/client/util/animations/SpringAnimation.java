package dev.meoray.client.util.animations;

public class SpringAnimation {
    private float value;
    private float velocity;
    private final float stiffness;
    private final float damping;

    public SpringAnimation() {
        this(0.12f, 0.82f);
    }

    public SpringAnimation(float stiffness, float damping) {
        this.stiffness = stiffness;
        this.damping = damping;
    }

    public float update(float target) {
        float force = (target - value) * stiffness;
        velocity += force;
        velocity *= damping;
        value += velocity;
        return value;
    }

    public float getValue() {
        return value;
    }

    public void setValue(float value) {
        this.value = value;
    }

    public void setVelocity(float velocity) {
        this.velocity = velocity;
    }
}
