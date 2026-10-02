package dev.aether.gui;

import dev.aether.forge189.Mc189Compat;
import dev.aether.forge189.font.AetherFontManager;
import dev.aether.forge189.font.AetherFontManager.Face;
import dev.aether.ui.UiIcon;
import dev.aether.forge189.font.GlyphPageFontRenderer;

/**
 * The UI's text layer: pixel sizes over the bundled Inter faces (SIL OFL), rasterised
 * through the existing glyph-atlas pipeline. Sizes are GUI-scale pixels and follow the
 * compact scale the Glide reference measures out - 7 for slider values, 7.5-8 for
 * descriptions, 9 for chips and fields, 10 for card titles, 11 for section headers,
 * 12.5-13 for row titles, 15 for the screen header - so the whole interface can be
 * specified in the same numbers the visual grammar uses.
 * <p>
 * The glyph atlas is rasterised at twice the wanted pixel size and drawn through the
 * renderer's half-scale matrix, which keeps small Inter sizes crisp. Every call falls
 * back to the vanilla font renderer when the bundled faces are unavailable, so a font
 * problem degrades readability, never the UI.
 */
public final class AetherFont {

    private AetherFont() {
    }

    /* ── renderer selection ─────────────────────────────────────────────── */

    private static GlyphPageFontRenderer renderer(float sizePx, Face face) {
        int fontPt = Math.max(10, Math.round(sizePx * 2.0F));
        GlyphPageFontRenderer bundled = face == Face.ICON
            ? AetherFontManager.instance().sized(face, fontPt, UiIcon.CHARSET)
            : AetherFontManager.instance().sized(face, fontPt);
        if (bundled != null) {
            return bundled;
        }
        return AetherFontManager.instance().sized(fontPt);
    }

    private static GlyphPageFontRenderer renderer(float sizePx) {
        return renderer(sizePx, Face.REGULAR);
    }

    private static Object fallback() {
        return Mc189Compat.fontRenderer(Mc189Compat.minecraft());
    }

    /* ── measurement (GUI pixels) ───────────────────────────────────────── */

    public static int width(float sizePx, Face face, String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        GlyphPageFontRenderer r = renderer(sizePx, face);
        if (r != null) {
            return r.getStringWidth(text);
        }
        return Mc189Compat.stringWidth(fallback(), text);
    }

    public static int width(float sizePx, String text) {
        return width(sizePx, Face.REGULAR, text);
    }

    /** Line height that comfortably fits the rasterised glyphs of this size. */
    public static int height(float sizePx) {
        return Math.max(8, Math.round(sizePx + 2.0F));
    }

    /** Trims {@code text} with an ellipsis until it fits {@code maxWidth} GUI pixels. */
    public static String trim(float sizePx, String text, int maxWidth) {
        return trim(sizePx, Face.REGULAR, text, maxWidth);
    }

    public static String trim(float sizePx, String text, float maxWidth) {
        return trim(sizePx, Face.REGULAR, text, (int) maxWidth);
    }

    public static String trim(float sizePx, Face face, String text, float maxWidth) {
        return trim(sizePx, face, text, (int) maxWidth);
    }

    public static String trim(float sizePx, Face face, String text, int maxWidth) {
        if (text == null) {
            return "";
        }
        if (width(sizePx, face, text) <= maxWidth) {
            return text;
        }
        String ellipsis = "..";
        while (text.length() > 1 && width(sizePx, face, text.substring(0, text.length() - 1) + ellipsis) > maxWidth) {
            text = text.substring(0, text.length() - 1);
        }
        return text + ellipsis;
    }

    /* ── drawing (GUI pixel coordinates) ────────────────────────────────── */

    public static void draw(float sizePx, Face face, String text, float x, float y, int argb) {
        if (text == null || text.isEmpty()) {
            return;
        }
        GlyphPageFontRenderer r = renderer(sizePx, face);
        if (r != null) {
            r.drawString(text, x, y, argb);
        } else {
            Mc189Compat.drawString(fallback(), text, x, y, argb, false);
        }
    }

    public static void draw(float sizePx, String text, float x, float y, int argb) {
        draw(sizePx, Face.REGULAR, text, x, y, argb);
    }

    public static void drawCentered(float sizePx, Face face, String text, float x, float y, float width, int argb) {
        draw(sizePx, face, text, x + (width - width(sizePx, face, text)) / 2.0F, y, argb);
    }

    public static void drawCentered(float sizePx, String text, float x, float y, float width, int argb) {
        drawCentered(sizePx, Face.REGULAR, text, x, y, width, argb);
    }

    public static void drawRight(float sizePx, Face face, String text, float rightX, float y, int argb) {
        draw(sizePx, face, text, rightX - width(sizePx, face, text), y, argb);
    }

    /** Draws the icon glyph for a codepoint from {@link dev.aether.ui.UiIcon}. */
    public static void drawIcon(char glyph, float sizePx, float x, float y, int argb) {
        draw(sizePx, Face.ICON, String.valueOf(glyph), x, y, argb);
    }

    public static int iconWidth(char glyph, float sizePx) {
        return width(sizePx, Face.ICON, String.valueOf(glyph));
    }
}
