package dev.aether.gui.leaf;

import dev.aether.forge189.Mc189Compat;

/**
 * The recoloured Leaf Client art, and the draw calls the screens use it through.
 * <p>
 * Every texture under {@code assets/aether/leaf/} is a Leaf Client 1.8.9 original (GPLv3, see
 * {@code NOTICE.txt} in that folder) with its colours replaced by Aether's palette: the shape and
 * the panel detail live in the texture's own alpha, the surfaces are translucent black and the
 * detail is white. That is why a state is a tint here rather than a different drawing code - the
 * art is authored once and multiplied.
 * <p>
 * Leaf's own rendering is the reference for every geometry constant in this package: a tile is
 * {@code drawModalRectWithCustomSizedTexture(x, y, 0, 0, w, h, w, h)} of one texture, i.e. the
 * source image is stretched into the design rectangle, which is what {@link #draw} does.
 */
public final class LeafArt {

    /* ── screens ────────────────────────────────────────────────────────── */

    /** Fullscreen backdrop of the module and cosmetics screens (Leaf's {@code main.png}). */
    public static final String BACKDROP_MAIN = "leaf/main.png";
    /** Fullscreen backdrop of the settings screens (Leaf's {@code main_mod.png}). */
    public static final String BACKDROP_SETTINGS = "leaf/main_mod.png";

    /* ── surfaces ───────────────────────────────────────────────────────── */

    /** The 170x182 module card. */
    public static final String CARD = "leaf/mod.png";
    /** The 50x50 settings gear drawn inside the card's lower half. */
    public static final String GEAR = "leaf/gear_small.png";
    /** The 300x90 selectable pill (Leaf's {@code select.png}). */
    public static final String SELECT = "leaf/select.png";

    /* ── state tiles ────────────────────────────────────────────────────── */

    public static final String TOGGLE_ON = "leaf/true.png";
    public static final String TOGGLE_OFF = "leaf/false.png";

    /* ── sliders and bars ───────────────────────────────────────────────── */

    public static final String BAR_TRACK = "leaf/bar_main.png";
    public static final String BAR_KNOB = "leaf/bar_point.png";
    public static final String SCROLL_TRACK = "leaf/scroll_main.png";
    public static final String SCROLL_THUMB = "leaf/scroll_bar.png";

    /* ── fields ─────────────────────────────────────────────────────────── */

    /** Leaf's 700x117 text field; the screens stretch it to their row size. */
    public static final String FIELD = "leaf/field/search.png";

    /* ── navigation tiles ───────────────────────────────────────────────── */

    public static final String NAV_MODULES = "leaf/button/mod.png";
    public static final String NAV_COSMETICS = "leaf/button/cosmetic.png";
    public static final String NAV_HUD = "leaf/button/location.png";
    /** Aether's Appearance destination: Leaf's system.png tab recoloured like the rest of the set. */
    public static final String NAV_APPEARANCE = "leaf/button/appearance.png";
    /** Aether's Profiles destination: Leaf's home tab recoloured, reading as "your things". */
    public static final String NAV_PROFILES = "leaf/button/profiles.png";
    public static final String NAV_SETTINGS = "leaf/button/setting.png";
    public static final String HOME = "leaf/button/home.png";
    public static final String CLOSE = "leaf/button/close.png";
    public static final String ARROW_LEFT = "leaf/button/arrow_left.png";
    public static final String ARROW_RIGHT = "leaf/button/arrow_right.png";

    /* ── state tints ────────────────────────────────────────────────────── */

    /** Full-brightness art: an enabled module, a selected entry. */
    public static final float BRIGHT = 1.00F;
    /** An enabled row that is not the one the cursor is on. */
    public static final float NORMAL = 0.86F;
    /** A disabled module or a row that is off: the white detail dims, the glass stays. */
    public static final float DIMMED = 0.42F;
    /** Hover highlight: the same art redrawn on top, which brightens the glass without a body. */
    public static final float HOVER = 0.30F;

    private LeafArt() {
    }

    /* ── draw calls ─────────────────────────────────────────────────────── */

    /** Draws the art at full brightness. */
    public static void draw(String path, int x, int y, int width, int height) {
        draw(path, x, y, width, height, BRIGHT);
    }

    /** Draws the art multiplied by {@code brightness} (0-1) in every channel. */
    public static void draw(String path, int x, int y, int width, int height, float brightness) {
        draw(path, x, y, width, height, brightness, 1.0F);
    }

    /** Draws the art multiplied by a brightness and an opacity. */
    public static void draw(String path, int x, int y, int width, int height, float brightness, float alpha) {
        float value = Math.max(0.0F, Math.min(1.0F, brightness));
        Mc189Compat.drawTextureTinted(path, x, y, width, height, value, value, value, alpha);
    }

    /**
     * The hover treatment: the same art drawn a second time on top of itself at low alpha.
     * <p>
     * Leaf tinted its whole texture with a bright colour on hover, which it could do because its
     * panels were opaque; here the surfaces are translucent, so a tint would only lighten the white
     * detail. Redrawing the shape instead brightens the glass itself, so a hovered tile reads as
     * fogged rather than recoloured.
     */
    public static void drawHovered(String path, int x, int y, int width, int height, float brightness) {
        draw(path, x, y, width, height, brightness);
        draw(path, x, y, width, height, 1.0F, HOVER);
    }
}
