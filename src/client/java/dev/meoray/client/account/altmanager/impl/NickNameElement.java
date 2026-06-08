package dev.meoray.client.account.altmanager.impl;

import dev.meoray.client.MeoRayClient;
import dev.meoray.client.account.altmanager.NickName;
import dev.meoray.client.managers.FontManager;
import dev.meoray.client.account.accessor.IMinecraftClientMixin;
import dev.meoray.client.util.animations.Direction;
import dev.meoray.client.util.math.MathUtil;
import dev.meoray.client.util.render.builders.Builder;
import dev.meoray.client.util.render.builders.states.QuadColorState;
import dev.meoray.client.util.render.builders.states.QuadRadiusState;
import dev.meoray.client.util.render.builders.states.SizeState;
import dev.meoray.client.util.render.msdf.MsdfFont;
import dev.meoray.client.util.render.renderers.impl.BuiltBorder;
import dev.meoray.client.util.render.renderers.impl.BuiltRectangle;
import dev.meoray.client.util.render.renderers.impl.BuiltText;
import dev.meoray.client.util.render.renderers.impl.BuiltTexture;
import java.awt.Color;
import java.util.UUID;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.session.Session;
import net.minecraft.client.session.Session.AccountType;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.util.Identifier;
import org.joml.Matrix4f;

@Environment(EnvType.CLIENT)
public class NickNameElement {
    private NickName nickName;

    public void button(double x, double y, double width, double height, double mouseX, double mouseY,
                       double xStart, double yStart, double xEnd, double yEnd,
                       DrawContext drawContext, int click) {
        Matrix4f matrix = drawContext.getMatrices().peek().getPositionMatrix();
        ((BuiltRectangle)Builder.rectangle().size(new SizeState(width, height))
            .color(new QuadColorState(new Color(-14737108, true)))
            .radius(new QuadRadiusState(4.0F)).smoothness(1.15F).build())
            .render(matrix, (float)x, (float)y);
        ((BuiltBorder)Builder.border().size(new SizeState(width, height))
            .color(new QuadColorState(new Color(2434866)))
            .radius(new QuadRadiusState(4.0F)).thickness(0.01F)
            .smoothness(0.7F, 0.7F).build())
            .render(matrix, (float)x, (float)y);

        double valAnimation = this.nickName.getAnimation().getOutput();
        if (!this.nickName.getAnimation().finished(Direction.BACKWARDS)) {
            ((BuiltRectangle)Builder.rectangle().size(new SizeState(width, height))
                .color(new QuadColorState(new Color(-14342350, true)))
                .radius(new QuadRadiusState(4.0F)).smoothness(1.15F).build())
                .render(matrix, (float)x, (float)y);
            ((BuiltBorder)Builder.border().size(new SizeState(width, height))
                .color(new QuadColorState(new Color(3092796)))
                .radius(new QuadRadiusState(4.0F)).thickness(0.01F)
                .smoothness(0.7F, 0.7F).build())
                .render(matrix, (float)x, (float)y);
        }

        AbstractTexture abstractTexture = MinecraftClient.getInstance().getTextureManager()
            .getTexture(Identifier.ofVanilla("textures/entity/player/wide/steve.png"));
        BuiltTexture texture = (BuiltTexture)Builder.texture()
            .size(new SizeState(height - 10.0, height - 10.0))
            .radius(new QuadRadiusState(2.0F))
            .texture(0.125F, 0.125F, 0.125F, 0.125F, abstractTexture)
            .color(new QuadColorState(Color.WHITE)).build();
        texture.render(matrix, (float)(x + 5.0), (float)(y + 5.0));

        BuiltText text = (BuiltText)Builder.text().font(FontManager.SUISSEINTMEDIUM.get())
            .text(this.nickName.getNickname()).color(new Color(12961746))
            .size(8.0F).thickness(0.05F).build();
        text.render(matrix, (float)(x + (height - 10.0) + 10.0), (float)(y + 8.0));
        text = (BuiltText)Builder.text().font(FontManager.SUISSEINTMEDIUM.get())
            .text(this.nickName.getTag()).color(new Color(7040376))
            .size(7.0F).thickness(0.05F).build();
        text.render(matrix, (float)(x + (height - 10.0) + 10.0), (float)(y + 19.0));

        double deleteButtonWidth = height - 15.0;
        if (MathUtil.isHovered((int)x, (int)y, (int)(width - deleteButtonWidth), (int)height, (int)mouseX, (int)mouseY) && click == 0) {
            IMinecraftClientMixin instance = (IMinecraftClientMixin)MinecraftClient.getInstance();
            Session session = MinecraftClient.getInstance().getSession();
            instance.setSession(new Session(this.nickName.getNickname(), UUID.randomUUID(),
                session.getAccessToken(), session.getXuid(), session.getClientId(), AccountType.LEGACY));
            MeoRayClient.INSTANCE.getConfigManager().saveNickNames();
        }

        this.deleteButton(x + width - (deleteButtonWidth + 6.0), y + 8.0,
            deleteButtonWidth, deleteButtonWidth, mouseX, mouseY, matrix, click);

        if (!this.nickName.getAnimation().finished(Direction.BACKWARDS)) {
            text = (BuiltText)Builder.text().font(FontManager.SUISSEINTMEDIUM.get())
                .text(this.nickName.getNickname()).color(Color.WHITE)
                .size(8.0F).thickness(0.05F).build();
            text.render(matrix, (float)(x + (height - 10.0) + 10.0), (float)(y + 8.0));
            text = (BuiltText)Builder.text().font(FontManager.SUISSEINTMEDIUM.get())
                .text(this.nickName.getTag()).color(new Color(12961746))
                .size(7.0F).thickness(0.05F).build();
            text.render(matrix, (float)(x + (height - 10.0) + 10.0), (float)(y + 19.0));
        }

        if (MinecraftClient.getInstance().getSession().getUsername().equals(this.nickName.getNickname())) {
            this.nickName.getAnimation().setDirection(Direction.FORWARDS);
        } else {
            this.nickName.getAnimation().setDirection(Direction.BACKWARDS);
        }
    }

