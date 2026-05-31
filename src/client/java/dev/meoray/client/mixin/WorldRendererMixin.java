package dev.meoray.client.mixin;

import dev.meoray.client.MeoRayClient;
import dev.meoray.client.feature.render.ESP;
import dev.meoray.client.util.render.esp.ChamsRenderer;
import dev.meoray.client.util.render.esp.EspMatrixHolder;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.util.ObjectAllocator;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin(WorldRenderer.class)
public class WorldRendererMixin {

    @Unique
    private static ESP meoray$esp;

    @Unique
    private static ESP meoray$getEsp() {
        if (meoray$esp == null) meoray$esp = (ESP) MeoRayClient.INSTANCE.moduleManager.getByName("ESP");
        return meoray$esp;
    }

    @Inject(method = "renderEntities", at = @At("TAIL"))
    private void afterRenderEntities(CallbackInfo ci) {
        ESP esp = meoray$getEsp();
        if (esp != null && esp.isEnabled() && esp.chamsMode.getValue().equals("New") && esp.chams.getValue()) {
            ChamsRenderer.renderAll();
        }
    }

    @Redirect(
        method = "renderEntities",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;getTeamColorValue()I")
    )
    private int redirectTeamColor(Entity entity) {
        ESP esp = meoray$getEsp();
        if (esp != null && esp.isEnabled() && esp.chams.getValue() && esp.chamsMode.getValue().equals("Glow") && entity instanceof PlayerEntity) {
            return MeoRayClient.INSTANCE.getThemeManager().getCurrentTheme().accent() & 0xFFFFFF;
        }
        return entity.getTeamColorValue();
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void onRenderTail(ObjectAllocator allocator, RenderTickCounter tickCounter, boolean renderBlockOutline, Camera camera, GameRenderer gameRenderer, Matrix4f positionMatrix, Matrix4f projectionMatrix, CallbackInfo ci) {
        Matrix4f pv = new Matrix4f(projectionMatrix).mul(positionMatrix);
        EspMatrixHolder.projView = pv;
        EspMatrixHolder.camera = camera;
    }
}
