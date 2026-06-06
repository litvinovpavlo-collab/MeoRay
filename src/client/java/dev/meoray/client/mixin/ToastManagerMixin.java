package dev.meoray.client.mixin;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.toast.Toast;
import net.minecraft.client.toast.ToastManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ToastManager.class)
public class ToastManagerMixin {

    @Inject(method = "draw", at = @At("HEAD"), cancellable = true)
    private void cancelDraw(DrawContext context, CallbackInfo ci) {
        ci.cancel();
    }

    @Inject(method = "add", at = @At("HEAD"), cancellable = true)
    private void cancelAdd(Toast toast, CallbackInfo ci) {
        ci.cancel();
    }
}
