package dev.meoray.client.feature.render;
import dev.meoray.client.core.Category;
import dev.meoray.client.core.Module;
import dev.meoray.client.core.setting.NumberSetting;
public class ChinaHat extends Module {
    public ChinaHat() { super("ChinaHat", "Render china hat", Category.RENDER);
        add(new NumberSetting("Radius", 0.8, 0.3, 1.2, 0.1));
        add(new NumberSetting("Height", 0.5, 0.2, 0.8, 0.1));
    }
}
