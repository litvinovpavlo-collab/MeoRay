package dev.meoray.client.feature.combat;
import dev.meoray.client.core.Category;
import dev.meoray.client.core.Module;
import dev.meoray.client.core.setting.ModeSetting;
import dev.meoray.client.core.setting.NumberSetting;
public class KillAura extends Module {
    public KillAura() { super("KillAura", "Автоматическая атака ближайших целей", Category.COMBAT);
        add(new ModeSetting("Mode", "Switch", "Switch", "Single", "Multi"));
        add(new NumberSetting("Range", 4.0, 2.5, 6.0, 0.1));
    }
}
