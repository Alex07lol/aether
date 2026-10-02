package dev.aether.theme;

/**
 * One selectable theme: an id, a display name and the palette it wears.
 * <p>
 * A definition is what the Appearance screen lists. The palette object itself is an
 * immutable singleton from {@link ThemePalettes}; the definition is a lightweight view of
 * it, built on demand by {@link ThemeManager}.
 */
public final class ThemeDefinition {

    private final String id;
    private final String name;
    private final ThemePalette palette;

    ThemeDefinition(String id, String name, ThemePalette palette) {
        this.id = id;
        this.name = name;
        this.palette = palette;
    }

    public String id() {
        return this.id;
    }

    public String name() {
        return this.name;
    }

    public ThemePalette palette() {
        return this.palette;
    }
}
