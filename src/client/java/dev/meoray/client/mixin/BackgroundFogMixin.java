package dev.meoray.client.mixin;

import dev.meoray.client.MeoRayClient;
import dev.meoray.client.feature.render.BetterWorld;
import net.minecraft.client.render.BackgroundRenderer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.world.ClientWorld;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BackgroundRenderer.class)
public class BackgroundFogMixin {

    @Inject(method = "getFogColor", at = @At("RETURN"), cancellable = true)
    private static void onGetFogColor(Camera camera, float tickDelta, ClientWorld world, int i, float f, CallbackInfoReturnable<Vector4f> cir) {
        BetterWorld bw = BetterWorld.getInstance();
        if (bw != null && bw.isFogEnabled()) {
            Vector4f orig = cir.getReturnValue();
            float alpha = bw.getFogAlpha();

            int accent = MeoRayClient.INSTANCE.getThemeManager().getRenderTheme().accent();
            float ar = ((accent >> 16) & 0xFF) / 255f;
            float ag = ((accent >> 8) & 0xFF) / 255f;
            float ab = (accent & 0xFF) / 255f;

            float blend = 0.35f;
            cir.setReturnValue(new Vector4f(
                orig.x * (1 - blend) + ar * blend * alpha,
                orig.y * (1 - blend) + ag * blend * alpha,
                orig.z * (1 - blend) + ab * blend * alpha,
                orig.w
            ));
        }
    }
}
