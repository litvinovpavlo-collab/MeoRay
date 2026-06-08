package dev.meoray.client.rotation.impl;

import dev.meoray.client.rotation.Rotation;
import dev.meoray.client.rotation.RotationBase;
import dev.meoray.client.rotation.RotationComponent;
import net.minecraft.entity.LivingEntity;

public class ReallyWorldRotation extends RotationBase {
    @Override
    public void update(Rotation targetAngle, boolean elytraVisual) {
        update(null, targetAngle, elytraVisual);
    }

    public void update(LivingEntity target, Rotation targetAngle, boolean elytraVisual) {
        RotationComponent.update(targetAngle, 360.0F, 360.0F, 360.0F, 360.0F, 0, 1, elytraVisual);
        this.lastYaw = targetAngle.getYaw();
        this.lastPitch = targetAngle.getPitch();
    }
}
