package dev.meoray.client.mixin;

import dev.meoray.client.MeoRayClient;
import dev.meoray.client.feature.misc.Interface;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.util.DefaultSkinHelper;
import net.minecraft.client.util.SkinTextures;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractClientPlayerEntity.class)
public abstract class AbstractClientPlayerEntityMixin {

    private static final Identifier MEORAY_CAPE = Identifier.of("meoray", "textures/meoray_cape.png");

    @Inject(method = "getSkinTextures", at = @At("RETURN"), cancellable = true)
    private void meoray$overrideCape(CallbackInfoReturnable<SkinTextures> cir) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.player == null) return;

        AbstractClientPlayerEntity self = (AbstractClientPlayerEntity) (Object) this;
        if (self != mc.player) return;
        if (!isCustomCapeEnabled()) return;

        SkinTextures original = cir.getReturnValue();
        SkinTextures fallback = DefaultSkinHelper.getSkinTextures(self.getUuid());

        Identifier skinTexture = original != null ? original.texture() : fallback.texture();
        String skinUrl = original != null ? original.textureUrl() : fallback.textureUrl();
        SkinTextures.Model model = original != null ? original.model() : fallback.model();
        boolean secure = original != null ? original.secure() : fallback.secure();

        SkinTextures overridden = new SkinTextures(
                skinTexture,
                skinUrl,
                MEORAY_CAPE,
                MEORAY_CAPE,
                model,
                secure
        );

        cir.setReturnValue(overridden);
    }

    private boolean isCustomCapeEnabled() {
        try {
            Interface module = MeoRayClient.INSTANCE.moduleManager.getModule(Interface.class);
            return module != null && module.isEnabled() && module.customCape.getValue();
        } catch (Throwable ignored) {
            return false;
        }
    }
}
