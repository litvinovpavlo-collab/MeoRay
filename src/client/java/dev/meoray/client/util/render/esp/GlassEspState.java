package dev.meoray.client.util.render.esp;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public class GlassEspState {
    public static int capturedTexture = 0;
    public static int capturedWidth = 0;
    public static int capturedHeight = 0;
}
