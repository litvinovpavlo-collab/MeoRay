package dev.meoray.client.mixin;

import dev.meoray.client.feature.render.AspectRatio;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderTickCounter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = InGameHud.class, priority = 900)
public abstract class InGameHudClipMixin {

    @Inject(method = "render", at = @At("HEAD"))
    private void meoray$enableHudClip(DrawContext context, RenderTickCounter tickCounter,
                                        CallbackInfo ci) {
        AspectRatio ar = AspectRatio.INSTANCE;
        if (ar == null || !ar.isActiveNow() || !ar.shouldHideHud()) return;

        MinecraftClient mc = MinecraftClient.getInstance();
        double scale = mc.getWindow().getScaleFactor();
        int barH = (int) Math.ceil(ar.getBarHorizontal() / scale);
        int barV = (int) Math.ceil(ar.getBarVertical() / scale);

        int screenW = context.getScaledWindowWidth();
        int screenH = context.getScaledWindowHeight();

        context.enableScissor(barH, barV, screenW - barH, screenH - barV);
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void meoray$disableHudClip(DrawContext context, RenderTickCounter tickCounter,
                                         CallbackInfo ci) {
        AspectRatio ar = AspectRatio.INSTANCE;
        if (ar == null || !ar.isActiveNow() || !ar.shouldHideHud()) return;

        context.disableScissor();
    }
}
