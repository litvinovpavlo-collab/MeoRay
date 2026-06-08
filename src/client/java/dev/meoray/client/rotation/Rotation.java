package dev.meoray.client.rotation;

import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public class Rotation {
    public final float yaw;
    public final float pitch;
    private final boolean isNormalized;

    public static final Rotation ZERO = new Rotation(0.0F, 0.0F);

    public Rotation(float yaw, float pitch) {
        this(yaw, pitch, false);
    }

    public Rotation(float yaw, float pitch, boolean isNormalized) {
        this.yaw = yaw;
        this.pitch = Math.max(-90, Math.min(90, pitch));
        this.isNormalized = isNormalized;
    }

    public Rotation normalize() {
        return new Rotation(wrapDegrees(yaw), pitch);
    }

    public Rotation copy() {
        return new Rotation(yaw, pitch);
    }

    public static float wrapDegrees(float value) {
        return wrapAngleTo180(value);
    }

    public static float wrapAngleTo180(float value) {
        value %= 360;
        if (value >= 180) value -= 360;
        if (value < -180) value += 360;
        return value;
    }

    // === Wyvern-style static constructors ===

    public static Rotation lookingAt(Vec3d point, Vec3d from) {
        return fromRotationVec(point.subtract(from));
    }

    public static Rotation fromRotationVec(Vec3d lookVec) {
        double diffX = lookVec.x;
        double diffY = lookVec.y;
        double diffZ = lookVec.z;
        return new Rotation(
                (float) MathHelper.wrapDegrees(Math.toDegrees(Math.atan2(diffZ, diffX)) - 90.0D),
                (float) MathHelper.wrapDegrees(-Math.toDegrees(Math.atan2(diffY, Math.sqrt(diffX * diffX + diffZ * diffZ)))));
    }

    public static Rotation fromVec3d(Vec3d vector) {
        return fromRotationVec(vector);
    }

    public static Rotation calculateAngle(Vec3d to) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return ZERO;
        return fromVec3d(to.subtract(mc.player.getEyePos()));
    }

    public static Rotation getRotations(Vec3d vec3d) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return ZERO;
        double deltaX = vec3d.x - mc.player.getX();
        double deltaY = vec3d.y - mc.player.getEyeY();
        double deltaZ = vec3d.z - mc.player.getZ();
        double distance = MathHelper.sqrt((float) (deltaX * deltaX + deltaZ * deltaZ));
        float yaw = (float) (MathHelper.atan2(deltaZ, deltaX) * 57.29577951308232D - 90.0D);
        float pitch = (float) (-MathHelper.atan2(deltaY, distance) * 57.29577951308232D);
        return new Rotation(yaw, pitch);
    }

    // === Wyvern-style instance methods ===

    public float angleTo(Rotation other) {
        return Math.min(this.rotationDeltaTo(other).length(), 180.0F);
    }

    public RotationDelta rotationDeltaTo(Rotation other) {
        return new RotationDelta(angleDifference(other.yaw, this.yaw), angleDifference(other.pitch, this.pitch));
    }

    public float getDelta(Rotation target) {
        float yawDelta = MathHelper.wrapDegrees(target.yaw - this.yaw);
        float pitchDelta = target.pitch - this.pitch;
        return (float) Math.hypot(Math.abs(yawDelta), Math.abs(pitchDelta));
    }

    private static float angleDifference(float a, float b) {
        return MathHelper.wrapDegrees(a - b);
    }

    public boolean approximatelyEquals(Rotation other, float tolerance) {
        return this.angleTo(other) <= tolerance;
    }

    public boolean isNormalized() {
        return this.isNormalized;
    }

    public Vec3d getDirectionVector() {
        return Vec3d.fromPolar(this.pitch, this.yaw);
    }

    public Vec3d toVector() {
        float f = this.pitch * 0.017453292F;
        float g = -this.yaw * 0.017453292F;
        float h = MathHelper.cos(g);
        float i = MathHelper.sin(g);
        float j = MathHelper.cos(f);
        float k = MathHelper.sin(f);
        return new Vec3d(i * j, -k, h * j);
    }

    public Rotation towardsLinear(Rotation other, float horizontalFactor, float verticalFactor) {
        RotationDelta diff = this.rotationDeltaTo(other);
        float rotationDifference = diff.length();
        if (rotationDifference < 0.0001F) return this;
        float straightLineYaw = Math.abs(diff.getDeltaYaw() / rotationDifference) * horizontalFactor;
        float straightLinePitch = Math.abs(diff.getDeltaPitch() / rotationDifference) * verticalFactor;
        float limitedYaw = MathHelper.clamp(diff.getDeltaYaw(), -straightLineYaw, straightLineYaw);
        float limitedPitch = MathHelper.clamp(diff.getDeltaPitch(), -straightLinePitch, straightLinePitch);
        return new Rotation(this.yaw + limitedYaw, this.pitch + limitedPitch);
    }

    public boolean check() {
        return Float.isInfinite(this.yaw) || Float.isNaN(this.yaw)
                || Float.isInfinite(this.pitch) || Float.isNaN(this.pitch);
    }

    public static float gcd() {
        MinecraftClient mc = MinecraftClient.getInstance();
        double sens = mc.options.getMouseSensitivity().getValue() * 0.6D + 0.2D;
        return (float) (sens * sens * sens * 8.0D * 0.15D);
    }

    public Rotation normalize(Rotation currentRotation) {
        if (!this.isNormalized && !this.equals(currentRotation)) {
            RotationDelta rotationDelta = currentRotation.rotationDeltaTo(this);
            double g = gcd();
            int targetX = (int) (rotationDelta.getDeltaYaw() / g);
            int targetY = (int) (rotationDelta.getDeltaPitch() / g);
            return new Rotation((float)(currentRotation.yaw + targetX * g), (float)(currentRotation.pitch + targetY * g), true);
        }
        return this;
    }

    public Rotation add(RotationDelta diff) {
        return new Rotation(this.yaw + diff.getDeltaYaw(), this.pitch + diff.getDeltaPitch());
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Rotation o2)) return false;
        return o2.yaw == this.yaw && o2.pitch == this.pitch;
    }

    public float getYaw() { return this.yaw; }
    public float getPitch() { return this.pitch; }
}