    private void deleteButton(double x, double y, double width, double height,
                              double mouseX, double mouseY, Matrix4f matrix, int click) {
        ((BuiltRectangle)Builder.rectangle().size(new SizeState(width, height))
            .color(new QuadColorState(new Color(-14342350, true)))
            .radius(new QuadRadiusState(3.0F)).smoothness(1.15F).build())
            .render(matrix, (float)x, (float)y);
        ((BuiltBorder)Builder.border().size(new SizeState(width, height))
            .color(new QuadColorState(new Color(3092796)))
            .radius(new QuadRadiusState(3.0F)).thickness(0.01F)
            .smoothness(0.7F, 0.7F).build())
            .render(matrix, (float)x, (float)y);
        BuiltText text = (BuiltText)Builder.text().font(FontManager.MAINMENU.get()).text("D")
            .color(new Color(7040376)).size(8.0F).thickness(0.05F).build();
        text.render(matrix, (float)(x + width / 2.0 - FontManager.MAINMENU.get().getWidth("D", 8.0F) / 2.0 + 0.5), (float)(y + 6.0));

        double valAnimation = this.nickName.getAnimation().getOutput();
        if (!this.nickName.getAnimation().finished(Direction.BACKWARDS)) {
            Color color1 = new Color((int)(valAnimation * 255) << 24 | 0x00FF3F3F, true);
            Color color2 = new Color((int)(valAnimation * 255) << 24 | 0x00332BFF, true);
            ((BuiltRectangle)Builder.rectangle().size(new SizeState(width + 2.0, height + 2.0))
                .color(new QuadColorState(color1, color2, color1, color2))
                .radius(new QuadRadiusState(5.0F)).smoothness(1.15F).build())
                .render(matrix, (float)(x - 1.0), (float)(y - 1.0));
            text = (BuiltText)Builder.text().font(FontManager.MAINMENU.get()).text("D")
                .color(Color.WHITE).size(8.0F).thickness(0.05F).build();
            text.render(matrix, (float)(x + width / 2.0 - FontManager.MAINMENU.get().getWidth("D", 8.0F) / 2.0 + 0.5), (float)(y + 6.0));
        }

        if (MathUtil.isHovered((int)x, (int)y, (int)width, (int)height, (int)mouseX, (int)mouseY) && click == 0) {
            MeoRayClient.INSTANCE.getNickNameManager().remove(this.nickName);
        }
    }

    public void setNickName(NickName nickName) { this.nickName = nickName; }
}
