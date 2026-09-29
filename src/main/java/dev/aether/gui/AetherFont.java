package dev.aether.gui;

import dev.aether.forge189.Mc189Compat;
import dev.aether.forge189.font.AetherFontManager;
import dev.aether.forge189.font.GlyphPageFontRenderer;

/**
 * The semantic type scale of the Aether GUI - the equivalent of Leaf Client's
 * {@code CustomFont} hub (one renderer, sizes set centrally instead of per screen).
 * Adapted in structure only; all rendering is Aether's own glyph atlas pipeline.
 * <p>
 * Five sizes carry the whole interface: {@link Size#TITLE}, {@link Size#SECTION},
 * {@link Size#BODY}, {@link Size#SMALL} and {@link Size#CAPTION}. Each is defined in
 * {@link GuiScale} design units; the concrete point size is derived from the current
 * window scale, so text is laid out and measured in design units exactly like every
 * other component, and the rasterised size follows the resolution instead of being
 * hardcoded per screen.
 * <p>
 * All coordinates passed to the draw methods are GUI pixels (convert design units with
 * {@link GuiScale}); widths returned by {@link #width} are GUI pixels too. When the
 * custom font cannot be built, every call transparently falls back to Minecraft's font
 * renderer, so the GUI stays readable even with a broken font pipeline.
 */
public final class AetherFont {

    /** The five semantic sizes. Values are heights in design units. */
    public enum Size {
        TITLE(30),
        SECTION(24),
        BODY(20),
        SMALL(17),
        CAPTION(15);

        final int designUnits;

        Size(int designUnits) {
            this.designUnits = designUnits;
        }
    }

    private AetherFont() {
    }

    /* ── renderer selection ─────────────────────────────────────────────── */

    private static GlyphPageFontRenderer renderer(Size size) {
        // The atlas is drawn through a 0.5 matrix, so a glyph of N points lands at N/2
        // pixels - the point size asked for is twice the wanted effective height.
        int fontPt = (int) Math.round(size.designUnits * 2.0D * GuiScale.unit());
        return AetherFontManager.instance().sized(fontPt);
    }

    private static Object fallback() {
        Object minecraft = Mc189Compat.minecraft();
        return Mc189Compat.fontRenderer(minecraft);
    }

    /* ── measurement (GUI pixels) ───────────────────────────────────────── */

    public static int width(Size size, String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        GlyphPageFontRenderer glyphRenderer = renderer(size);
        if (glyphRenderer != null) {
            return glyphRenderer.getStringWidth(text);
        }
        return Mc189Compat.stringWidth(fallback(), text);
    }

    public static int widthOf(Size size, String text, double maxDesignUnits) {
        int maxWidth = GuiScale.w(maxDesignUnits);
        String fit = text;
        while (fit.length() > 1 && width(size, fit) > maxWidth) {
            fit = fit.substring(0, fit.length() - 1);
        }
        return width(size, fit);
    }

    /** Effective line height in GUI pixels, matching the rasterised glyph size. */
    public static int height(Size size) {
        int effective = (int) Math.round(size.designUnits * GuiScale.unit());
        return Math.max(8, effective + 2);
    }

    /* ── drawing (GUI pixel coordinates) ────────────────────────────────── */

    public static void draw(Size size, String text, int x, int y, int color) {
        if (text == null || text.isEmpty()) {
            return;
        }
        GlyphPageFontRenderer glyphRenderer = renderer(size);
        if (glyphRenderer != null) {
            glyphRenderer.drawString(text, x, y, color);
        } else {
            Mc189Compat.drawString(fallback(), text, x, y, color, false);
        }
    }

    public static void drawShadowed(Size size, String text, int x, int y, int color) {
        if (text == null || text.isEmpty()) {
            return;
        }
        GlyphPageFontRenderer glyphRenderer = renderer(size);
        if (glyphRenderer != null) {
            glyphRenderer.drawStringWithShadow(text, x, y, color);
        } else {
            Mc189Compat.drawString(fallback(), text, x, y, color, true);
        }
    }

    public static void drawCentered(Size size, String text, int x, int y, int width, int color) {
        draw(size, text, x + (width - width(size, text)) / 2, y, color);
    }

    public static void drawCenteredShadowed(Size size, String text, int x, int y, int width, int color) {
        drawShadowed(size, text, x + (width - width(size, text)) / 2, y, color);
    }

    public static void drawRight(Size size, String text, int rightX, int y, int color) {
        draw(size, text, rightX - width(size, text), y, color);
    }

    /** Trims {@code text} with an ellipsis until it fits {@code maxWidth} GUI pixels. */
    public static String trimTo(Size size, String text, int maxWidth) {
        if (text == null || width(size, text) <= maxWidth) {
            return text == null ? "" : text;
        }
        String ellipsis = "..";
        while (text.length() > 1 && width(size, text.substring(0, text.length() - 1) + ellipsis) > maxWidth) {
            text = text.substring(0, text.length() - 1);
        }
        return text + ellipsis;
    }
}
