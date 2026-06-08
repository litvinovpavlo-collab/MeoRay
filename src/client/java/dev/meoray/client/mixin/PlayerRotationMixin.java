package dev.meoray.client.mixin;

import dev.meoray.client.MeoRayClient;
import dev.meoray.client.rotation.RotationComponent;
import net.minecraft.client.network.ClientPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayerEntity.class)
public class PlayerRotationMixin {
    @Unique
    private float savedYaw;
    @Unique
    private float savedPitch;

    @Inject(method = "sendMovementPackets", at = @At("HEAD"))
    private void onSendMovementPacketsHead(CallbackInfo ci) {
        MeoRayClient.INSTANCE.moduleManager.eventRotate();

        ClientPlayerEntity player = (ClientPlayerEntity)(Object)this;
        savedYaw = player.getYaw();
        savedPitch = player.getPitch();
        if (RotationComponent.isAiming()) {
            player.setYaw(RotationComponent.getSilentYaw());
            player.setPitch(RotationComponent.getSilentPitch());
            float sy = RotationComponent.getSilentYaw();
            player.prevHeadYaw = sy;
            player.headYaw = sy;
            player.prevBodyYaw = sy;
            player.bodyYaw = sy;
        }
    }

    @Inject(method = "sendMovementPackets", at = @At("TAIL"))
    private void onSendMovementPacketsTail(CallbackInfo ci) {
        ClientPlayerEntity player = (ClientPlayerEntity)(Object)this;
        player.setYaw(savedYaw);
        player.setPitch(savedPitch);
    }
}
