package dev.aether.ui;

/**
 * The top-level pages of the Aether Control Center.
 * <p>
 * The Control Center is one of the few surfaces where naming has to be exact, because the section
 * name is what a stored preference opens the menu on. Keeping the list here - in core, next to the
 * search and the state machine - means the sidebar, the keyboard cycle and
 * {@code ClientPreferences} all read the same array and a new page cannot be added in one place and
 * forgotten in another.
 * <p>
 * A page only becomes a section once it has real content; the ordering below is the sidebar order.
 */
public enum ControlCenterSection {
    MODULES("Modules"),
    PROFILES("Profiles"),
    THEMES("Themes"),
    COSMETICS("Cosmetics"),
    SCREENSHOTS("Screenshots"),
    SETTINGS("Settings");

    private static final ControlCenterSection[] ORDERED = values();

    private final String label;

    ControlCenterSection(String label) {
        this.label = label;
    }

    /** The name shown in the sidebar and stored in the config. */
    public String label() {
        return this.label;
    }

    /** The stable identifier used by the search/filter machinery and the state machine. */
    public String key() {
        return name();
    }

    /** Sections in sidebar order. */
    public static ControlCenterSection[] ordered() {
        return ORDERED.clone();
    }

    /** Section names in sidebar order, for preference validation and the screens that print them. */
    public static String[] labels() {
        String[] labels = new String[ORDERED.length];
        for (int i = 0; i < ORDERED.length; i++) {
            labels[i] = ORDERED[i].label;
        }
        return labels;
    }

    /** @return the section named by {@code value} (case-insensitive), or {@code fallback}. */
    public static ControlCenterSection fromLabel(String value, ControlCenterSection fallback) {
        if (value != null) {
            String trimmed = value.trim();
            for (ControlCenterSection section : ORDERED) {
                if (section.label.equalsIgnoreCase(trimmed) || section.name().equalsIgnoreCase(trimmed)) {
                    return section;
                }
            }
        }
        return fallback;
    }

    /** Wraps around the section list, so Tab can cycle the way a ribbon does. */
    public ControlCenterSection shift(int direction) {
        int size = ORDERED.length;
        int index = (ordinal() + direction) % size;
        if (index < 0) {
            index += size;
        }
        return ORDERED[index];
    }
}
