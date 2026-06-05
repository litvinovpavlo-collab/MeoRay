package dev.meoray.client.core;

public enum Category {
    COMBAT("Combat", "A", "icons"),
    MOVEMENT("Movement", "J", "icons"),
    PLAYER("Player", "I", "icons"),
    RENDER("Render", "B", "icons"),
    WORLD("World", "E", "icons"),
    MISC("Misc", "G", "icons"),
    SETTINGS("Settings", "F", "icons"),
    THEMES("Themes", "C", "icons");

    private final String name;
    private final String icon;
    private final String font;

    Category(String name, String icon, String font) {
        this.name = name;
        this.icon = icon;
        this.font = font;
    }

    public String getName() {
        return name;
    }

    public String getIcon() {
        return icon;
    }

    public String getFont() {
        return font;
    }

    public static Category[] ALL = values();
}
