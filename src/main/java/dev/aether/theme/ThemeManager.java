package dev.aether.theme;

/**
 * The client's themes: the built-in palettes and which one is worn.
 * <p>
 * This is the theme system's own state, deliberately outside the module registry. The previous
 * design enabled a palette through a registered module, which forced the registry to carry a
 * {@code ModuleKind.THEME} category and a mutual-exclusion group just so the palettes could be
 * switched - a theme is a look, not a feature, and owning it as configuration rather than as a
 * module means the module browser can never list it and no screen has to filter anything.
 * <p>
 * All palettes are immutable singletons (renderers read them every frame), so handing out the
 * same instances is free. The active palette is chosen by id and persists through the client's
 * save path; an unknown id falls back to the default.
 */
public final class ThemeManager {

    /** The palette the client starts on: translucent black and white. */
    public static final String DEFAULT_THEME_ID = "theme.monochrome";

    private final ThemePalette[] palettes = {
        ThemePalettes.mono(),
        ThemePalettes.aetherBlue(),
        ThemePalettes.midnight(),
        ThemePalettes.aurora(),
        ThemePalettes.frost(),
        ThemePalettes.light(),
    };
    private final String[] ids = {
        "theme.monochrome",
        "theme.aether_blue",
        "theme.midnight",
        "theme.aurora",
        "theme.frost",
        "theme.light",
    };
    private final String[] names = {
        "Monochrome",
        "Aether Blue",
        "Midnight",
        "Aurora",
        "Frost",
        "Light",
    };

    private String activeId = DEFAULT_THEME_ID;

    /** All themes, in the order the Appearance screen shows them. */
    public ThemeDefinition[] themes() {
        ThemeDefinition[] result = new ThemeDefinition[ids.length];
        for (int i = 0; i < ids.length; i++) {
            result[i] = new ThemeDefinition(ids[i], names[i], palettes[i]);
        }
        return result;
    }

    /** @return the count of registered themes, so a screen can page without materialising them. */
    public int count() {
        return ids.length;
    }

    public String activeId() {
        return this.activeId;
    }

    public ThemeDefinition active() {
        return definitionOf(this.activeId);
    }

    /** Wears the theme with the given id; an unknown id keeps the current one. */
    public void select(String id) {
        if (definitionOf(id) != null) {
            this.activeId = id;
        }
    }

    /** Wears the default palette again. */
    public void resetToDefault() {
        this.activeId = DEFAULT_THEME_ID;
    }

    /**
     * Restores the worn theme from the config document. Also migrates configs written by the
     * builds that enabled a palette through a registered module: the {@code module.theme.<id>
     * .enabled} keys are read once here and the module-era keys are left alone (the modules they
     * describe no longer exist, so the registry simply never touches them again).
     */
    public void applyConfig(dev.aether.config.ConfigDocument document) {
        String stored = document.get("theme.active", null);
        if (stored == null) {
            // Module-era migration: the last enabled palette module wins, if any was left on.
            for (String id : ids) {
                if (document.getBoolean("module." + id + ".enabled", false)) {
                    stored = id;
                }
            }
        }
        if (stored != null && definitionOf(stored) != null) {
            this.activeId = stored;
        }
    }

    public void writeConfig(dev.aether.config.ConfigDocument.Builder builder) {
        if (!isDefaultActive()) {
            builder.put("theme.active", this.activeId);
        }
    }

    public ThemeDefinition definitionOf(String id) {
        if (id != null) {
            for (int i = 0; i < ids.length; i++) {
                if (ids[i].equalsIgnoreCase(id.trim())) {
                    return new ThemeDefinition(ids[i], names[i], palettes[i]);
                }
            }
        }
        return null;
    }

    /** @return true when the active theme is the one the client ships on. */
    public boolean isDefaultActive() {
        return DEFAULT_THEME_ID.equals(this.activeId);
    }
}
