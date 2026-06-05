package dev.meoray.client.mixin;

import dev.meoray.client.MeoRayClient;
import dev.meoray.client.gui.screen.MeoRayClickGUI;
import dev.meoray.client.hud.draggable.DraggableManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.Mouse;
import net.minecraft.client.gui.screen.ChatScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Mouse.class)
public class MouseMixin {
    @Inject(method = "onMouseButton", at = @At("HEAD"))
    private void onMouseButton(long window, int button, int action, int mods, CallbackInfo ci) {
        if (window != MinecraftClient.getInstance().getWindow().getHandle()) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        DraggableManager mgr = MeoRayClient.INSTANCE.getDraggableManager();
        if (mgr == null) return;
        boolean editable = mc.currentScreen instanceof ChatScreen || mc.currentScreen instanceof MeoRayClickGUI;

        if (action == 1 && editable) {
            double mx = mc.mouse.getX() / MeoRayClient.mainScale;
            double my = mc.mouse.getY() / MeoRayClient.mainScale;
            mgr.handleClick(button, mx, my);
        }
        if (action == 0 && editable) {
            mgr.endDrag();
        }
    }
}
