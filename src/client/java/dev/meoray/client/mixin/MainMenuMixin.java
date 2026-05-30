package dev.meoray.client.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import dev.meoray.client.gui.screen.CustomTitleScreen;

@Mixin(MinecraftClient.class)
public class MainMenuMixin {

    @Inject(method = "setScreen", at = @At("HEAD"), cancellable = true)
    private void nova$onSetScreen(Screen screen, CallbackInfo ci) {
        if (screen instanceof TitleScreen && !(screen instanceof CustomTitleScreen)) {
            ((MinecraftClient)(Object)this).setScreen(new CustomTitleScreen());
            ci.cancel();
        }
    }
}
