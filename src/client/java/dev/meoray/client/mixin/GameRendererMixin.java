package dev.meoray.client.mixin;

import dev.meoray.client.feature.render.FreeCam;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.RenderTickCounter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {

    @Inject(method = "render", at = @At("HEAD"))
    private void meoray$updateFreeCamEveryFrame(RenderTickCounter tickCounter,
                                                  boolean tick, CallbackInfo ci) {
        FreeCam fc = FreeCam.INSTANCE;
        if (fc != null && fc.isEnabled()) {
            fc.onFrameUpdate();
        }
    }
}
