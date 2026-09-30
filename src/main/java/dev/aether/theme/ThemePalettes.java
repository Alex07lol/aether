package dev.aether.theme;

/**
 * Palettes for the built-in theme modules.
 * <p>
 * Every palette fills the same five semantic tokens so an adapter can swap a theme
 * without knowing which theme it is: {@code surface} is the window body,
 * {@code surfaceSoft} raised cards and rows, {@code accent} the interactive
 * highlight, {@code text} the primary foreground, and {@code glassHighlight} the
 * 1px inner border used across the Aether screens.
 * <p>
 * Instances are immutable singletons: adapters read a palette every frame, so
 * handing out a fresh object per call would allocate in the render loop.
 */
public final class ThemePalettes {
    /**
     * Translucent black and white - the palette the client ships on, and the one the Leaf screen
     * art is recoloured for. Every surface is nearly black, every accent is the same white as the
     * text, so state is carried by the art (a toggle's knob, a card's brightness) rather than by
     * hue. The other palettes stay selectable for players who want colour.
     */
    private static final ThemePalette MONO = new ThemePalette(
        ColorRgb.of(6, 7, 10),
        ColorRgb.of(26, 27, 34),
        ColorRgb.of(242, 242, 245),
        ColorRgb.of(242, 242, 245),
        ColorRgb.of(255, 255, 255)
    );

    private static final ThemePalette AETHER_BLUE = new ThemePalette(
        ColorRgb.of(11, 18, 32),
        ColorRgb.of(27, 36, 56),
        ColorRgb.of(56, 125, 255),
        ColorRgb.of(235, 242, 255),
        ColorRgb.of(255, 255, 255)
    );

    private static final ThemePalette MIDNIGHT = new ThemePalette(
        ColorRgb.of(7, 7, 14),
        ColorRgb.of(20, 20, 36),
        ColorRgb.of(150, 120, 255),
        ColorRgb.of(230, 226, 255),
        ColorRgb.of(198, 186, 255)
    );

    private static final ThemePalette AURORA = new ThemePalette(
        ColorRgb.of(8, 20, 26),
        ColorRgb.of(18, 38, 42),
        ColorRgb.of(56, 224, 168),
        ColorRgb.of(228, 255, 246),
        ColorRgb.of(180, 255, 232)
    );

    private static final ThemePalette FROST = new ThemePalette(
        ColorRgb.of(16, 24, 36),
        ColorRgb.of(32, 46, 66),
        ColorRgb.of(130, 200, 255),
        ColorRgb.of(240, 248, 255),
        ColorRgb.of(255, 255, 255)
    );

    private static final ThemePalette LIGHT = new ThemePalette(
        ColorRgb.of(244, 248, 253),
        ColorRgb.of(222, 231, 243),
        ColorRgb.of(36, 104, 200),
        ColorRgb.of(26, 34, 48),
        ColorRgb.of(255, 255, 255)
    );

    private ThemePalettes() {
    }

    /** @return the translucent black + white palette, which is also the client default. */
    public static ThemePalette mono() {
        return MONO;
    }

    public static ThemePalette aetherBlue() {
        return AETHER_BLUE;
    }

    public static ThemePalette midnight() {
        return MIDNIGHT;
    }

    public static ThemePalette aurora() {
        return AURORA;
    }

    public static ThemePalette frost() {
        return FROST;
    }

    public static ThemePalette light() {
        return LIGHT;
    }

    /** @return true when the palette surface is bright enough to need dark foreground text. */
    public static boolean isLightSurface(ThemePalette palette) {
        ColorRgb surface = palette.surface();
        int luminance = (surface.red() * 299 + surface.green() * 587 + surface.blue() * 114) / 1000;
        return luminance > 150;
    }
}
