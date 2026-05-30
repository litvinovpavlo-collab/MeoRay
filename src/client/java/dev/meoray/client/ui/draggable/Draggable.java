package dev.meoray.client.ui.draggable;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ChatScreen;

public class Draggable implements IDraggable {
    private int x, y, width, height;
    private int mouseX, mouseY;
    private int button;
    private int offsetX, offsetY;
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

    @Override public int getX() { return x; }
    @Override public int getY() { return y; }
    @Override public int getWidth() { return width; }
    @Override public int getHeight() { return height; }
    @Override public int getMouseX() { return mouseX; }
    @Override public int getMouseY() { return mouseY; }
    @Override public boolean isDragging() { return dragging; }
    public int getOffsetX() { return offsetX; }
    public int getOffsetY() { return offsetY; }

    @Override
    public void setX(int x) { this.x = x; }
    @Override
    public void setY(int y) { this.y = y; }
    @Override
    public void setWidth(int w) { this.width = w; }
    @Override
    public void setHeight(int h) { this.height = h; }
    @Override
    public void setMouseX(int x) { this.mouseX = x; }
    @Override
    public void setMouseY(int y) { this.mouseY = y; }
    @Override
    public void setButton(int button) { this.button = button; }

    @Override
    public void onMouseClick() {
        if (mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height
            && MinecraftClient.getInstance().currentScreen instanceof ChatScreen && button == 0) {
            dragging = true;
        }
        offsetX = (int) (mouseStartX - x);
        offsetY = (int) (mouseStartY - y);
    }

    @Override
    public void endDrag() { dragging = false; }

    public void setMouseStart(double mx, double my) {
        this.mouseStartX = mx;
        this.mouseStartY = my;
    }
}
