package dev.meoray.client.feature.combat;
import dev.meoray.client.core.Category;
import dev.meoray.client.core.Module;
import dev.meoray.client.core.setting.ModeSetting;
public class AutoPotion extends Module {
    public AutoPotion() { super("AutoPotion", "Auto throw potions", Category.COMBAT);
        add(new ModeSetting("Mode", "Auto", "Auto", "Heal", "Buff"));
    }
}
