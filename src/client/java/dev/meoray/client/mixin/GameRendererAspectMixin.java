package dev.meoray.client.mixin;

import dev.meoray.client.feature.render.AspectRatio;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.RenderTickCounter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public abstract class GameRendererAspectMixin {

    private long meoray$lastFrameTime = System.nanoTime();

    @Inject(method = "render", at = @At("HEAD"))
    private void meoray$updateAspectRatio(RenderTickCounter tickCounter, boolean tick,
                                            CallbackInfo ci) {
        AspectRatio ar = AspectRatio.INSTANCE;
        if (ar == null || !ar.isEnabled()) return;

        long now = System.nanoTime();
        float deltaTime = (now - meoray$lastFrameTime) / 1_000_000_000f;
        meoray$lastFrameTime = now;

        if (deltaTime > 0.1f) deltaTime = 0.1f;

        ar.update(deltaTime);
    }
}
