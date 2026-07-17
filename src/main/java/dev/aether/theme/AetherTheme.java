package dev.aether.theme;

public final class AetherTheme {
    private final String name;
    private final ThemePalette palette;

    private AetherTheme(String name, ThemePalette palette) {
        this.name = name;
        this.palette = palette;
    }

    public static AetherTheme defaultTheme() {
        return new AetherTheme("Aether Frost", new ThemePalette(
            ColorRgb.of(245, 251, 255),
            ColorRgb.of(210, 239, 255),
            ColorRgb.of(82, 190, 235),
            ColorRgb.of(22, 74, 112),
            ColorRgb.of(255, 255, 255)
        ));
    }

    public String name() {
        return name;
    }

    public ThemePalette palette() {
        return palette;
    }
}

