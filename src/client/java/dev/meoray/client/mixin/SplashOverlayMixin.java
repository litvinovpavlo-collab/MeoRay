package dev.meoray.client.mixin;

import net.minecraft.client.gui.screen.SplashOverlay;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SplashOverlay.class)
public class SplashOverlayMixin {

    @Mutable
    @Shadow @Final private static int MOJANG_RED;

    @Inject(method = "<clinit>", at = @At("TAIL"))
    private static void nova$changeBackgroundColor(CallbackInfo ci) {
        MOJANG_RED = 0xFF000000;
    }
}
