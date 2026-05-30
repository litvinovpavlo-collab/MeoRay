package dev.meoray.client.ui.draggable;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

import java.util.ArrayList;
import java.util.List;

public class DraggableManager {
    private final List<Draggable> draggables = new ArrayList<>();

    public void add(Draggable d) { draggables.add(d); }

    public void tick(int mouseX, int mouseY, DrawContext ctx) {
        for (Draggable d : draggables) {
            d.setMouseX(mouseX);
            d.setMouseY(mouseY);
            if (d.isDragging()) {
                int w = MinecraftClient.getInstance().getWindow().getScaledWidth();
                int h = MinecraftClient.getInstance().getWindow().getScaledHeight();
                int nx = mouseX - d.getOffsetX();
                int ny = mouseY - d.getOffsetY();
                if (nx > 0 && ny > 0 && nx + d.getWidth() < w && ny + d.getHeight() < h) {
                    d.setX(nx);
                    d.setY(ny);
                }
            }
        }
    }

    public void onMouseClick(int button, double mx, double my) {
        for (Draggable d : draggables) {
            if (!d.isDragging()) {
                d.setMouseStart(mx, my);
                d.setButton(button);
                d.onMouseClick();
            }
        }
    }

    public void onRelease() {
        for (Draggable d : draggables) d.endDrag();
    }

    public List<Draggable> getDraggables() { return draggables; }
}
