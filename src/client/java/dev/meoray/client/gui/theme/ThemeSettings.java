package dev.meoray.client.gui.theme;

public class ThemeSettings {
    private boolean glowEnabled;
    private float glowStrength;

    public ThemeSettings() {
        this.glowEnabled = true;
        this.glowStrength = 1.0F;
    }

    public ThemeSettings(boolean glowEnabled, float glowStrength) {
        this.glowEnabled = glowEnabled;
        this.glowStrength = glowStrength;
    }

    public boolean isGlowEnabled() {
        return glowEnabled;
    }

    public void setGlowEnabled(boolean glowEnabled) {
        this.glowEnabled = glowEnabled;
    }

    public float getGlowStrength() {
        return glowStrength;
    }

    public void setGlowStrength(float glowStrength) {
        this.glowStrength = glowStrength;
    }
}
