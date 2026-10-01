package dev.aether.ui;

/**
 * The top-level destinations of the Aether GUI.
 * <p>
 * Follows the screen-per-section organization of Leaf Client's 1.8.9 GUI (mods /
 * cosmetics / HUD positions / client settings as sibling destinations), which replaces
 * the previous six-section Control Center sidebar. Kept in core, Minecraft-free, so the
 * preference that remembers the last open section and the screens read one list.
 * <p>
 * THEMES is Aether's own addition: Leaf has no theme picker, but Aether ships six palettes,
 * and a row of theme toggles buried in the Modules grid is not discoverable - the palettes
 * get their own screen.
 * <p>
 * The declaration order is the navigation order and pairs with the navigation bar's x positions,
 * which are Leaf's own tile rectangles: Modules at 430, Cosmetics at 650, Settings at 1320, and
 * Leaf's two inner tiles - the HUD editor at 1100 - keep their places, while Aether's Themes tile
 * takes the free slot at 860 in the middle of the row. Reordering this enum moves the tiles with
 * it, so the two lists must stay in step.
 */
public enum GuiSection {
    MODULES("Modules"),
    COSMETICS("Cosmetics"),
    THEMES("Themes"),
    HUD("HUD Editor"),
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
