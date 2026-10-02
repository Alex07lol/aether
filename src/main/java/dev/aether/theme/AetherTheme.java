package dev.aether.theme;

/**
 * The theme the client is wearing right now: a name plus the palette every adapter renders from.
 * <p>
 * Instances come from {@link ThemeManager} (which owns which theme is active) and are cached in
 * the client, because renderers call for the palette every frame and comparing identity is the
 * cheapest way for a screen to notice a switch.
 */
public final class AetherTheme {
    private final String name;
    private final ThemePalette palette;
    private final String id;

    AetherTheme(String id, String name, ThemePalette palette) {
        this.id = id;
        this.name = name;
        this.palette = palette;
    }

    public static AetherTheme of(ThemeDefinition definition) {
        return new AetherTheme(definition.id(), definition.name(), definition.palette());
    }

    public String id() {
        return id;
    }

    public String name() {
        return name;
    }

    public ThemePalette palette() {
        return palette;
    }
}
