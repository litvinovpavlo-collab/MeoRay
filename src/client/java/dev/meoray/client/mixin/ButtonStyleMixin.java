package dev.meoray.client.mixin;

import dev.meoray.client.util.render.builders.Builder;
import dev.meoray.client.util.render.builders.states.QuadColorState;
import dev.meoray.client.util.render.builders.states.QuadRadiusState;
import dev.meoray.client.util.render.builders.states.SizeState;
import dev.meoray.client.util.render.renderers.impl.BuiltBorder;
import dev.meoray.client.util.render.renderers.impl.BuiltRectangle;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.PressableWidget;
import net.minecraft.client.gui.widget.ClickableWidget;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.awt.Color;

@Mixin(PressableWidget.class)
public abstract class ButtonStyleMixin extends ClickableWidget {

    public ButtonStyleMixin(int x, int y, int width, int height, net.minecraft.text.Text message) {
        super(x, y, width, height, message);
    }

    @Inject(method = "renderWidget", at = @At("HEAD"), cancellable = true)
    private void nova$renderCustomButton(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (MinecraftClient.getInstance().world != null) return;

        int x = this.getX();
        int y = this.getY();
        int w = this.getWidth();
        int h = this.getHeight();
        boolean hover = this.isHovered();
        boolean active = this.active;

        Color bgColor, borderColor;
        int textColor;

        if (!active) {
            bgColor = new Color(0x88111122, true);
            borderColor = new Color(0xFF444466, true);
            textColor = 0xFF888888;
        } else if (hover) {
            bgColor = new Color(0xDD2A1A4A, true);
            borderColor = new Color(0xFFBB44FF, true);
            textColor = 0xFFFFFFFF;
        } else {
            bgColor = new Color(0xCC1A0F2A, true);
            borderColor = new Color(0xFF6622AA, true);
            textColor = 0xFFCCCCCC;
        }

        Matrix4f matrix = context.getMatrices().peek().getPositionMatrix();
        float radius = 6.0F;

        // ГЛАДКИЙ скруглённый фон через GLSL SDF шейдер
        BuiltRectangle rectangle = Builder.rectangle()
                .size(new SizeState((float) w, (float) h))
                .color(new QuadColorState(bgColor))
                .radius(new QuadRadiusState(radius))
                .smoothness(1.15F)
                .build();
        rectangle.render(matrix, (float) x, (float) y);

        // Гладкая рамка через шейдер
        BuiltBorder border = Builder.border()
                .size(new SizeState((float) w, (float) h))
                .color(new QuadColorState(borderColor))
                .radius(new QuadRadiusState(radius))
                .thickness(0.015F)
                .smoothness(0.65F, 0.65F)
                .build();
        border.render(matrix, (float) x, (float) y);

        // Hover индикатор слева
        if (hover && active) {
            BuiltRectangle hoverBar = Builder.rectangle()
                    .size(new SizeState(2.5F, (float)(h - 12)))
                    .color(new QuadColorState(new Color(0xFFBB44FF, true)))
                    .radius(new QuadRadiusState(1.5F))
                    .smoothness(1.15F)
                    .build();
            hoverBar.render(matrix, (float)(x + 3), (float)(y + 6));
        }

        // Текст
        context.drawCenteredTextWithShadow(
                MinecraftClient.getInstance().textRenderer,
                this.getMessage(),
                x + w / 2,
                y + (h - 8) / 2,
                textColor
        );

        ci.cancel();
    }
}
