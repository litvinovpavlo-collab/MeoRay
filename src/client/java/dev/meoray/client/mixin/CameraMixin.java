package dev.meoray.client.mixin;

import dev.meoray.client.feature.render.FreeCam;
import net.minecraft.client.render.Camera;
import net.minecraft.entity.Entity;
import net.minecraft.world.BlockView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Camera.class)
public abstract class CameraMixin {

    @Invoker("setPos")
    public abstract void invokeSetPos(double x, double y, double z);

    @Invoker("setRotation")
    public abstract void invokeSetRotation(float yaw, float pitch);

    @Inject(method = "update", at = @At("RETURN"))
    private void meoray$applyFreeCam(BlockView area, Entity focusedEntity,
                                      boolean thirdPerson, boolean inverseView,
                                      float tickDelta, CallbackInfo ci) {
        FreeCam fc = FreeCam.INSTANCE;
        if (fc == null || !fc.isEnabled()) return;

        invokeSetPos(fc.getCamPos().x, fc.getCamPos().y, fc.getCamPos().z);
        invokeSetRotation(fc.getCamYaw(), fc.getCamPitch());
    }
}
