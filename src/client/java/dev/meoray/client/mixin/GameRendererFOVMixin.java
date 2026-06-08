package dev.meoray.client.mixin;

import dev.meoray.client.feature.render.AspectRatio;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GameRenderer.class)
public abstract class GameRendererFOVMixin {

    @Inject(method = "getFov", at = @At("RETURN"), cancellable = true)
    private void meoray$adjustFOV(Camera camera, float tickDelta, boolean changingFov,
                                    CallbackInfoReturnable<Double> cir) {
        AspectRatio ar = AspectRatio.INSTANCE;
        if (ar == null || !ar.isActiveNow() || !ar.shouldAffectFOV()) return;

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.getWindow() == null) return;

        float screenAspect = (float) mc.getWindow().getFramebufferWidth()
                / mc.getWindow().getFramebufferHeight();
        float targetAspect = ar.getCurrentAspect();

        if (targetAspect > screenAspect) {
            float ratio = targetAspect / screenAspect;
            double adjustedFov = cir.getReturnValue() * ratio;

            adjustedFov = Math.min(adjustedFov, 170.0);
            cir.setReturnValue(adjustedFov);
        }
    }
}
