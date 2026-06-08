package dev.meoray.client.rotation;

public class RotationDelta {
    private final float deltaYaw;
    private final float deltaPitch;

    public RotationDelta(float deltaYaw, float deltaPitch) {
        this.deltaYaw = deltaYaw;
        this.deltaPitch = deltaPitch;
    }

    public float length() {
        return (float) Math.sqrt(this.deltaYaw * this.deltaYaw + this.deltaPitch * this.deltaPitch);
    }

    public float getDeltaYaw() { return this.deltaYaw; }
    public float getDeltaPitch() { return this.deltaPitch; }
}
