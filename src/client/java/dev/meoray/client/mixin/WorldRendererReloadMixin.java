package dev.meoray.client.mixin;

import dev.meoray.client.util.render.SkyShaderHolder;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.resource.ResourceManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WorldRenderer.class)
public class WorldRendererReloadMixin {

    @Inject(method = "reload", at = @At("TAIL"))
    private void onReload(ResourceManager manager, CallbackInfo ci) {
        SkyShaderHolder.reload(manager);
    }
}
