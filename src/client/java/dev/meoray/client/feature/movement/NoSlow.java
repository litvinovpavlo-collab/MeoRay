package dev.meoray.client.feature.movement;
import dev.meoray.client.core.Category;
import dev.meoray.client.core.Module;
import dev.meoray.client.core.setting.ModeSetting;
public class NoSlow extends Module {
    public NoSlow() { super("NoSlow", "Remove slow effects", Category.MOVEMENT);
        add(new ModeSetting("Mode", "Vanilla", "Vanilla", "NCP", "Strict"));
    }
}
