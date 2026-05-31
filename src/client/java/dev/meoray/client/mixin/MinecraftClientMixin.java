package dev.meoray.client.mixin;

import dev.meoray.client.MeoRayClient;
import dev.meoray.client.feature.render.ESP;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Environment(EnvType.CLIENT)
@Mixin(MinecraftClient.class)
public class MinecraftClientMixin {

    @Unique
    private static ESP meoray$esp;

    @Unique
    private static ESP meoray$getEsp() {
        if (meoray$esp == null) meoray$esp = (ESP) MeoRayClient.INSTANCE.moduleManager.getByName("ESP");
        return meoray$esp;
    }

    @Inject(method = "hasOutline", at = @At("HEAD"), cancellable = true)
    private void onHasOutline(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        ESP esp = meoray$getEsp();
        if (esp != null && esp.isEnabled() && esp.chams.getValue() && esp.chamsMode.getValue().equals("Glow") && entity instanceof PlayerEntity) {
            cir.setReturnValue(true);
        }
    }
}
