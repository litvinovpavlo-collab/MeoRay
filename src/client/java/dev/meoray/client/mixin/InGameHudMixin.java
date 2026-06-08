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

@Mixin(InGameHud.class)
public abstract class InGameHudMixin {

    @Inject(method = "render", at = @At("TAIL"))
    private void meoray$drawAspectRatioBars(DrawContext context, RenderTickCounter tickCounter,
                                              CallbackInfo ci) {
        AspectRatio ar = AspectRatio.INSTANCE;
        if (ar == null || !ar.isActiveNow()) return;

        MinecraftClient mc = MinecraftClient.getInstance();
        int screenW = context.getScaledWindowWidth();
        int screenH = context.getScaledWindowHeight();

        double scale = mc.getWindow().getScaleFactor();
        int barH = (int) Math.ceil(ar.getBarHorizontal() / scale);
        int barV = (int) Math.ceil(ar.getBarVertical() / scale);

        int color = ar.getBarColorARGB();

        if (barV > 0) {
            context.fill(0, 0, screenW, barV, color);
            context.fill(0, screenH - barV, screenW, screenH, color);
        }

        if (barH > 0) {
            context.fill(0, 0, barH, screenH, color);
            context.fill(screenW - barH, 0, screenW, screenH, color);
        }
    }
}
