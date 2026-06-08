package dev.meoray.client.hud.draggable;

import dev.meoray.client.gui.screen.MeoRayClickGUI;
import dev.meoray.client.hud.HudElement;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;

import java.util.ArrayList;
import java.util.List;

@Environment(EnvType.CLIENT)
public class DraggableManager {
    private final List<HudElement> elements = new ArrayList<>();
    private int lastScreenW = -1;
    private int lastScreenH = -1;

    public void add(HudElement element) {
        elements.add(element);
    }

    public List<HudElement> getElements() {
        return elements;
    }

    public List<Draggable> getDraggables() {
        List<Draggable> result = new ArrayList<>();
        for (HudElement el : elements) {
            if (el.getDraggable() != null) result.add(el.getDraggable());
        }
        return result;
    }

    public void updatePositions(float mouseX, float mouseY) {
        MinecraftClient mc = MinecraftClient.getInstance();
        int screenW = mc.getWindow().getScaledWidth();
        int screenH = mc.getWindow().getScaledHeight();

        // Clamp all on resize
        if (screenW != lastScreenW || screenH != lastScreenH) {
            lastScreenW = screenW;
            lastScreenH = screenH;
            for (HudElement el : elements) {
                Draggable d = el.getDraggable();
                if (d == null) continue;
                clampElement(d, screenW, screenH);
            }
        }

        for (HudElement el : elements) {
            Draggable d = el.getDraggable();
            if (d == null) continue;
            d.setMouseX(mouseX);
            d.setMouseY(mouseY);
            if (d.isDragging()) {
                float newX = mouseX - d.getOffsetX();
                float newY = mouseY - d.getOffsetY();
                d.setX(newX);
                d.setY(newY);
                clampElement(d, screenW, screenH);
            }
        }
    }

    private void clampElement(Draggable d, int screenW, int screenH) {
        float w = Math.max(d.getWidth(), 10f);
        float h = Math.max(d.getHeight(), 10f);
        d.setX(Math.max(0, Math.min(screenW - w, d.getX())));
        d.setY(Math.max(0, Math.min(screenH - h, d.getY())));
    }

    public void handleClick(int button, double mouseX, double mouseY) {
        for (HudElement el : elements) {
            Draggable d = el.getDraggable();
            if (d == null) continue;
            if (!d.isDragging()) {
                d.setMouseXStart(mouseX);
                d.setMouseYStart(mouseY);
                d.setButton(button);
                d.mouseClick();
            }
        }
    }

    public void endDrag() {
        for (HudElement el : elements) {
            Draggable d = el.getDraggable();
            if (d != null) d.endDrag();
        }
    }

    public void onMouseClick(int button, double mx, double my) {
        for (HudElement el : elements) {
            Draggable d = el.getDraggable();
            if (d == null || d.isDragging()) continue;
            d.setMouseXStart(mx);
            d.setMouseYStart(my);
            d.setButton(button);
            d.mouseClick();
        }
    }

    public void onRelease() {
        for (HudElement el : elements) {
            Draggable d = el.getDraggable();
            if (d != null) d.endDrag();
        }
    }

    public void renderPanels(DrawContext context) {
        boolean editable = MinecraftClient.getInstance().currentScreen instanceof ChatScreen
                || MinecraftClient.getInstance().currentScreen instanceof MeoRayClickGUI;
        if (!editable) return;
        for (HudElement el : elements) {
            el.renderPanel(context);
        }
    }
}
