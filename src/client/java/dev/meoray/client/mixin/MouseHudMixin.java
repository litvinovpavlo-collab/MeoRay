package dev.meoray.client.mixin;

import dev.meoray.client.MeoRayClient;
import net.minecraft.client.Mouse;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.lwjgl.glfw.GLFW;

@Mixin(Mouse.class)
public class MouseHudMixin {

    @Inject(method = "onMouseButton", at = @At("HEAD"))
    private void onMouseButton(long window, int button, int action, int mods, CallbackInfo ci) {
        if (MeoRayClient.INSTANCE == null || MeoRayClient.INSTANCE.getDraggableManager() == null) return;

        MinecraftClient mc = MinecraftClient.getInstance();
        double rawMx = mc.mouse.getX();
        double rawMy = mc.mouse.getY();
        int winW = mc.getWindow().getWidth();
        int winH = mc.getWindow().getHeight();
        int scW = mc.getWindow().getScaledWidth();
        int scH = mc.getWindow().getScaledHeight();
        float ms = MeoRayClient.mainScale;
        double mx = rawMx * scW / winW / ms;
        double my = rawMy * scH / winH / ms;

        if (action == GLFW.GLFW_PRESS) {
            MeoRayClient.INSTANCE.getDraggableManager().onMouseClick(button, mx, my);
        } else if (action == GLFW.GLFW_RELEASE) {
            MeoRayClient.INSTANCE.getDraggableManager().onRelease();
        }
    }
}
