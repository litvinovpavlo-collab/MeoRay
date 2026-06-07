package dev.meoray.client.mixin;

import dev.meoray.client.MeoRayClient;
import dev.meoray.client.core.Module;
import dev.meoray.client.core.setting.Setting;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderTickCounter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameHud.class)
public class InGameHudHotbarMixin {

    @Inject(method = "renderHotbar", at = @At("HEAD"), cancellable = true)
    private void meoray$cancelVanillaHotbar(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        if (isCustomHotbarOn()) ci.cancel();
    }

    private boolean isCustomHotbarOn() {
        try {
            if (MeoRayClient.INSTANCE == null) return false;
            Module iface = MeoRayClient.INSTANCE.moduleManager.getByName("Interface");
            if (iface == null || !iface.isEnabled()) return false;
            for (Setting<?> s : iface.getSettings()) {
                if (s.getName().equals("Hotbar") && s.getValue() instanceof Boolean b) {
                    return b;
                }
            }
        } catch (Throwable ignored) {}
        return false;
    }
}
