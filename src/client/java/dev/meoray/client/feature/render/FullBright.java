package dev.meoray.client.feature.render;
import dev.meoray.client.core.Category;
import dev.meoray.client.core.Module;
import net.minecraft.client.MinecraftClient;

public class FullBright extends Module {
    private double oldGamma;

    public FullBright() {
        super("FullBright", "Brighten the world", Category.RENDER);
    }

    @Override
    public void onEnable() {
        oldGamma = MinecraftClient.getInstance().options.getGamma().getValue();
        MinecraftClient.getInstance().options.getGamma().setValue(100.0);
    }

    @Override
    public void onDisable() {
        MinecraftClient.getInstance().options.getGamma().setValue(oldGamma);
    }
}
