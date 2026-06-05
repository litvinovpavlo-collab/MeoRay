package dev.meoray.client.hud;

import dev.meoray.client.hud.draggable.Draggable;
import dev.meoray.client.util.render.GlowRenderer;
import dev.meoray.client.util.render.builders.Builder;
import dev.meoray.client.util.render.builders.states.QuadColorState;
import dev.meoray.client.util.render.builders.states.QuadRadiusState;
import dev.meoray.client.util.render.builders.states.SizeState;
import dev.meoray.client.util.render.renderers.impl.BuiltRectangle;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.DrawContext;
import org.joml.Matrix4f;

@Environment(EnvType.CLIENT)
public abstract class HudElement {
    protected final Draggable draggable;
    protected boolean visiblePane;
    private final String name;

    public HudElement(Draggable draggable, String name) {
        this.draggable = draggable;
        this.name = name;
    }

    public abstract void render(DrawContext context);

    public Draggable getDraggable() {
        return draggable;
    }

    public String getName() {
        return name;
    }

    public void setVisiblePane(boolean v) {
        this.visiblePane = v;
    }

    public void renderPanel(DrawContext context) {
        if (draggable == null) return;
        Matrix4f matrix = context.getMatrices().peek().getPositionMatrix();

        float px = draggable.getX();
        float py = draggable.getY();
        float pw = draggable.getWidth();
        float ph = draggable.getHeight();

        int outline = 0x80FFFFFF;
        ((BuiltRectangle) Builder.rectangle()
            .size(new SizeState(pw, ph))
            .color(new QuadColorState(0x10FFFFFF))
            .radius(new QuadRadiusState(4.0))
            .smoothness(1.15F)
            .build()).render(matrix, px, py);

        int dotSpacing = 5;
        int dotSize = 2;
        int dotColor = 0x99FFFFFF;
        float perimeter = 2 * (pw + ph);
        int dots = (int) (perimeter / dotSpacing);
        for (int i = 0; i < dots; i++) {
            float dist = i * dotSpacing;
            float dx, dy;
            if (dist < pw) { dx = px + dist; dy = py; }
            else if (dist < pw + ph) { dx = px + pw; dy = py + (dist - pw); }
            else if (dist < pw * 2 + ph) { dx = px + pw - (dist - pw - ph); dy = py + ph; }
            else { dx = px; dy = py + ph - (dist - pw * 2 - ph); }
            ((BuiltRectangle) Builder.rectangle()
                .size(new SizeState(dotSize, dotSize))
                .color(new QuadColorState(dotColor))
                .radius(new QuadRadiusState(1.0))
                .smoothness(1.15F)
                .build()).render(matrix, dx, dy);
        }
    }
}
