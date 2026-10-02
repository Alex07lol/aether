package dev.aether.ui;

/**
 * The chrome glyph set: single codepoints from Microsoft's Fluent System Icons (MIT),
 * drawn as text through the bundled icon face - the same icon-as-font approach the
 * reference client uses, which keeps every icon vector-crisp at any size with zero
 * texture management. Codepoint values for these constants were selected from the
 * reference client's MIT-licensed Fluent mapping; the licence files ship with the font.
 * <p>
 * Draw with {@code AetherFont.drawIcon(glyph, sizePx, x, y, argb)}; icons are always
 * drawn at text size so they optically match the surrounding label.
 */
public final class UiIcon {

    /** Navigation rail. */
    public static final char HOME = '\uF481';
    public static final char MODULES = '\uF134'; // APPS: the feature grid
    public static final char COSMETICS = '\uF710'; // STAR: the wardrobe
    public static final char APPEARANCE = '\uF593'; // PAINT_BRUSH
    public static final char PROFILES = '\uF5BE'; // PERSON
    public static final char SETTINGS = '\uF6AA';
    public static final char EDIT_HUD = '\uF532'; // MATCH_APP_LAYOUT
    public static final char STAR = '\uF710';

    /** Header and rows. */
    public static final char SEARCH = '\uF690';
    public static final char FOLDER = '\uF419';
    public static final char OPEN_FOLDER = '\uF584';
    public static final char REFRESH = '\uF13E'; // ARROW_CLOCKWISE
    public static final char GEAR = '\uF6AA';
    public static final char CHEVRON_LEFT = '\uF2AB';
    public static final char CHEVRON_RIGHT = '\uF2B1';
    public static final char CHECK = '\uE2F5';
    public static final char ADD = '\uF10A';
    public static final char TRASH = '\uF713';
    public static final char INFO = '\uF4A4';
    public static final char WARNING = '\uF86A';
    public static final char DISMISS = '\uF36A';
    public static final char GAMES = '\uF451';

    /** Type glyphs for cosmetic categories without thumbnails. */
    public static final char CAPE_GLYPH = '\uF710';
    public static final char WINGS_GLYPH = '\uF2C7';
    public static final char HAT_GLYPH = '\uF710';

    /** Every glyph this set uses, for rasterising the icon atlas. */
    public static final char[] CHARSET = {
        HOME, MODULES, COSMETICS, APPEARANCE, PROFILES, SETTINGS, EDIT_HUD,
        SEARCH, FOLDER, OPEN_FOLDER, REFRESH, GEAR, CHEVRON_LEFT, CHEVRON_RIGHT,
        CHECK, ADD, TRASH, INFO, WARNING, DISMISS, GAMES, STAR,
        CAPE_GLYPH, WINGS_GLYPH, HAT_GLYPH,
    };

    private UiIcon() {
    }
}
