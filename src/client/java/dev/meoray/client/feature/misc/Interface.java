package dev.meoray.client.feature.misc;

import dev.meoray.client.core.Category;
import dev.meoray.client.core.Module;
import dev.meoray.client.core.setting.BooleanSetting;

public class Interface extends Module {
    public final BooleanSetting crosshair = add(new BooleanSetting("Crosshair", false));
    public final BooleanSetting watermark = add(new BooleanSetting("Watermark", false));
    public final BooleanSetting coordinates = add(new BooleanSetting("Coordinates", false));
    public final BooleanSetting potions = add(new BooleanSetting("Potions", false));
    public final BooleanSetting inventoryHud = add(new BooleanSetting("InventoryHUD", false));
    public final BooleanSetting targetHud = add(new BooleanSetting("TargetHUD", false));
    public final BooleanSetting keyList = add(new BooleanSetting("KeyList", false));

    public Interface() {
        super("Interface", "Manage HUD elements", Category.MISC);
    }
}
