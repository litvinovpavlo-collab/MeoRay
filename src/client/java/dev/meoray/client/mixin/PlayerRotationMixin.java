package dev.meoray.client.mixin;

import dev.meoray.client.util.RotationUtil;
import net.minecraft.client.network.ClientPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayerEntity.class)
public class PlayerRotationMixin {

    @Inject(method = "sendMovementPackets", at = @At("HEAD"))
    private void onSendMovementPackets(CallbackInfo ci) {
        if (RotationUtil.isRotating()) {
            ClientPlayerEntity self = (ClientPlayerEntity) (Object) this;
            RotationUtil.setClientRotation(self.getYaw(), self.getPitch());
            self.setYaw(RotationUtil.getServerYaw());
            self.setPitch(RotationUtil.getServerPitch());
        }
    }

    @Inject(method = "sendMovementPackets", at = @At("TAIL"))
    private void afterSendMovementPackets(CallbackInfo ci) {
        if (RotationUtil.isRotating()) {
            ClientPlayerEntity self = (ClientPlayerEntity) (Object) this;
            self.setYaw(RotationUtil.getClientYaw());
            self.setPitch(RotationUtil.getClientPitch());
        }
    }
}
