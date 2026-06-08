package dev.meoray.client.account.altmanager;

import dev.meoray.client.MeoRayClient;
import dev.meoray.client.account.altmanager.impl.CreateAccoutButton;
import dev.meoray.client.account.altmanager.impl.GenerateAccoutButton;
import dev.meoray.client.account.altmanager.impl.NickNameElement;
import dev.meoray.client.account.altmanager.impl.StringElement;
import dev.meoray.client.managers.FontManager;
import dev.meoray.client.account.accessor.IMinecraftClientMixin;
import dev.meoray.client.util.render.AnimationUtil;
import dev.meoray.client.util.animations.Animation;
import dev.meoray.client.util.animations.Direction;
import dev.meoray.client.util.animations.EaseBackIn;
import dev.meoray.client.util.math.MathUtil;
import dev.meoray.client.util.render.Scissor;
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
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.session.Session;
import net.minecraft.client.session.Session.AccountType;
import net.minecraft.text.Text;
import org.joml.Matrix4f;

@Environment(EnvType.CLIENT)
public class AltManagerScreen extends Screen {
    private final Screen parent;
    private final NickNameElement nickNameElement;
    private final StringElement nameElement;
    private final StringElement tagElement;
    public int preScroll;
    public int mainScroll;
    private int click;
    private int key;
    private String type;
    private final GenerateAccoutButton generateAccoutButton;
    private final CreateAccoutButton createAccoutButton;
    private final Animation animation;

    public AltManagerScreen(Screen parent) {
        super(Text.of(""));
        this.parent = parent;
        this.nameElement = new StringElement(Type.NAME);
        this.tagElement = new StringElement(Type.TAG);
        this.click = -1;
        this.type = "";
        this.generateAccoutButton = new GenerateAccoutButton("", () -> {
            String name = MeoRayClient.INSTANCE.getNameGen().generate();
            NickName nickName = new NickName(name, this.tagElement.getValue());
            this.tagElement.setValue("");
            this.nameElement.setValue("");
            MeoRayClient.INSTANCE.getNickNameManager().addNickname(nickName);
            IMinecraftClientMixin instance = (IMinecraftClientMixin)MinecraftClient.getInstance();
            Session session = MinecraftClient.getInstance().getSession();
            instance.setSession(new Session(name, UUID.randomUUID(),
                session.getAccessToken(), session.getXuid(), session.getClientId(), AccountType.LEGACY));
            this.resetSelect();
        });
        this.createAccoutButton = new CreateAccoutButton("Create", () -> {
            NickName nickName = new NickName(this.nameElement.getValue(), this.tagElement.getValue());
            this.tagElement.setValue("");
            this.nameElement.setValue("");
            MeoRayClient.INSTANCE.getNickNameManager().addNickname(nickName);
            IMinecraftClientMixin instance = (IMinecraftClientMixin)MinecraftClient.getInstance();
            Session session = MinecraftClient.getInstance().getSession();
            instance.setSession(new Session(nickName.getNickname(), UUID.randomUUID(),
                session.getAccessToken(), session.getXuid(), session.getClientId(), AccountType.LEGACY));
            this.resetSelect();
        });
        this.nameElement.setSibling(this.tagElement);
        this.tagElement.setSibling(this.nameElement);
        this.nickNameElement = new NickNameElement();
        this.animation = new EaseBackIn(400, 1.0, 0.1f, Direction.FORWARDS);
    }

    protected void init() {
        this.animation.reset();
        this.animation.setDirection(Direction.FORWARDS);
        double centerX = this.width / 2.0 - 247;
        this.createAccoutButton.updatePos(centerX + 10.0, this.height / 2.0 + 25.0 + 18.0);
        this.generateAccoutButton.updatePos(centerX + 10.0 + 122.0 + 7.0, this.height / 2.0 + 25.0 + 18.0);
    }

    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.initStrings(this.key, this.type);
        Matrix4f matrix = context.getMatrices().peek().getPositionMatrix();
        AnimationUtil.sizeAnimation(context, this.width / 2.0, this.height / 2.0, 1.35 - this.animation.getOutput() * 0.35);

        ((BuiltRectangle)Builder.rectangle()
            .size(new SizeState(this.width, this.height))
            .color(new QuadColorState(new Color(-15395300, true)))
            .radius(new QuadRadiusState(0.0F)).smoothness(1.15F).build())
            .render(matrix, 0, 0);

        double centerX = this.width / 2.0 - 247;

