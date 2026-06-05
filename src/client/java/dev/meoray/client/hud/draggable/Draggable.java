package dev.meoray.client.hud.draggable;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ChatScreen;

@Environment(EnvType.CLIENT)
public class Draggable {
    public float x;
    public float y;
    public float width;
    public float height;
    public float mouseX;
    public float mouseY;
    public int button;
    public float offsetX;
    public float offsetY;
    public float preX;
    private double mouseXStart;
    private double mouseYStart;
    private boolean dragging;
    private final String name;

    public Draggable(String name, float x, float y, float width, float height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.name = name;
        this.preX = x;
    }

    public boolean canPush() {
        return true;
    }

    public float getX() { return x; }
    public float getY() { return y; }
    public float getHeight() { return height; }
    public float getWidth() { return width; }

    public void setX(float v) { this.preX = this.x; this.x = v; }
    public void setY(float v) { this.y = v; }
    public void setWidth(float v) { this.width = v; }
    public void setHeight(float v) { this.height = v; }

    public float getMouseX() { return mouseX; }
    public float getMouseY() { return mouseY; }
    public void setMouseX(float v) { this.mouseX = v; }
    public void setMouseY(float v) { this.mouseY = v; }

    public boolean isDragging() { return dragging; }

    public void mouseClick() {
        if (isHovered(mouseXStart, mouseYStart)
                && MinecraftClient.getInstance().currentScreen instanceof ChatScreen
                && button == 0) {
            this.dragging = true;
        }
        this.offsetX = (float) (mouseXStart - x);
        this.offsetY = (float) (mouseYStart - y);
    }

    public void setButton(int b) { this.button = b; }

    public void endDrag() { this.dragging = false; }

    public void setMouseXStart(double v) { this.mouseXStart = v; }
    public void setMouseYStart(double v) { this.mouseYStart = v; }

    public float getOffsetX() { return offsetX; }
    public float getOffsetY() { return offsetY; }

    public String getName() { return name; }

    public boolean isHovered(double mx, double my) {
        float pad = 6f;
        return mx >= x - pad && mx <= x + width + pad && my >= y - pad && my <= y + height + pad;
    }
}
