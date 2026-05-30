package dev.meoray.client.gui.theme;

public record Theme(
    String name,
    int bgMain,
    int bgSidebar,
    int accent,
    int panel,
    int textPrimary,
    int textSecondary,
    int border,
    int gradientTop,
    int gradientBottom,
    int glow
) {
    public Theme(String name, int bgMain, int bgSidebar, int accent, int panel,
                 int textPrimary, int textSecondary, int border,
                 int gradientTop, int gradientBottom) {
        this(name, bgMain, bgSidebar, accent, panel,
             textPrimary, textSecondary, border,
             gradientTop, gradientBottom, accent);
    }
}
