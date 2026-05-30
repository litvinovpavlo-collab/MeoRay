package dev.meoray.client.feature.visual.wings;

import dev.meoray.client.core.Category;
import dev.meoray.client.core.Module;
import java.util.Arrays;
import java.util.List;

public class Wings extends Module {

    public final List<String> modes = Arrays.asList("Angel", "Bat", "Azalea", "Allay");
    public String currentMode = "Angel";

    public Wings() {
        super("Wings", "Wings on your back", Category.RENDER);
    }

    public void setMode(String mode) {
        if (modes.contains(mode)) {
            this.currentMode = mode;
        }
    }

    public String getTextureName() {
        switch (currentMode) {
            case "Bat":
                return "bat_wing_1.png";
            case "Azalea":
                return "azalea_wing_1.png";
            case "Allay":
                return "allay_wing_1.png";
            case "Angel":
            default:
                return "angel_wings.png";
        }
    }
}
