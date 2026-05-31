package dev.meoray.client.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import dev.meoray.client.MeoRayClient;
import dev.meoray.client.feature.render.ESP;
import dev.meoray.client.util.render.esp.ChamsRenderer;
import dev.meoray.client.util.render.esp.FlatEspLayer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Environment(EnvType.CLIENT)
@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin {

    @Unique
    private static ESP meoray$esp;

    @Unique
    private static ESP meoray$getEsp() {
        if (meoray$esp == null) meoray$esp = (ESP) MeoRayClient.INSTANCE.moduleManager.getByName("ESP");
        return meoray$esp;
    }

    @Redirect(
        method = "render(Lnet/minecraft/client/render/entity/state/LivingEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/entity/model/EntityModel;render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumer;III)V")
    )
    private void redirectModelRender(EntityModel<?> model, MatrixStack matrixStack, VertexConsumer vertexConsumer, int light, int overlay, int color,
                                     @Local(argsOnly = true) LivingEntityRenderState renderState,
                                     @Local(argsOnly = true) VertexConsumerProvider vertexConsumers) {
        ESP esp = meoray$getEsp();
        if (esp == null || !esp.isEnabled() || !esp.chams.getValue()) {
            model.render(matrixStack, vertexConsumer, light, overlay, color);
            return;
        }

        boolean isPlayer = renderState instanceof PlayerEntityRenderState;
        String mode = esp.chamsMode.getValue();

        if (isPlayer && mode.equals("Glass")) {
        } else {
            model.render(matrixStack, vertexConsumer, light, overlay, color);
        }

        if (isPlayer) {
            int themeColor = MeoRayClient.INSTANCE.getThemeManager().getCurrentTheme().accent() & 0xFFFFFF;

            if (mode.equals("Glass")) {
                VertexConsumer depthVc = vertexConsumers.getBuffer(FlatEspLayer.FLAT_ESP_DEPTH);
                model.render(matrixStack, depthVc, light, overlay, -1);

                VertexConsumer colorVc = vertexConsumers.getBuffer(FlatEspLayer.FLAT_ESP);
                int glassColor = (int)(0.4 * 255) << 24 | themeColor;
                model.render(matrixStack, colorVc, 15728880, overlay, glassColor);
            }

            if (mode.equals("Flat")) {
                VertexConsumer espVc = vertexConsumers.getBuffer(FlatEspLayer.FLAT_ESP);
                model.render(matrixStack, espVc, 15728880, overlay, themeColor);
            }

            if (mode.equals("New")) {
                int[] chams = meoray$adapt(themeColor);
                ChamsRenderer.enqueue(model, matrixStack, chams);
            }
        }
    }

    @Unique
    private int[] meoray$adapt(int rgb) {
        int r = rgb >> 16 & 255;
        int g = rgb >> 8 & 255;
        int b = rgb & 255;
        float brightness = (r * 0.299F + g * 0.587F + b * 0.114F) / 255.0F;
        int overAlpha = Math.max(80, (int)(224.0F * (1.0F - brightness * 0.75F)));
        int throughAlpha = Math.max(37, (int)(112.0F * (1.0F - brightness * 0.65F)));
        return new int[]{overAlpha << 24 | r << 16 | g << 8 | b, throughAlpha << 24 | r << 16 | g << 8 | b};
    }
}
