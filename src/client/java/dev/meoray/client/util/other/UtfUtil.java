package dev.meoray.client.util.other;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public class UtfUtil {
    public static boolean containsRussianLetter(String text) {
        return text != null && !text.isEmpty() ? text.matches(".*[А-яЁё].*") : false;
    }
}
