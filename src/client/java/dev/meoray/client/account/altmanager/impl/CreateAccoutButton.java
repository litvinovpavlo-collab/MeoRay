package dev.meoray.client.account.altmanager.impl;

import dev.meoray.client.managers.FontManager;
import dev.meoray.client.util.render.builders.Builder;
import dev.meoray.client.util.render.builders.states.QuadColorState;
import dev.meoray.client.util.render.builders.states.QuadRadiusState;
import dev.meoray.client.util.render.builders.states.SizeState;
import dev.meoray.client.util.render.msdf.MsdfFont;
import dev.meoray.client.util.render.renderers.impl.BuiltRectangle;
import dev.meoray.client.util.render.renderers.impl.BuiltText;
import java.awt.Color;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.joml.Matrix4f;

@Environment(EnvType.CLIENT)
public class CreateAccoutButton {
    private double x;
    private double y;
    private double width = 124.0;
    private double height = 20.0;
    private Runnable run;
    private String name;

    public CreateAccoutButton(String name, Runnable run) {
        this.run = run;
        this.name = name;
    }

    public void onClick() {
        this.run.run();
    }

    public void render(Matrix4f matrix4f) {
        ((BuiltRectangle)Builder.rectangle()
            .size(new SizeState(this.width, this.height))
            .color(new QuadColorState(new Color(-12536065, true), new Color(-13466369, true), new Color(-12536065, true), new Color(-13466369, true)))
            .radius(new QuadRadiusState(5.0F)).smoothness(1.15F).build())
            .render(matrix4f, (float)this.x, (float)this.y);
        double centerX = this.x + this.width / 2.0 - ((MsdfFont)FontManager.SUISSEINTMEDIUM.get()).getWidth("Create", 8.0F) / 2.0;
        BuiltText text = (BuiltText)Builder.text().font(FontManager.SUISSEINTMEDIUM.get()).text("Create")
            .color(Color.WHITE).size(8.0F).thickness(0.05F).build();
        text.render(matrix4f, (float)(centerX + 5.0), (float)(this.y + 5.0));
        text = (BuiltText)Builder.text().font(FontManager.MAINMENU.get()).text("A")
            .color(Color.WHITE).size(8.0F).thickness(0.05F).build();
        text.render(matrix4f, (float)(centerX - 5.0), (float)(this.y + 6.0));
    }

    public void updatePos(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public double getWidth() { return this.width; }
    public double getHeight() { return this.height; }
    public String getName() { return this.name; }
    public double getX() { return this.x; }
    public double getY() { return this.y; }
}
