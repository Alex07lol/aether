package dev.aether.config;

import dev.aether.ui.GuiSection;

/**
 * Client-wide preferences: the handful of options that belong to Aether itself rather than to one
 * module, and therefore cannot live on a {@code Setting}.
 * <p>
 * They are stored in the same config document as everything else (under {@code preference.*}) so the
 * client keeps exactly one configuration file and one save path. Anything that can be expressed as a
 * module setting stays a module setting - this class deliberately stays small.
 */
public final class ClientPreferences {
    public static final String KEY_SAVE_ON_CLOSE = "preference.save_on_close";
    public static final String KEY_SHOW_TOOLTIPS = "preference.show_tooltips";
    public static final String KEY_OPEN_SECTION = "preference.open_section";
    public static final String KEY_INVERT_SCROLL = "preference.invert_scroll";

    /**
     * The GUI sections, in the order they are shown in the navigation bar. The list itself
     * lives in core next to the navigation model; this is the config-facing spelling of it.
     */
    public static final String[] SECTIONS = GuiSection.labels();

    private boolean saveOnClose = true;
    private boolean showTooltips = true;
    private boolean invertScroll = false;
    private String openSection = "Modules";

    public boolean saveOnClose() {
        return this.saveOnClose;
    }

    public void setSaveOnClose(boolean value) {
        this.saveOnClose = value;
    }

    public boolean showTooltips() {
        return this.showTooltips;
    }

    public void setShowTooltips(boolean value) {
        this.showTooltips = value;
    }

    /**
     * Whether the mouse wheel runs inverted in every Aether screen. {@code false} scrolls like
     * vanilla Minecraft (wheel up = towards the top of the list); {@code true} flips it, for
     * natural-scroll mice and trackpads.
     */
    public boolean invertScroll() {
        return this.invertScroll;
    }

    public void setInvertScroll(boolean value) {
        this.invertScroll = value;
    }

    /** The section the Control Center opens on; always one of {@link #SECTIONS}. */
    public String openSection() {
        return this.openSection;
    }

    public void setOpenSection(String value) {
        this.openSection = normalizeSection(value);
    }

    public void applyConfig(ConfigDocument document) {
        this.saveOnClose = document.getBoolean(KEY_SAVE_ON_CLOSE, this.saveOnClose);
        this.showTooltips = document.getBoolean(KEY_SHOW_TOOLTIPS, this.showTooltips);
        // Configs written by builds that shipped a "Themes" destination (and older four-section
        // layouts) must still load: the remembered section folds onto the closest existing one.
        this.openSection = normalizeSection(document.get(KEY_OPEN_SECTION, this.openSection));
        this.invertScroll = document.getBoolean(KEY_INVERT_SCROLL, this.invertScroll);
    }

    public void writeConfig(ConfigDocument.Builder builder) {
        builder.putBoolean(KEY_SAVE_ON_CLOSE, this.saveOnClose);
        builder.putBoolean(KEY_SHOW_TOOLTIPS, this.showTooltips);
        builder.put(KEY_OPEN_SECTION, this.openSection);
        builder.putBoolean(KEY_INVERT_SCROLL, this.invertScroll);
    }

    /**
     * @return the closest known section name; an unknown value falls back to Modules. A value
     *     from an older architecture folds onto its successor first: the retired "Themes"
     *     destination is Appearance now.
     */
    public static String normalizeSection(String value) {
        return GuiSection.fromLabel(migrateSection(value), GuiSection.MODULES).label();
    }

    private static String migrateSection(String value) {
        if (value != null && value.trim().equalsIgnoreCase("Themes")) {
            return "Appearance";
        }
        return value;
    }

    /** @return the index of {@code section} in {@link #SECTIONS}, or 0 when it is unknown. */
    public static int sectionIndex(String section) {
        return GuiSection.fromLabel(section, GuiSection.MODULES).ordinal();
    }
}
