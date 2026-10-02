package dev.aether.ui;

/**
 * The top-level destinations of the Aether GUI.
 * <p>
 * The information architecture is Aether's own, not Leaf's four-tile layout: Modules, Cosmetics,
 * HUD Editor, Appearance, Profiles, Settings. The declaration order is the navigation order; the
 * navigation bar's x positions are computed responsively from the design width (see
 * {@code AetherGuiScreen}), so reordering this enum moves the tiles with it and the two lists
 * stay in step by construction.
 * <p>
 * Themes are not a section. A theme is configuration worn on the Appearance screen; the
 * {@code dev.aether.theme.ThemeManager} owns it and the module registry never sees it.
 */
public enum GuiSection {
    MODULES("Modules"),
    COSMETICS("Cosmetics"),
    HUD("HUD Editor"),
    APPEARANCE("Appearance"),
    PROFILES("Profiles"),
    SETTINGS("Settings");

    private static final GuiSection[] ORDERED = values();

    private final String label;

    GuiSection(String label) {
        this.label = label;
    }

    /** The name shown in the navigation bar and stored in the config. */
    public String label() {
        return this.label;
    }

    /** Sections in navigation order. */
    public static GuiSection[] ordered() {
        return ORDERED.clone();
    }

    /** Section labels in navigation order, for preference validation. */
    public static String[] labels() {
        String[] labels = new String[ORDERED.length];
        for (int i = 0; i < ORDERED.length; i++) {
            labels[i] = ORDERED[i].label;
        }
        return labels;
    }

    /** @return the section named by {@code value} (case-insensitive), or {@code fallback}. */
    public static GuiSection fromLabel(String value, GuiSection fallback) {
        if (value != null) {
            String trimmed = value.trim();
            for (GuiSection section : ORDERED) {
                if (section.label.equalsIgnoreCase(trimmed) || section.name().equalsIgnoreCase(trimmed)) {
                    return section;
                }
            }
        }
        return fallback;
    }

    /** Wraps around the section list, so keyboard navigation can cycle like a ribbon. */
    public GuiSection shift(int direction) {
        int size = ORDERED.length;
        int index = (ordinal() + direction) % size;
        if (index < 0) {
            index += size;
        }
        return ORDERED[index];
    }
}