        ((BuiltRectangle)Builder.rectangle()
            .size(new SizeState(170.0F, 110.0F))
            .color(new QuadColorState(new Color(-15000539, true)))
            .radius(new QuadRadiusState(5.0F)).smoothness(1.15F).build())
            .render(matrix, (float)centerX, (float)(this.height / 2 - 35));
        ((BuiltBorder)Builder.border()
            .size(new SizeState(170.0F, 110.0F))
            .color(new QuadColorState(new Color(2040108)))
            .radius(new QuadRadiusState(5.0F)).thickness(0.01F)
            .smoothness(0.7F, 0.7F).build())
            .render(matrix, (float)centerX, (float)(this.height / 2 - 35));
        ((BuiltText)Builder.text().font(FontManager.MAINMENU.get()).text("F")
            .color(new Color(-11710630, true)).size(7.0F).thickness(0.05F).build())
            .render(matrix, (float)(centerX + 13.0), (float)(this.height / 2.0 - 25.5));
        ((BuiltText)Builder.text().font(FontManager.SUISSEINTREGULAR.get()).text("AltManager")
            .color(new Color(-11710630, true)).size(7.0F).thickness(0.05F).build())
            .render(matrix, (float)(centerX + 23.5), (float)(this.height / 2.0 - 26.5));

        this.nameElement.button(centerX + 10.0, this.height / 2.0 - 30.0 + 20.0, 150.0, 20.0,
            mouseX, mouseY, 0, 0, 0, 0, context, this.click);
        this.tagElement.button(centerX + 10.0, this.height / 2.0 - 5.0 + 20.0, 150.0, 20.0,
            mouseX, mouseY, 0, 0, 0, 0, context, this.click);

        this.generateAccoutButton.render(matrix);
        this.createAccoutButton.render(matrix);

        ((BuiltRectangle)Builder.rectangle()
            .size(new SizeState(335.0F, 210.0F))
            .color(new QuadColorState(new Color(-15000539, true)))
            .radius(new QuadRadiusState(5.0F)).smoothness(1.15F).build())
            .render(matrix, (float)(centerX + 190.0), (float)(this.height / 2 - 85));
        ((BuiltBorder)Builder.border()
            .size(new SizeState(335.0F, 210.0F))
            .color(new QuadColorState(new Color(2040108)))
            .radius(new QuadRadiusState(5.0F)).thickness(0.01F)
            .smoothness(0.6F, 0.6F).build())
            .render(matrix, (float)(centerX + 190.0), (float)(this.height / 2 - 85));

        double scissorOffset = 50.0 - this.animation.getOutput() * 50.0;
        Scissor.start(
            (float)((centerX + 190.0) - scissorOffset),
            (float)((this.height / 2.0 - 85.0) - scissorOffset),
            (float)(335.0 * (1.35 - this.animation.getOutput() * 0.35)),
            (float)(210.0 * (1.35 - this.animation.getOutput() * 0.35)));

        double offsetX = 0;
        double offsetY = 0;

        try {
            for (NickName nickName : MeoRayClient.INSTANCE.getNickNameManager().getNickNames()) {
                this.nickNameElement.setNickName(nickName);
                this.nickNameElement.button(
                    centerX + 200.0 + offsetX,
                    this.height / 2.0 - 75.0 + offsetY + this.mainScroll,
                    155.0, 35.0,
                    mouseX, mouseY,
                    centerX + 190.0, this.height / 2.0 - 85.0,
                    centerX + 190.0 + 335.0, this.height / 2.0 - 85.0 + 210.0,
                    context, this.click);
                offsetX += 160.0;
                if (offsetX > 200.0) {
                    offsetX = 0;
                    offsetY += 39.5;
                }
            }
        } catch (Exception ignored) {
        }

        Scissor.stop();

        if (this.preScroll < -offsetY) {
            this.preScroll = (int)MathUtil.fast(this.preScroll, (float)-offsetY, 1000.0F);
        } else if (this.preScroll > 0) {
            this.preScroll = (int)MathUtil.fast(this.preScroll, 0, 1000.0F);
        }
        this.mainScroll = (int)MathUtil.fast(this.mainScroll, this.preScroll, 15.0F);

        this.key = 0;
        this.type = "";
        this.click = -1;
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        this.click = button;
        if (MathUtil.isHovered(
            (int)this.generateAccoutButton.getX(), (int)this.generateAccoutButton.getY(),
            (int)this.generateAccoutButton.getWidth(), (int)this.generateAccoutButton.getHeight(),
            (int)mouseX, (int)mouseY)) {
            this.generateAccoutButton.onClick();
        }
        if (MathUtil.isHovered(
            (int)this.createAccoutButton.getX(), (int)this.createAccoutButton.getY(),
            (int)this.createAccoutButton.getWidth(), (int)this.createAccoutButton.getHeight(),
            (int)mouseX, (int)mouseY)) {
            this.createAccoutButton.onClick();
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    public void close() {
        this.client.setScreen(this.parent);
    }

    private void initStrings(int key, String type) {
        this.nameElement.setKey(key);
        this.tagElement.setKey(key);
        this.nameElement.setType(type);
        this.tagElement.setType(type);
    }

    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        this.key = keyCode;
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    public boolean charTyped(char chr, int modifiers) {
        this.type = String.valueOf(chr);
        return super.charTyped(chr, modifiers);
    }

    private void resetSelect() {
        this.nameElement.resetSelect();
        this.tagElement.resetSelect();
    }

    public StringElement getNameElement() { return this.nameElement; }
    public StringElement getTagElement() { return this.tagElement; }

    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        this.preScroll += (int)(verticalAmount * 30.0);
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }
}
