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
        for (HudElement el : elements) {
            Draggable d = el.getDraggable();
            if (d == null) continue;
            d.setMouseX(mouseX);
            d.setMouseY(mouseY);
            if (d.isDragging()) {
                float newX = mouseX - d.getOffsetX();
                float newY = mouseY - d.getOffsetY();
                // No strict bounds check; Draggable allows free movement within screen
                d.setX(newX);
                d.setY(newY);
            }
        }
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

    public void renderPanels(DrawContext context) {
        boolean editable = MinecraftClient.getInstance().currentScreen instanceof ChatScreen
                || MinecraftClient.getInstance().currentScreen instanceof MeoRayClickGUI;
        if (!editable) return;
        for (HudElement el : elements) {
            el.renderPanel(context);
        }
    }
}
