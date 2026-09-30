package dev.aether.theme;

public final class AetherTheme {
    private final String name;
    private final ThemePalette palette;

    private AetherTheme(String name, ThemePalette palette) {
        this.name = name;
        this.palette = palette;
    }

    public static AetherTheme of(String name, ThemePalette palette) {
        return new AetherTheme(name == null ? "Custom" : name, palette);
    }

    /**
     * The palette the client starts on, before any theme module is enabled: translucent black and
     * white, so the default UI is the Leaf composition in those two colours. See
     * {@link ThemePalettes#mono()} - the palette is shared with the Monochrome theme module so the
     * default and the module can never drift apart.
     */
    public static AetherTheme defaultTheme() {
        return new AetherTheme("Monochrome", ThemePalettes.mono());
    }

    public String name() {
        return name;
    }

    public ThemePalette palette() {
        return palette;
    }
}

