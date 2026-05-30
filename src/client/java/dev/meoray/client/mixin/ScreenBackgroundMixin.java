package dev.meoray.client.mixin;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import dev.meoray.client.gui.screen.CustomTitleScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Screen.class)
public abstract class ScreenBackgroundMixin {

    @Inject(method = "renderInGameBackground", at = @At("HEAD"), cancellable = true, require = 0)
    private void nova$skipInGameBackground(DrawContext context, CallbackInfo ci) {
        Screen self = (Screen) (Object) this;
        if (self instanceof CustomTitleScreen) {
            ci.cancel();
        }
    }
}
