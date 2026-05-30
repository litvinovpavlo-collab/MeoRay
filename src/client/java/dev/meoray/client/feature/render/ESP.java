package dev.meoray.client.feature.render;
import dev.meoray.client.core.Category;
import dev.meoray.client.core.Module;
import dev.meoray.client.core.setting.BooleanSetting;
public class ESP extends Module {
    public ESP() {
        super("ESP", "Подсветка игроков сквозь стены", Category.RENDER);
        add(new BooleanSetting("Players", true));
        add(new BooleanSetting("Monsters", false));
        add(new BooleanSetting("Animals", false));
    }
}
