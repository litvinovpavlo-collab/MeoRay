package dev.meoray.client.feature.misc;

import dev.meoray.client.core.Category;
import dev.meoray.client.core.Module;
import dev.meoray.client.core.setting.BooleanSetting;
import dev.meoray.client.core.setting.SectionSetting;

public class Interface extends Module {
    public final BooleanSetting crosshair = add(new BooleanSetting("Crosshair", false));
    public final BooleanSetting watermark = add(new BooleanSetting("Watermark", false));
    public final BooleanSetting coordinates = add(new BooleanSetting("Coordinates", false));
    public final BooleanSetting potions = add(new BooleanSetting("Potions", false));
    public final BooleanSetting inventoryHud = add(new BooleanSetting("InventoryHUD", false));
    public final BooleanSetting targetHud = add(new BooleanSetting("TargetHUD", false));
    public final BooleanSetting keyList = add(new BooleanSetting("KeyList", false));
    public final BooleanSetting hotbar = add(new BooleanSetting("Hotbar", false));
    public final BooleanSetting armorHud = add(new BooleanSetting("ArmorHUD", false));
    public final BooleanSetting keyBinds = add(new BooleanSetting("KeyBinds", false));
    public final BooleanSetting notifications = add(new BooleanSetting("Notifications", true));

    public final SectionSetting capeSection = add(new SectionSetting("Custom Cape"));
    public final BooleanSetting customCape = capeSection.add(new BooleanSetting("Custom Cape", true));

    public Interface() {
        super("Interface", "Manage HUD elements", Category.MISC);
    }
}
