package dev.meoray.client.mixin;

import dev.meoray.client.MeoRayClient;
import dev.meoray.client.feature.render.ESP;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin(EntityRenderer.class)
public abstract class EntityRendererMixin<T extends EntityRenderState> {

    @Unique
    private static ESP meoray$esp;

    @Unique
    private static ESP meoray$getEsp() {
        if (meoray$esp == null) meoray$esp = (ESP) MeoRayClient.INSTANCE.moduleManager.getByName("ESP");
        return meoray$esp;
    }

    @Inject(method = "renderLabelIfPresent", at = @At("HEAD"), cancellable = true)
    private void renderLabelIfPresent(T state, Text text, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, CallbackInfo ci) {
        ESP esp = meoray$getEsp();
        if (esp != null && esp.isEnabled() && esp.nametag.getValue() && meoray$isPlayer(text)) {
            ci.cancel();
        }
    }

    @Unique
    private boolean meoray$isPlayer(Text name) {
        var mc = MinecraftClient.getInstance();
        if (mc.player != null && mc.world != null) {
            for (PlayerEntity player : mc.world.getPlayers()) {
                if (player.getDisplayName() != null && player.getDisplayName().equals(name)) {
                    return true;
                }
            }
        }
        return false;
    }
}
