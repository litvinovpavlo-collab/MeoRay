package dev.meoray.client.gui.screen;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import dev.meoray.client.account.AccountManager;
import dev.meoray.client.account.MeoRayAccount;
import dev.meoray.client.gui.common.SpaceBackground;
import dev.meoray.client.gui.font.MeoRayFont;
import dev.meoray.client.gui.common.CustomButton;

import java.util.List;

public class AccountManagerScreen extends Screen {

    private final Screen parent;
    private TextFieldWidget usernameField;
    private int scrollOffset = 0;
    private static final int ITEM_HEIGHT = 30;
    private static final int LIST_TOP = 70;

    public AccountManagerScreen(Screen parent) {
        super(Text.literal("Accounts"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;

        this.usernameField = new TextFieldWidget(this.textRenderer,
                centerX - 150, 35, 200, 22, Text.literal("Username"));
        this.usernameField.setMaxLength(16);
        this.usernameField.setPlaceholder(MeoRayFont.styled("Enter username..."));
        this.addDrawableChild(this.usernameField);

        this.addDrawableChild(new CustomButton(centerX + 55, 35, 95, 22,
                MeoRayFont.styled("+ Add Account"),
                b -> {
                    String name = usernameField.getText().trim();
                    if (!name.isEmpty()) {
                        AccountManager.addAccount(name);
                        usernameField.setText("");
                    }
                }));

        this.addDrawableChild(new CustomButton(centerX - 100, this.height - 30, 200, 22,
                MeoRayFont.styled("Back"),
                b -> this.client.setScreen(parent)));
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
        SpaceBackground.render(context, this.width, this.height);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        SpaceBackground.render(context, this.width, this.height);

        TextRenderer tr = MinecraftClient.getInstance().textRenderer;
        Text titleText = MeoRayFont.styled("Account Manager");
        int titleW = tr.getWidth(titleText);
        context.drawTextWithShadow(tr, titleText, this.width / 2 - titleW / 2, 10, 0xFF00BFFF);

        MeoRayAccount active = AccountManager.getActiveAccount();
        String activeText = active != null
                ? "Active: " + active.getUsername()
                : "Active: " + MinecraftClient.getInstance().getSession().getUsername() + " (default)";
        Text activeTxt = MeoRayFont.styled(activeText);
        int activeW = tr.getWidth(activeTxt);
        context.drawTextWithShadow(tr, activeTxt, this.width / 2 - activeW / 2, 22, 0xFFAAFFAA);

        renderAccountList(context, mouseX, mouseY);

        super.render(context, mouseX, mouseY, delta);
    }

    private void renderAccountList(DrawContext context, int mouseX, int mouseY) {
        List<MeoRayAccount> accounts = AccountManager.getAccounts();
        int centerX = this.width / 2;
        int listX = centerX - 180;
        int listW = 360;
        int listY = LIST_TOP;
        int listH = this.height - LIST_TOP - 40;

        context.fill(listX, listY, listX + listW, listY + listH, 0x88000000);
        context.fill(listX, listY, listX + listW, listY + 1, 0xFF1E40AF);
        context.fill(listX, listY + listH - 1, listX + listW, listY + listH, 0xFF1E40AF);
        context.fill(listX, listY, listX + 1, listY + listH, 0xFF1E40AF);
        context.fill(listX + listW - 1, listY, listX + listW, listY + listH, 0xFF1E40AF);

        if (accounts.isEmpty()) {
            TextRenderer tr = MinecraftClient.getInstance().textRenderer;
            Text empty = MeoRayFont.styled("No accounts. Add one above!");
            int ew = tr.getWidth(empty);
            context.drawTextWithShadow(tr, empty,
                    centerX - ew / 2, listY + listH / 2 - 4, 0xFF888888);
            return;
        }

        TextRenderer tr = MinecraftClient.getInstance().textRenderer;
        MeoRayAccount active = AccountManager.getActiveAccount();

        for (int i = 0; i < accounts.size(); i++) {
            MeoRayAccount acc = accounts.get(i);
            int itemY = listY + 5 + i * ITEM_HEIGHT - scrollOffset;

            if (itemY + ITEM_HEIGHT < listY || itemY > listY + listH) continue;

            int itemBg = (acc == active) ? 0x6600BFFF : 0x44000000;
            boolean itemHover = mouseX >= listX + 5 && mouseX <= listX + listW - 5
                    && mouseY >= itemY && mouseY <= itemY + ITEM_HEIGHT - 2;
            if (itemHover && acc != active) itemBg = 0x661E40AF;

            context.fill(listX + 5, itemY, listX + listW - 5, itemY + ITEM_HEIGHT - 2, itemBg);

            int borderColor = (acc == active) ? 0xFF00BFFF : 0xFF333355;
            context.fill(listX + 5, itemY, listX + listW - 5, itemY + 1, borderColor);
            context.fill(listX + 5, itemY + ITEM_HEIGHT - 3, listX + listW - 5, itemY + ITEM_HEIGHT - 2, borderColor);

            Text nameText = MeoRayFont.styled(acc.getUsername());
            context.drawTextWithShadow(tr, nameText, listX + 15, itemY + 10, 0xFFFFFFFF);

            if (acc == active) {
                Text activeLabel = MeoRayFont.styled("[ACTIVE]");
                int alw = tr.getWidth(activeLabel);
                context.drawTextWithShadow(tr, activeLabel, listX + listW - alw - 100, itemY + 10, 0xFF00FF88);
            }

            int btnLoginX = listX + listW - 95;
            int btnDeleteX = listX + listW - 45;
            int btnY = itemY + 5;

            boolean loginHover = mouseX >= btnLoginX && mouseX <= btnLoginX + 45
                    && mouseY >= btnY && mouseY <= btnY + 18;
            int loginBg = loginHover ? 0xFF00BFFF : 0xFF1E40AF;
            context.fill(btnLoginX, btnY, btnLoginX + 45, btnY + 18, loginBg);
            Text loginText = MeoRayFont.styled("Login");
            int ltw = tr.getWidth(loginText);
            context.drawTextWithShadow(tr, loginText,
                    btnLoginX + (45 - ltw) / 2, btnY + 5, 0xFFFFFFFF);

            boolean delHover = mouseX >= btnDeleteX && mouseX <= btnDeleteX + 40
                    && mouseY >= btnY && mouseY <= btnY + 18;
            int delBg = delHover ? 0xFFCC2222 : 0xFF881111;
            context.fill(btnDeleteX, btnY, btnDeleteX + 40, btnY + 18, delBg);
            Text delText = MeoRayFont.styled("Del");
            int dtw = tr.getWidth(delText);
            context.drawTextWithShadow(tr, delText,
                    btnDeleteX + (40 - dtw) / 2, btnY + 5, 0xFFFFFFFF);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            List<MeoRayAccount> accounts = AccountManager.getAccounts();
            int centerX = this.width / 2;
            int listX = centerX - 180;
            int listW = 360;
            int listY = LIST_TOP;

            for (int i = 0; i < accounts.size(); i++) {
                MeoRayAccount acc = accounts.get(i);
                int itemY = listY + 5 + i * ITEM_HEIGHT - scrollOffset;
                int btnLoginX = listX + listW - 95;
                int btnDeleteX = listX + listW - 45;
                int btnY = itemY + 5;

                if (mouseX >= btnLoginX && mouseX <= btnLoginX + 45
                        && mouseY >= btnY && mouseY <= btnY + 18) {
                    AccountManager.setActiveAccount(acc);
                    return true;
                }

                if (mouseX >= btnDeleteX && mouseX <= btnDeleteX + 40
                        && mouseY >= btnY && mouseY <= btnY + 18) {
                    AccountManager.removeAccount(acc);
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        scrollOffset -= (int)(verticalAmount * 15);
        int maxScroll = Math.max(0, AccountManager.getAccounts().size() * ITEM_HEIGHT - (this.height - LIST_TOP - 50));
        scrollOffset = Math.max(0, Math.min(scrollOffset, maxScroll));
        return true;
    }
}
