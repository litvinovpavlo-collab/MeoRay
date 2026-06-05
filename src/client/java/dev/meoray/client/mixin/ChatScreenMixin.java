package dev.meoray.client.mixin;

import dev.meoray.client.MeoRayClient;
import dev.meoray.client.hud.draggable.DraggableManager;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChatScreen.class)
public class ChatScreenMixin {
    @Inject(method = "render", at = @At("HEAD"))
    private void onRender(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        DraggableManager mgr = MeoRayClient.INSTANCE.getDraggableManager();
        if (mgr == null) return;
        float ms = MeoRayClient.mainScale;
        mgr.updatePositions(mouseX / ms, mouseY / ms);
        mgr.renderPanels(context);
    }
}
