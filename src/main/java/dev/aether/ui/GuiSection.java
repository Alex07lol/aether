package dev.aether.ui;

/**
 * The top-level destinations of the Aether GUI.
 * <p>
 * The information architecture follows the compact navigation model: Home, Modules,
 * Cosmetics, Appearance, Profiles, Settings - all routed inside one menu window. HUD
 * is not a routed category: the editor is fullscreen and opens from the rail's
 * dedicated Edit HUD button, so {@link #HUD} exists for call sites but never renders
 * a rail tile. The declaration order is the navigation order.
 * <p>
 * Themes are not a section. A theme is configuration worn on the Appearance screen; the
 * {@code dev.aether.theme.ThemeManager} owns it and the module registry never sees it.
 */
public enum GuiSection {
    HOME("Home"),
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
