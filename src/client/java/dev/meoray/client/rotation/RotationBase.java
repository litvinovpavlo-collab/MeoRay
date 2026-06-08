package dev.meoray.client.rotation;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.MathHelper;

import java.security.SecureRandom;

public abstract class RotationBase {
    protected static final MinecraftClient mc = MinecraftClient.getInstance();
    protected final SecureRandom rng = new SecureRandom();
    protected float lastYaw;
    protected float lastPitch;
    protected boolean noCircling;

    public abstract void update(Rotation targetAngle, boolean elytraVisual);

    /** Default: ignores the entity, delegates to the rotation-only update. */
    public void update(LivingEntity target, Rotation targetAngle, boolean elytraVisual) {
        update(targetAngle, elytraVisual);
    }

    public void snapToCurrent() {
        if (mc.player != null) {
            this.lastYaw = mc.player.getYaw();
            this.lastPitch = mc.player.getPitch();
        }
    }

    public float getYaw() { return this.lastYaw; }
    public float getPitch() { return this.lastPitch; }
    public void setYaw(float yaw) { this.lastYaw = yaw; }
    public void setPitch(float pitch) { this.lastPitch = pitch; }
    public void setNoCircling(boolean noCircling) { this.noCircling = noCircling; }

    protected float clampYaw(float y) { return y; }
    protected float clampPitch(float p) { return MathHelper.clamp(p, -90.0F, 90.0F); }

    public Rotation current() {
        return new Rotation(this.lastYaw, this.lastPitch);
    }
}
