package dev.meoray.client.ui.draggable;

import dev.meoray.client.gui.screen.MeoRayClickGUI;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ChatScreen;

public class Draggable implements IDraggable {
    private float x, y, width, height;
    private float mouseX, mouseY;
    private int button;
    private float offsetX, offsetY;
    private double mouseStartX, mouseStartY;
    private boolean dragging;
    private final String name;

    public Draggable(String name, int x, int y, int width, int height) {
        this.name = name;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public String getName() { return name; }

    @Override public int getX() { return (int) x; }
    @Override public int getY() { return (int) y; }
    @Override public int getWidth() { return (int) width; }
    @Override public int getHeight() { return (int) height; }
    @Override public int getMouseX() { return (int) mouseX; }
    @Override public int getMouseY() { return (int) mouseY; }
    @Override public boolean isDragging() { return dragging; }
    public float getOffsetX() { return offsetX; }
    public float getOffsetY() { return offsetY; }

    public float getXf() { return x; }
    public float getYf() { return y; }
    public float getWidthf() { return width; }
    public float getHeightf() { return height; }

    @Override public void setX(int x) { this.x = x; }
    @Override public void setY(int y) { this.y = y; }
    @Override public void setWidth(int w) { this.width = w; }
    @Override public void setHeight(int h) { this.height = h; }
    @Override public void setMouseX(int x) { this.mouseX = x; }
    @Override public void setMouseY(int y) { this.mouseY = y; }
    @Override public void setButton(int button) { this.button = button; }

    public void setX(float x) { this.x = x; }
    public void setY(float y) { this.y = y; }
    public void setWidth(float w) { this.width = w; }
    public void setHeight(float h) { this.height = h; }
    public void setMouseX(float x) { this.mouseX = x; }
    public void setMouseY(float y) { this.mouseY = y; }

    @Override
    public void onMouseClick() {
        var screen = MinecraftClient.getInstance().currentScreen;
        boolean editable = screen instanceof ChatScreen || screen instanceof MeoRayClickGUI;

        if (mouseX >= x && mouseX <= x + width
                && mouseY >= y && mouseY <= y + height
                && editable && button == 0) {
            dragging = true;
            offsetX = (float) (mouseStartX - x);
            offsetY = (float) (mouseStartY - y);
        }
    }

    @Override
    public void endDrag() { dragging = false; }

    public void setMouseStart(double mx, double my) {
        this.mouseStartX = mx;
        this.mouseStartY = my;
    }
}
