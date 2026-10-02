package dev.aether.ui;

import dev.aether.theme.ColorRgb;
import dev.aether.theme.ThemePalette;

/**
 * The menu's colour tokens, resolved from the worn {@link ThemePalette} each frame.
 * <p>
 * The mapping follows the reference grammar: the window sheet is the palette's
 * {@code surface}, cards/rows/inputs/the rail are {@code surfaceSoft}, titles use
 * {@code text} at full strength and descriptions blend it towards the surface, and
 * every interactive highlight is {@code accent}. Tokens are plain ARGB ints so the
 * vector layer and the font layer share one colour language.
 */
public final class UiTheme {

    private static int sheet = 0xFF131314;
    private static int card = 0xFF222327;
    private static int accent = 0xFF11FFBD;
    private static int text = 0xFFFFFFFF;
    private static int textSoft = blend(text, sheet, 0.45F);
    private static int textFaint = blend(text, sheet, 0.62F);
    private static int edge = 0x1EFFFFFF;

    private UiTheme() {
    }

    /** Re-resolves every token from the worn palette. The screen calls this per frame. */
    public static void apply(ThemePalette palette) {
        if (palette == null) {
            return;
        }
        sheet = argb(palette.surface());
        card = argb(palette.surfaceSoft());
        accent = argb(palette.accent());
        text = argb(palette.text());
        boolean light = luminance(sheet) > 140;
        textSoft = blend(text, sheet, 0.24F);
        textFaint = blend(text, sheet, 0.42F);
        edge = light ? 0x26000000 : 0x1EFFFFFF;
    }

    public static int sheet() {
        return sheet;
    }

    public static int card() {
        return card;
    }

    /** A raised variant of the card surface for hover and pressed states. */
    public static int cardHover() {
        return blend(card, text, luminance(sheet) > 140 ? 0.05F : 0.08F);
    }

    public static int accent() {
        return accent;
    }

    /** The darker end of the accent gradient pair. */
    public static int accentDeep() {
        return blend(accent, sheet, 0.45F);
    }

    public static int text() {
        return text;
    }

    public static int textSoft() {
        return textSoft;
    }

    public static int textFaint() {
        return textFaint;
    }

    public static int edge() {
        return edge;
    }

    /** Foreground that stays readable on the accent: dark ink on bright, light on dark. */
    public static int readableOn(int background) {
        int r = (background >> 16) & 0xFF;
        int g = (background >> 8) & 0xFF;
        int b = background & 0xFF;
        return (r * 299 + g * 587 + b * 114) / 1000 > 150 ? 0xFF14202E : 0xFFF4F8FF;
    }

    public static int withAlpha(int argb, int alpha) {
        return ((Math.max(0, Math.min(255, alpha)) & 0xFF) << 24) | (argb & 0xFFFFFF);
    }

    public static int blend(int from, int to, float t) {
        t = Math.max(0.0F, Math.min(1.0F, t));
        int a = Math.round(((from >> 24) & 0xFF) + (((to >> 24) & 0xFF) - ((from >> 24) & 0xFF)) * t);
        int r = Math.round(((from >> 16) & 0xFF) + (((to >> 16) & 0xFF) - ((from >> 16) & 0xFF)) * t);
        int g = Math.round(((from >> 8) & 0xFF) + (((to >> 8) & 0xFF) - ((from >> 8) & 0xFF)) * t);
        int b = Math.round((from & 0xFF) + ((to & 0xFF) - (from & 0xFF)) * t);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private static int argb(ColorRgb rgb) {
        return 0xFF000000 | (rgb.red() << 16) | (rgb.green() << 8) | rgb.blue();
    }

    private static int luminance(int argb) {
        int r = (argb >> 16) & 0xFF;
        int g = (argb >> 8) & 0xFF;
        int b = argb & 0xFF;
        return (r * 299 + g * 587 + b * 114) / 1000;
    }
}
