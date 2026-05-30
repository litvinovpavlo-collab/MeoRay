package dev.meoray.client.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.world.LevelLoadingScreen;
import net.minecraft.client.gui.screen.ProgressScreen;
import net.minecraft.client.gui.screen.MessageScreen;
import net.minecraft.client.gui.screen.DownloadingTerrainScreen;
import dev.meoray.client.gui.common.SpaceBackground;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

public class LoadingScreensMixin {

    @Mixin(LevelLoadingScreen.class)
    public static abstract class LevelLoading {
        @Inject(method = "render", at = @At("HEAD"))
        private void nova$drawBg(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
            LevelLoadingScreen self = (LevelLoadingScreen)(Object)this;
            SpaceBackground.render(context, self.width, self.height);
        }
    }

    @Mixin(ProgressScreen.class)
    public static abstract class Progress {
        @Inject(method = "render", at = @At("HEAD"))
        private void nova$drawBg(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
            ProgressScreen self = (ProgressScreen)(Object)this;
            SpaceBackground.render(context, self.width, self.height);
        }
    }

    @Mixin(MessageScreen.class)
    public static abstract class Message {
        @Inject(method = "render", at = @At("HEAD"))
        private void nova$drawBg(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
            MessageScreen self = (MessageScreen)(Object)this;
            if (MinecraftClient.getInstance().world == null) {
                SpaceBackground.render(context, self.width, self.height);
            }
        }
    }

    @Mixin(DownloadingTerrainScreen.class)
    public static abstract class DownloadingTerrain {
        @Inject(method = "render", at = @At("HEAD"))
        private void nova$drawBg(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
            DownloadingTerrainScreen self = (DownloadingTerrainScreen)(Object)this;
            SpaceBackground.render(context, self.width, self.height);
        }
    }
}
