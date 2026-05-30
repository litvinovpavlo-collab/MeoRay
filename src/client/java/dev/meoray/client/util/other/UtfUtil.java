package dev.meoray.client.util.other;

public class UtfUtil {
    public static boolean containsRussianLetter(String text) {
        return text != null && !text.isEmpty() ? text.matches(".*[А-яЁё].*") : false;
    }
}
