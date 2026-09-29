package dev.aether.gui;

/**
 * The single coordinate system of the Aether GUI.
 * <p>
 * Every screen lays out and hit-tests its controls in one design space: 1080 units
 * tall, with the width following the window's aspect ratio (so a 16:9 window uses a
 * 1920x1080 canvas). The design space sits on top of Minecraft's own scaled
 * resolution, which is what already absorbs GUI scale settings, high-DPI displays and
 * fullscreen/windowed differences; this class adds exactly one conversion on top of
 * it, so no component invents a scale factor of its own.
 * <p>
 * The structure follows the proven idea of Leaf Client's
 * {@code com.leafclient.screen.ScaleFixer} (design units converted once, centrally),
 * rebuilt with double math and an aspect-derived width instead of Leaf's
 * Toolkit-based integer approximation. Adapted portion is GPLv3, (C) Leaf Client
 * contributors - see docs/GUI_REBUILD.md.
 * <p>
 * The values are set once per frame at the top of the screen render pass and only
 * read afterwards, on the render thread.
 */
public final class GuiScale {

    /** Vertical extent of the design canvas. Never changes. */
    public static final int DESIGN_HEIGHT = 1080;

    private static final GuiScale INSTANCE = new GuiScale();

    private double unit = 1.0D;
    private double designWidth = 1920.0D;
    private int guiWidth = 1920;
    private int guiHeight = 1080;

    private GuiScale() {
    }

    /** Recomputes the conversion for the current scaled-resolution frame. */
    public static void update(int scaledWidth, int scaledHeight) {
        if (scaledWidth <= 0 || scaledHeight <= 0) {
            return; // keep the previous values; a zero-sized frame draws nothing anyway
        }
        INSTANCE.guiWidth = scaledWidth;
        INSTANCE.guiHeight = scaledHeight;
        INSTANCE.unit = scaledHeight / (double) DESIGN_HEIGHT;
        INSTANCE.designWidth = scaledWidth / INSTANCE.unit;
    }

    /** GUI pixels per design unit. */
    public static double unit() {
        return INSTANCE.unit;
    }

    /** Width of the design canvas for the current frame, in design units. */
    public static double designWidth() {
        return INSTANCE.designWidth;
    }

    /** Current scaled-resolution width, in GUI pixels. */
    public static int guiWidth() {
        return INSTANCE.guiWidth;
    }

    /** Current scaled-resolution height, in GUI pixels. */
    public static int guiHeight() {
        return INSTANCE.guiHeight;
    }

    /** Converts a design-space X coordinate to GUI pixels. */
    public static int x(double designX) {
        return round(designX * INSTANCE.unit);
    }

    /** Converts a design-space Y coordinate to GUI pixels. */
    public static int y(double designY) {
        return round(designY * INSTANCE.unit);
    }

    /** Converts a design-space width to GUI pixels. */
    public static int w(double designWidthUnits) {
        return round(designWidthUnits * INSTANCE.unit);
    }

    /** Converts a design-space height to GUI pixels. */
    public static int h(double designHeightUnits) {
        return round(designHeightUnits * INSTANCE.unit);
    }

    /** Converts a GUI-pixel mouse X into design units. */
    public static double mouseX(int guiX) {
        return guiX / INSTANCE.unit;
    }

    /** Converts a GUI-pixel mouse Y into design units. */
    public static double mouseY(int guiY) {
        return guiY / INSTANCE.unit;
    }

    /**
     * Converts a font size given in design units into the pixel size a glyph page
     * must be rasterized at so the text matches the layout grid.
     */
    public static int fontPixels(double designSize) {
        return Math.max(9, round(designSize * INSTANCE.unit));
    }

    private static int round(double value) {
        return (int) Math.round(value);
    }
}
