package dev.aether.theme;

public final class ThemePalette {
    private final ColorRgb surface;
    private final ColorRgb surfaceSoft;
    private final ColorRgb accent;
    private final ColorRgb text;
    private final ColorRgb glassHighlight;

    public ThemePalette(ColorRgb surface, ColorRgb surfaceSoft, ColorRgb accent, ColorRgb text, ColorRgb glassHighlight) {
        this.surface = surface;
        this.surfaceSoft = surfaceSoft;
        this.accent = accent;
        this.text = text;
        this.glassHighlight = glassHighlight;
    }

    public ColorRgb surface() {
        return surface;
    }

    public ColorRgb surfaceSoft() {
        return surfaceSoft;
    }

    public ColorRgb accent() {
        return accent;
    }

    public ColorRgb text() {
        return text;
    }

    public ColorRgb glassHighlight() {
        return glassHighlight;
    }
}

