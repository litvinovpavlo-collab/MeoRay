package dev.meoray.client.mixin;

import dev.meoray.client.feature.render.FreeCam;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public abstract class ClientPlayerEntityMixin {

    @Inject(method = "changeLookDirection", at = @At("HEAD"), cancellable = true)
    private void meoray$mouseToFreeCam(double cursorDeltaX, double cursorDeltaY,
                                        CallbackInfo ci) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || (Entity) (Object) this != mc.player) return;

        FreeCam fc = FreeCam.INSTANCE;
        if (fc == null || !fc.isEnabled()) return;

        fc.onMouseDelta(cursorDeltaX, cursorDeltaY);
        ci.cancel();
    }
}
