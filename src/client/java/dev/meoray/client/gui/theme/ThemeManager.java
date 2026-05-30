package dev.meoray.client.gui.theme;

import dev.meoray.client.util.animations.AnimatedFloat;
import dev.meoray.client.util.animations.Easing;

import java.util.HashMap;
import java.util.Map;

public class ThemeManager {

    private int selectedIndex = 0;
    private final Map<String, ThemeSettings> settingsMap = new HashMap<>();
    private final Theme[] themes = {
        new Theme("Dark Carbon",
            0xFF0C0C0E, 0xFF121216, 0xFFDCDCE1,
            0xFF18181C, 0xFFE8E8ED, 0xFF888896, 0xFF32323E,
            0xFF0C0C0E, 0xFF0A0A0C),
        new Theme("Amethyst",
            0xFF1A1028, 0xFF221838, 0xFFFF6FD8,
            0xFF2A1F3A, 0xFFE8DFF0, 0xFF9980B0, 0xFF3A2850,
            0xFF2A1538, 0xFF0E0818),
        new Theme("Emerald",
            0xFF0A1A0A, 0xFF0F220F, 0xFF50FF50,
            0xFF142814, 0xFFD0F0D0, 0xFF70A070, 0xFF1A3A1A,
            0xFF0E220E, 0xFF060E06),
        new Theme("Dark",
            0xFF0A0A0A, 0xFF111111, 0xFF6C63FF,
            0x33151515, 0xFFDDDDDD, 0xFF777777, 0xFF222222,
            0xFF14141A, 0xFF060608),
        new Theme("Ocean",
            0xFF0A1628, 0xFF0F1F3A, 0xFF00E5FF,
            0x33152A4A, 0xFFCCE0FF, 0xFF8899BB, 0xFF1A3A5A,
            0xFF0F2038, 0xFF060E18),
        new Theme("Forest",
            0xFF0D1A12, 0xFF12281A, 0xFF50C878,
            0x331A3015, 0xFFCCEEDD, 0xFF88AA99, 0xFF1A3A2A,
            0xFF12281A, 0xFF060E0A),
        new Theme("Sunset",
            0xFF1A0D0D, 0xFF281212, 0xFFFF8C42,
            0x33301515, 0xFFEEDDCC, 0xFFAA9988, 0xFF3A1A1A,
            0xFF281818, 0xFF100808),
        new Theme("Catppuccin",
            0xFF1E1E2E, 0xFF181825, 0xFFCBA6F7,
            0x33252535, 0xFFCDD6F4, 0xFFA6ADC8, 0xFF313244,
            0xFF252540, 0xFF12121A),
        new Theme("Nord",
            0xFF2E3440, 0xFF252B36, 0xFF88C0D0,
            0x333B4252, 0xFFECEFF4, 0xFF8FBCBB, 0xFF434C5E,
            0xFF354050, 0xFF1A1E26),
        new Theme("Dracula",
            0xFF282A36, 0xFF1E1F2B, 0xFFBD93F9,
            0x33333345, 0xFFF8F8F2, 0xFF9580B0, 0xFF44475A,
            0xFF352F3E, 0xFF14151C),
        new Theme("Cherry",
            0xFF1A0D14, 0xFF22101A, 0xFFFF6B9D,
            0x33281520, 0xFFF0DCE8, 0xFFAA809A, 0xFF3A2030,
            0xFF2A0D1A, 0xFF0A0508),
        new Theme("Black Gold",
            0xFF050505, 0xFF0A0A0A, 0xFFD4AF37,
            0x33101010, 0xFFE8E0CC, 0xFFAA9E80, 0xFF1A1A1A,
            0xFF1A1408, 0xFF050505),
        new Theme("DarkGrey",
            0xFF1A1A1A, 0xFF222222, 0xFF6C63FF,
            0x33252525, 0xFFF0F0F0, 0xFFAAAAAA, 0xFF333333,
            0xFF2A2A2A, 0xFF121212)
    };

    private final AnimatedFloat transition = new AnimatedFloat(1f);
    private Theme prevTheme;

    public ThemeManager() {
        prevTheme = themes[0];
        for (Theme t : themes) {
            settingsMap.put(t.name(), new ThemeSettings());
        }
    }

    public Theme[] getThemes() {
        return themes;
    }

    public int getSelectedIndex() {
        return selectedIndex;
    }

    public Theme getCurrentTheme() {
        return themes[selectedIndex];
    }

    public Theme getRenderTheme() {
        float t = transition.update();
        if (transition.isFinished()) {
            return themes[selectedIndex];
        }
        return interpolate(prevTheme, themes[selectedIndex], t);
    }

    public ThemeSettings getSettings(String name) {
        return settingsMap.get(name);
    }

    public ThemeSettings getCurrentSettings() {
        return settingsMap.get(themes[selectedIndex].name());
    }

    public boolean isTransitioning() {
        return !transition.isFinished();
    }

    public void select(int index) {
        if (index < 0 || index >= themes.length || index == selectedIndex) return;
        prevTheme = getRenderTheme();
        selectedIndex = index;
        transition.snapTo(0f);
        transition.animate(1f, 1000L, Easing.EASE_OUT_CUBIC);
    }

    private static Theme interpolate(Theme a, Theme b, float t) {
        return new Theme(
            b.name(),
            lerpColor(a.bgMain(), b.bgMain(), t),
            lerpColor(a.bgSidebar(), b.bgSidebar(), t),
            lerpColor(a.accent(), b.accent(), t),
            lerpColor(a.panel(), b.panel(), t),
            lerpColor(a.textPrimary(), b.textPrimary(), t),
            lerpColor(a.textSecondary(), b.textSecondary(), t),
            lerpColor(a.border(), b.border(), t),
            lerpColor(a.gradientTop(), b.gradientTop(), t),
            lerpColor(a.gradientBottom(), b.gradientBottom(), t),
            lerpColor(a.glow(), b.glow(), t)
        );
    }

    private static int lerpColor(int c1, int c2, float t) {
        int a = lerpChannel((c1 >> 24) & 0xFF, (c2 >> 24) & 0xFF, t);
        int r = lerpChannel((c1 >> 16) & 0xFF, (c2 >> 16) & 0xFF, t);
        int g = lerpChannel((c1 >> 8) & 0xFF, (c2 >> 8) & 0xFF, t);
        int b = lerpChannel(c1 & 0xFF, c2 & 0xFF, t);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private static int lerpChannel(int a, int b, float t) {
        return Math.max(0, Math.min(255, (int) (a + (b - a) * t)));
    }
}
