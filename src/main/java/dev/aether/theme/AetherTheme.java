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
     * The palette the client starts on, before any theme module is enabled.
     * <p>
     * It is deliberately dark. Every Aether screen is designed as charcoal glass, and the light
     * palettes are opt-in theme modules. A light default did not just look wrong: the theme hub
     * repaints the shared tokens from the active palette on the first frame, so a near-white
     * default replaced the UI's own dark surface, deck and panel tokens and the Control Center
     * rendered as one large washed-out panel no screen could opt out of.
     */
    public static AetherTheme defaultTheme() {
        return new AetherTheme("Aether", new ThemePalette(
            ColorRgb.of(8, 9, 13),
            ColorRgb.of(29, 30, 37),
            ColorRgb.of(155, 140, 255),
            ColorRgb.of(242, 242, 245),
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

