package dev.meoray.client.util.render.renderers;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.joml.Matrix4f;

@Environment(EnvType.CLIENT)
public interface IRenderer {
    Matrix4f DEFAULT_MATRIX = new Matrix4f();

    default void render(double x, double y) {
        this.render((float)x, (float)y);
    }

    default void render(float x, float y) {
        this.render(DEFAULT_MATRIX, x, y);
    }

    default void render(Matrix4f matrix, double x, double y) {
        this.render(matrix, (float)x, (float)y);
    }

    default void render(Matrix4f matrix, float x, float y) {
        this.render(matrix, x, y, 0.0F);
    }

    default void render(double x, double y, double z) {
        this.render((float)x, (float)y, (float)z);
    }

    default void render(float x, float y, float z) {
        this.render(DEFAULT_MATRIX, x, y, z);
    }

    default void render(Matrix4f matrix, double x, double y, double z) {
        this.render(matrix, (float)x, (float)y, (float)z);
    }

    void render(Matrix4f var1, float var2, float var3, float var4);
}
