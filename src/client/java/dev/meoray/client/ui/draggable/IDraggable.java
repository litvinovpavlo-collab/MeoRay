package dev.meoray.client.ui.draggable;

public interface IDraggable {
    int getX();
    int getY();
    int getWidth();
    int getHeight();
    int getMouseX();
    int getMouseY();
    void setX(int x);
    void setY(int y);
    void setWidth(int w);
    void setHeight(int h);
    void setMouseX(int x);
    void setMouseY(int y);
    boolean isDragging();
    void onMouseClick();
    void setButton(int button);
    void endDrag();
}
