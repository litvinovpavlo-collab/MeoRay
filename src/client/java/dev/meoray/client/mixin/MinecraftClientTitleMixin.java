package dev.meoray.client.mixin;

import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftClient.class)
public class MinecraftClientTitleMixin {
    @Inject(method = "updateWindowTitle", at = @At("HEAD"), cancellable = true)
    private void nova$cancelTitle(CallbackInfo ci) {
        ci.cancel();
    }
}
