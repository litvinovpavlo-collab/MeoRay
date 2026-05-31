package dev.meoray.client.util.render.esp;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.Camera;
import org.joml.Matrix4f;

@Environment(EnvType.CLIENT)
public class EspMatrixHolder {
    public static Matrix4f projView = new Matrix4f();
    public static Camera camera = null;
}
