package dev.meoray.client.mixin;

import dev.meoray.client.account.accessor.IMinecraftClientMixin;
import dev.meoray.client.feature.render.FreeCam;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.session.Session;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Environment(EnvType.CLIENT)
@Mixin(MinecraftClient.class)
public abstract class MinecraftClientMixin implements IMinecraftClientMixin {

    @Mutable @Final @Shadow private Session session;

    @Override
    public void setSession(Session session) {
        this.session = session;
    }

    @Inject(method = "doAttack", at = @At("HEAD"), cancellable = true)
    private void meoray$blockAttack(CallbackInfoReturnable<Boolean> cir) {
        FreeCam fc = FreeCam.INSTANCE;
        if (fc != null && fc.shouldBlockInteractions()) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "doItemUse", at = @At("HEAD"), cancellable = true)
    private void meoray$blockUse(CallbackInfo ci) {
        FreeCam fc = FreeCam.INSTANCE;
        if (fc != null && fc.shouldBlockInteractions()) {
            ci.cancel();
        }
    }

    @Inject(method = "handleBlockBreaking", at = @At("HEAD"), cancellable = true)
    private void meoray$blockBreaking(boolean breaking, CallbackInfo ci) {
        FreeCam fc = FreeCam.INSTANCE;
        if (fc != null && fc.shouldBlockInteractions()) {
            ci.cancel();
        }
    }
}
