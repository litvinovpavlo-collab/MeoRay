package dev.meoray.client.feature.combat;
import dev.meoray.client.core.Category;
import dev.meoray.client.core.Module;
import dev.meoray.client.core.setting.ModeSetting;
import dev.meoray.client.core.setting.NumberSetting;
public class Velocity extends Module {
    public Velocity() { super("Velocity", "Убирает отдачу от ударов", Category.COMBAT);
        add(new ModeSetting("Mode", "Intave", "Intave", "Normal", "Cancel"));
        add(new NumberSetting("Horizontal", 0, 0, 100, 5));
    }
}
