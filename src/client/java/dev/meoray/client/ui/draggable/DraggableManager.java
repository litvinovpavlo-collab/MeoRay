package dev.meoray.client.ui.draggable;

import dev.meoray.client.gui.screen.MeoRayClickGUI;
import dev.meoray.client.hud.HudElement;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;

import java.util.ArrayList;
import java.util.List;

public class DraggableManager {
    private final List<Draggable> draggables = new ArrayList<>();
    private final List<HudElement> elements = new ArrayList<>();

    public void add(Draggable d) { draggables.add(d); }

    public void add(HudElement el) {
        elements.add(el);
    }

    public void updatePositions(float mx, float my) {
        for (Draggable d : draggables) {
            d.setMouseX(mx);
            d.setMouseY(my);

            if (d.isDragging()) {
                float nx = mx - d.getOffsetX();
                float ny = my - d.getOffsetY();

                int w = MinecraftClient.getInstance().getWindow().getScaledWidth();
                int h = MinecraftClient.getInstance().getWindow().getScaledHeight();
                float ms = dev.meoray.client.MeoRayClient.mainScale;
                float maxX = (w / ms) - d.getWidthf();
                float maxY = (h / ms) - d.getHeightf();

                if (nx < 0) nx = 0;
                if (ny < 0) ny = 0;
                if (nx > maxX) nx = maxX;
                if (ny > maxY) ny = maxY;

                d.setX(nx);
                d.setY(ny);
            }
        }
        for (HudElement el : elements) {
            dev.meoray.client.hud.draggable.Draggable d = el.getDraggable();
            if (d == null) continue;
            d.setMouseX(mx);
            d.setMouseY(my);
            if (d.isDragging()) {
                float nx = mx - (float) d.getOffsetX();
                float ny = my - (float) d.getOffsetY();
                int w = MinecraftClient.getInstance().getWindow().getScaledWidth();
                int h = MinecraftClient.getInstance().getWindow().getScaledHeight();
                float ms = dev.meoray.client.MeoRayClient.mainScale;
                float maxX = (w / ms) - d.getWidth();
                float maxY = (h / ms) - d.getHeight();
                if (nx < 0) nx = 0;
                if (ny < 0) ny = 0;
                if (nx > maxX) nx = maxX;
                if (ny > maxY) ny = maxY;
                d.setX(nx);
                d.setY(ny);
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
        for (HudElement el : elements) {
            dev.meoray.client.hud.draggable.Draggable d = el.getDraggable();
            if (d == null || d.isDragging()) continue;
            d.setMouseXStart(mx);
            d.setMouseYStart(my);
            d.setButton(button);
            d.mouseClick();
        }
    }

    public void onRelease() {
        for (Draggable d : draggables) d.endDrag();
        for (HudElement el : elements) {
            dev.meoray.client.hud.draggable.Draggable d = el.getDraggable();
            if (d != null) d.endDrag();
        }
    }

    public void renderPanels(DrawContext ctx) {
        var screen = MinecraftClient.getInstance().currentScreen;
        boolean editable = screen instanceof ChatScreen || screen instanceof MeoRayClickGUI;
        if (!editable) return;
        for (HudElement el : elements) el.renderPanel(ctx);
    }

    public List<Draggable> getDraggables() { return draggables; }
}
