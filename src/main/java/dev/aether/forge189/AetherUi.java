package dev.aether.forge189;

import dev.aether.AetherClient;
import dev.aether.forge189.font.GlyphPageFontRenderer;
import dev.aether.theme.AetherTheme;
import dev.aether.theme.ColorRgb;
import dev.aether.theme.ThemePalette;
import dev.aether.theme.ThemePalettes;

import java.awt.Color;

/**
 * The single theme source of truth for every Aether screen and the HUD.
 * <p>
 * Screens never hard-code a palette colour: they read these tokens. {@link #applyTheme}
 * derives every token from the five semantic values of a {@link ThemePalette}
 * ({@code surface}, {@code surfaceSoft}, {@code accent}, {@code text},
 * {@code glassHighlight}), and {@link #syncTheme} repaints them when the enabled theme
 * module changes. Because a theme is only ever five numbers, adding a theme means adding
 * one palette in core - not touching a dozen screens.
 */
public final class AetherUi {
    /* ── structure ──────────────────────────────────────────────────────── */
    /** Window body - deep charcoal glass. */
    public static int SURFACE = 0xFF08090D;
    /** Translucent panel over the world (menus). */
    public static int PANEL = 0xCC15161B;
    /** Raised card / row. */
    public static int CARD = 0xCC1D1E25;
    public static int CARD_HOVER = 0xD9252630;
    public static int SEARCH = 0xCC1F2028;
    public static int SEARCH_FOCUS = 0xD92A2B36;
    /** Frosted panel fill used by the classic Aether screens. */
    public static int GLASS = 0xCC15161B;
    public static int GLASS_SOFT = 0xAA1D1E25;
    public static int TOGGLE_BG = 0xFF343A49;
    /** Slider / progress trough. */
    public static int TRACK = 0xFF252630;
    public static int BORDER = 0x14FFFFFF;
    public static int PANEL_EDGE = 0x14FFFFFF;
    public static int SHADOW = 0x50000000;

    /* ── text ───────────────────────────────────────────────────────────── */
    public static int TEXT_PRIMARY = 0xFFF2F2F5;
    public static int TEXT_SECONDARY = 0xFFA3A4B3;
    public static int TEXT_DISABLED = 0xFF737585;

    /* ── accent ─────────────────────────────────────────────────────────── */
    /**
     * The interactive highlight. It defaults to the same white as the text because the default
     * palette is translucent black and white (see {@link ThemePalettes#mono()}).
     */
    public static int ACCENT = 0xFFF2F2F5;
    public static int ACCENT_DARK = 0xFFC9CAD4;
    public static int ACCENT_SOFT = 0x66F2F2F5;
    /**
     * Lamp colour for "this module is on". It follows the accent rather than staying a fixed hue:
     * the ported Leaf art carries on/off in its own shape (a toggle's knob, a card's brightness),
     * so a second fixed colour would only fight the palette.
     */
    public static int ACCENT_ON = 0xFFF2F2F5;
    public static int ACCENT_GLOW = 0x55F2F2F5;

    /* ── click deck ─────────────────────────────────────────────────────── */
    public static int SCRIM_TOP = 0xF208090D;
    public static int SCRIM_BOTTOM = 0xF20D0F17;
    public static int DECK_BG = 0xE615161B;
    public static int DECK_EDGE = 0x20FFFFFF;
    public static int ROW_BG = 0x0AFFFFFF;
    public static int ROW_HOVER = 0x14FFFFFF;
    public static int ROW_SELECTED = 0x26FFFFFF;
    public static int ROW_ON_TINT = 0x1AFFFFFF;

    /* ── background gradient (title/menu screens) ───────────────────────── */
    public static int SKY_TOP = 0xFF08090D;
    public static int SKY_BOTTOM = 0xFF0D0F17;
    public static int SPARKLE = 0x33FFFFFF;

    /* ── semantic accents that must stay recognisable across themes ─────── */
    public static int STAR = 0xFFFFD166;
    public static int WARN = 0xFFFF7A6B;

    /** True when the active surface is bright, so the UI can pick dark ink and light edges. */
    public static boolean LIGHT_SURFACE = false;

    private static AetherClient client;
    private static AetherTheme appliedTheme;

    private AetherUi() {
    }

    /**
     * Connects the hub to the client whose theme modules drive it. Called once by the mod
     * loader, and by the headless self-test.
     */
    public static void bind(AetherClient boundClient) {
        client = boundClient;
        appliedTheme = null;
    }

    /* ── theme plumbing ─────────────────────────────────────────────────── */

    /**
     * Repaints every token from {@code theme}. This is the only place that turns a
     * palette into concrete colours, so a screen can never invent one that does not
     * follow the active theme.
     */
    public static void applyTheme(AetherTheme theme) {
        appliedTheme = theme;
        ThemePalette palette = theme.palette();
        int surface = fullAlpha(palette.surface());
        int raised = fullAlpha(palette.surfaceSoft());
        int accent = fullAlpha(palette.accent());
        int text = fullAlpha(palette.text());
        int edge = fullAlpha(palette.glassHighlight());
        boolean light = ThemePalettes.isLightSurface(palette);
        int ink = light ? 0xFF000000 : 0xFFFFFFFF;
        int shade = light ? 0xFFFFFFFF : 0xFF000000;
        // Glass highlights are light by design, so on a light palette they would draw an
        // invisible border. There the ink colour becomes the edge instead.
        int edgeInk = light ? blend(text, surface, 0.35F) : edge;
        LIGHT_SURFACE = light;

        SURFACE = surface;
        PANEL = withAlpha(raised, 0xEE);
        CARD = blend(surface, raised, 0.75F);
        CARD_HOVER = blend(CARD, ink, light ? 0.08F : 0.10F);
        SEARCH = withAlpha(blend(surface, ink, light ? 0.06F : 0.10F), 0xF0);
        SEARCH_FOCUS = withAlpha(blend(surface, accent, 0.18F), 0xF0);
        GLASS = withAlpha(raised, 0xDD);
        GLASS_SOFT = withAlpha(raised, 0xBB);
        TOGGLE_BG = blend(raised, shade, 0.25F);
        TRACK = blend(raised, text, 0.18F);
        BORDER = withAlpha(edgeInk, 0x22);
        PANEL_EDGE = withAlpha(edgeInk, light ? 0xFF : 0xE6);
        SHADOW = withAlpha(light ? blend(surface, 0xFF000000, 0.55F) : 0xFF000000, 0x50);

        TEXT_PRIMARY = text;
        TEXT_SECONDARY = blend(text, surface, 0.42F);
        TEXT_DISABLED = blend(text, surface, 0.66F);

        ACCENT = accent;
        ACCENT_DARK = blend(accent, 0xFF000000, 0.25F);
        ACCENT_SOFT = withAlpha(accent, 0x66);
        ACCENT_GLOW = withAlpha(accent, 0x55);
        ACCENT_ON = accent;

        SCRIM_TOP = withAlpha(blend(surface, shade, 0.22F), 0xF2);
        SCRIM_BOTTOM = withAlpha(blend(surface, shade, 0.08F), 0xF2);
        DECK_BG = withAlpha(raised, 0xE6);
        DECK_EDGE = withAlpha(edgeInk, 0x30);
        ROW_BG = withAlpha(edgeInk, 0x12);
        ROW_HOVER = withAlpha(edgeInk, 0x1E);
        ROW_SELECTED = withAlpha(accent, 0x26);
        ROW_ON_TINT = withAlpha(accent, 0x1A);

        SKY_TOP = blend(surface, accent, light ? 0.26F : 0.30F);
        SKY_BOTTOM = blend(surface, light ? 0xFFFFFFFF : edge, light ? 0.55F : 0.06F);
        SPARKLE = withAlpha(edge, light ? 0x66 : 0x44);

        STAR = light ? 0xFFE8A22B : 0xFFFFD166;
        WARN = light ? 0xFFD6453B : 0xFFFF7A6B;
    }

    /**
     * Applies the bound client's active theme when it changed since the last call. Every
     * screen calls this at the top of its render pass; no screen needs to be handed the
     * client just to stay themed.
     *
     * @return true when the tokens were repainted, so a screen can drop any cached
     *     colours of its own. {@link AetherClient#theme()} hands back a cached instance
     *     while the theme stays the same, so this is an identity compare per frame.
     */
    public static boolean syncTheme() {
        if (client == null) {
            return false;
        }
        AetherTheme theme = client.theme();
        if (theme == appliedTheme) {
            return false;
        }
        applyTheme(theme);
        return true;
    }

    private static int fullAlpha(ColorRgb rgb) {
        return 0xFF000000 | (rgb.red() << 16) | (rgb.green() << 8) | rgb.blue();
    }

    /** Linear ARGB blend: {@code t = 0} returns {@code from}, {@code t = 1} returns {@code to}. */
    public static int blend(int from, int to, float t) {
        float clamped = Math.max(0F, Math.min(1F, t));
        int a = Math.round(((from >> 24) & 0xFF) + (((to >> 24) & 0xFF) - ((from >> 24) & 0xFF)) * clamped);
        int r = Math.round(((from >> 16) & 0xFF) + (((to >> 16) & 0xFF) - ((from >> 16) & 0xFF)) * clamped);
        int g = Math.round(((from >> 8) & 0xFF) + (((to >> 8) & 0xFF) - ((from >> 8) & 0xFF)) * clamped);
        int b = Math.round((from & 0xFF) + ((to & 0xFF) - (from & 0xFF)) * clamped);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    /** Foreground that stays readable on {@code background}: dark ink on light, light ink on dark. */
    public static int readableOn(int background) {
        int luminance = (red(background) * 299 + green(background) * 587 + blue(background) * 114) / 1000;
        return luminance > 150 ? 0xFF14202E : 0xFFF4F8FF;
    }

    public static int argb(int r, int g, int b) {
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    public static int argb(dev.aether.theme.ColorRgb rgb) {
        return 0xFF000000 | (rgb.red() << 16) | (rgb.green() << 8) | rgb.blue();
    }

    private static int red(int color) {
        return (color >> 16) & 0xFF;
    }

    private static int green(int color) {
        return (color >> 8) & 0xFF;
    }

    private static int blue(int color) {
        return color & 0xFF;
    }

    /* ── shared drawing helpers ─────────────────────────────────────────── */

    public static void drawScreen(int width, int height) {
        Mc189Compat.drawRect(0, 0, width, height, SURFACE);
    }

    public static void roundRect(int left, int top, int right, int bottom, int radius, int color) {
        Mc189Compat.drawRect(left + radius, top, right - radius, bottom, color);
        Mc189Compat.drawRect(left, top + radius, right, bottom - radius, color);
        drawCircle(left + radius, top + radius, radius, color);
        drawCircle(right - radius, top + radius, radius, color);
        drawCircle(left + radius, bottom - radius, radius, color);
        drawCircle(right - radius, bottom - radius, radius, color);
    }

    public static void drawRect(int left, int top, int right, int bottom, int color) {
        Mc189Compat.drawRect(left, top, right, bottom, color);
    }

    public static void drawRoundRect(int left, int top, int right, int bottom, int radius, int color) {
        Mc189Compat.drawRect(left + radius, top, right - radius, bottom, color);
        Mc189Compat.drawRect(left, top + radius, right, bottom - radius, color);
        drawCircle(left + radius, top + radius, radius, color);
        drawCircle(right - radius, top + radius, radius, color);
        drawCircle(left + radius, bottom - radius, radius, color);
        drawCircle(right - radius, bottom - radius, radius, color);
    }

    public static void drawCircle(int cx, int cy, int r, int color) {
        // This is a cheap approximation of a circle for UI purposes.
        for (int i = 0; i <= r; i++) {
            int y = (int) Math.round(Math.sqrt(r * r - i * i));
            Mc189Compat.drawRect(cx - i, cy - y, cx - i + 1, cy + y, color);
            Mc189Compat.drawRect(cx + i, cy - y, cx + i + 1, cy + y, color);
        }
    }

    static void drawShadow(int left, int top, int right, int bottom, int radius, int spread) {
        for (int i = 0; i < spread; i++) {
            int alpha = 40 - (i * (40 / spread));
            int shadowColor = (alpha << 24) | (SHADOW & 0x00FFFFFF);
            drawRoundRect(left - i, top - i, right + i, bottom + i, radius + i, shadowColor);
        }
    }

    public static void text(Object font, String text, int x, int y, int color) {
        Mc189Compat.drawString(font, text, x, y, color, false);
    }

    public static void textSmooth(Object font, String text, int x, int y, int color) {
        if (font instanceof GlyphPageFontRenderer) {
            ((GlyphPageFontRenderer) font).drawString(text, x, y, color);
        } else {
            Mc189Compat.drawString(font, text, x, y, color, false);
        }
    }

    public static void centered(Object font, String text, int x, int y, int width, int color) {
        int textX = x + (width - Mc189Compat.stringWidth(font, text)) / 2;
        Mc189Compat.drawStringWithShadow(font, text, textX, y, color);
    }

    public static void centeredSmooth(Object font, String text, int x, int y, int width, int color) {
        int tw = stringWidthSmooth(font, text);
        int textX = x + (width - tw) / 2;
        textSmooth(font, text, textX, y, color);
    }

    /** Get text width from either font type. */
    public static int stringWidthSmooth(Object font, String text) {
        if (font instanceof GlyphPageFontRenderer) {
            return ((GlyphPageFontRenderer) font).getStringWidth(text);
        }
        return Mc189Compat.stringWidth(font, text);
    }

    public static void outline(int left, int top, int right, int bottom, int color) {
        Mc189Compat.drawRect(left, top, right, top + 1, color);
        Mc189Compat.drawRect(left, bottom - 1, right, bottom, color);
        Mc189Compat.drawRect(left, top, left + 1, bottom, color);
        Mc189Compat.drawRect(right - 1, top, right, bottom, color);
    }

    public static void drawSearchGlyph(int x, int y, int color) {
        drawCircle(x + 4, y + 4, 3, color);
        Mc189Compat.drawRect(x + 3, y + 4, x + 5, y + 5, SEARCH); // Clear inside of magnifying glass
        Mc189Compat.drawRect(x + 7, y + 7, x + 9, y + 8, color);
        Mc189Compat.drawRect(x + 8, y + 8, x + 10, y + 9, color);
    }

    public static void drawBadge(String label, int x, int y, int color) {
        int w = Mc189Compat.stringWidth(null, label) + 10;
        roundRect(x, y, x + w, y + 12, 6, withAlpha(color, 0x44));
        text(null, label, x + 5, y + 1, color);
    }

    public static void drawStar(int x, int y, int color) {
        Mc189Compat.drawRect(x + 4, y, x + 5, y + 8, color);
        Mc189Compat.drawRect(x, y + 2, x + 9, y + 4, color);
        Mc189Compat.drawRect(x + 1, y + 4, x + 8, y + 6, color);
        Mc189Compat.drawRect(x + 2, y + 6, x + 7, y + 8, color);
    }

    public static void drawChevron(int x, int y, int direction, int color) {
        for (int i = 0; i < 5; i++) {
            int dx = direction > 0 ? i : -i;
            Mc189Compat.drawRect(x + dx, y + i, x + dx + 1, y + i + 1, color);
            Mc189Compat.drawRect(x + dx, y - i, x + dx + 1, y - i + 1, color);
        }
    }

    public static void drawMark(int x, int y, int color) {
        Mc189Compat.drawRect(x, y + 3, x + 10, y + 7, color);
        Mc189Compat.drawRect(x + 2, y + 1, x + 8, y + 9, color);
        Mc189Compat.drawRect(x + 4, y, x + 6, y + 10, color);
    }

    public static void drawSearchIcon(int x, int y, int color) {
        // Magnifying glass
        drawCircle(x + 4, y + 4, 3, color);
        Mc189Compat.drawRect(x + 3, y + 4, x + 5, y + 5, SEARCH); // Clear inside of magnifying glass
        Mc189Compat.drawRect(x + 7, y + 7, x + 9, y + 8, color);
        Mc189Compat.drawRect(x + 8, y + 8, x + 10, y + 9, color);
    }

    public static void drawHomeIcon(int x, int y, int color) {
        Mc189Compat.drawRect(x + 2, y + 5, x + 10, y + 11, color); // base
        Mc189Compat.drawRect(x + 1, y + 4, x + 11, y + 5, color); // roof slant
        Mc189Compat.drawRect(x + 5, y + 1, x + 7, y + 4, color); // roof top
        Mc189Compat.drawRect(x + 4, y + 8, x + 8, y + 11, PANEL); // door
    }

    public static void drawAccountIcon(int x, int y, int color) {
        // Simple person icon
        drawCircle(x + 6, y + 4, 2, color); // head
        Mc189Compat.drawRect(x + 2, y + 7, x + 10, y + 8, color); // shoulders
        Mc189Compat.drawRect(x + 4, y + 8, x + 8, y + 11, color); // body
    }

    public static void drawEditIcon(int x, int y, int color) {
        // Simple pencil icon
        Mc189Compat.drawRect(x + 4, y + 2, x + 8, y + 9, color); // body
        Mc189Compat.drawRect(x + 3, y + 9, x + 9, y + 10, color); // tip base
        Mc189Compat.drawRect(x + 5, y + 10, x + 7, y + 11, color); // tip
        Mc189Compat.drawRect(x + 4, y + 1, x + 8, y + 2, color); // eraser
    }

    public static void drawHudIcon(int x, int y, int color) {
        Mc189Compat.drawRect(x + 2, y + 2, x + 10, y + 3, color);
        Mc189Compat.drawRect(x + 2, y + 9, x + 10, y + 10, color);
        Mc189Compat.drawRect(x + 2, y + 2, x + 3, y + 10, color);
        Mc189Compat.drawRect(x + 9, y + 2, x + 10, y + 10, color);
        Mc189Compat.drawRect(x + 4, y + 4, x + 8, y + 5, color);
    }

    public static void drawGameplayIcon(int x, int y, int color) {
        Mc189Compat.drawRect(x + 5, y + 2, x + 7, y + 10, color); // sword hilt
        Mc189Compat.drawRect(x + 3, y + 4, x + 9, y + 5, color); // sword guard
        Mc189Compat.drawRect(x + 6, y + 1, x + 7, y + 2, color); // sword tip
    }

    public static void drawRenderIcon(int x, int y, int color) {
        // eye
        drawCircle(x + 6, y + 6, 4, color);
        drawCircle(x + 6, y + 6, 3, PANEL);
        drawCircle(x + 6, y + 6, 1, color);
    }

    public static void drawPerformanceIcon(int x, int y, int color) {
        // bolt
        Mc189Compat.drawRect(x + 5, y + 1, x + 7, y + 4, color);
        Mc189Compat.drawRect(x + 4, y + 4, x + 8, y + 7, color);
        Mc189Compat.drawRect(x + 5, y + 7, x + 7, y + 11, color);
    }

    public static void drawCosmeticsIcon(int x, int y, int color) {
        // shirt
        Mc189Compat.drawRect(x + 2, y + 2, x + 10, y + 10, color);
        Mc189Compat.drawRect(x + 4, y, x + 8, y + 2, color);
        Mc189Compat.drawRect(x, y + 2, x + 2, y + 5, color);
        Mc189Compat.drawRect(x + 10, y + 2, x + 12, y + 5, color);
    }

    public static void drawClientIcon(int x, int y, int color) {
        // gear
        drawCircle(x + 6, y + 6, 4, color);
        drawCircle(x + 6, y + 6, 2, PANEL);
        for (int i = 0; i < 4; i++) {
            Mc189Compat.drawRect(x + 5, y - 1 + i * 4, x + 7, y + 1 + i * 4, color);
            Mc189Compat.drawRect(x - 1 + i * 4, y + 5, x + 1 + i * 4, y + 7, color);
        }
    }

    static float lerp(float a, float b, float t) {
        return a + t * (b - a);
    }

    public static int lerpColor(int from, int to, float t) {
        Color fromColor = new Color(from, true);
        Color toColor = new Color(to, true);
        int r = (int) lerp(fromColor.getRed(), toColor.getRed(), t);
        int g = (int) lerp(fromColor.getGreen(), toColor.getGreen(), t);
        int b = (int) lerp(fromColor.getBlue(), toColor.getBlue(), t);
        int a = (int) lerp(fromColor.getAlpha(), toColor.getAlpha(), t);
        return new Color(r, g, b, a).getRGB();
    }

    /* ── classic screens (frosted panels) ───────────────────────────────── */

    static void background(int width, int height, long time) {
        Mc189Compat.drawRect(0, 0, width, height, SKY_TOP);
        Mc189Compat.drawRect(0, height / 2, width, height, SKY_BOTTOM);
        for (int i = 0; i < 18; i++) {
            int x = (int) ((i * 73L + time / 45L) % Math.max(1, width + 40)) - 20;
            int y = 18 + (i * 29) % Math.max(1, height - 36);
            int size = 2 + (i % 3);
            Mc189Compat.drawRect(x, y, x + size, y + size, SPARKLE);
        }
    }

    public static void panel(int left, int top, int right, int bottom) {
        panel(left, top, right, bottom, false);
    }

    public static void panel(int left, int top, int right, int bottom, boolean hover) {
        Mc189Compat.drawRect(left + 2, top + 2, right + 2, bottom + 2, hover ? withAlpha(ACCENT, 0x77) : SHADOW);
        Mc189Compat.drawRect(left, top, right, bottom, hover ? withAlpha(GLASS, 0xFF) : GLASS);
        Mc189Compat.drawRect(left, top, right, top + 2, hover ? PANEL_EDGE : withAlpha(PANEL_EDGE, 0xFF));
        Mc189Compat.drawRect(left, bottom - 1, right, bottom, withAlpha(ACCENT, 0x77));
    }

    static void button(Object font, AetherButton button, int mouseX, int mouseY) {
        boolean hover = button.contains(mouseX, mouseY);
        int fill = hover ? withAlpha(ACCENT, 0xEE) : withAlpha(GLASS_SOFT, 0xCC);
        int text = hover ? readableOn(ACCENT) : TEXT_PRIMARY;
        Mc189Compat.drawRect(button.x(), button.y(), button.x() + button.width(), button.y() + button.height(), fill);
        Mc189Compat.drawRect(button.x(), button.y(), button.x() + 2, button.y() + button.height(), ACCENT);
        centered(font, button.label(), button.x(), button.y() + 7, button.width(), text);
    }

    static void iconButton(Object font, AetherButton button, String icon, int mouseX, int mouseY) {
        boolean hover = button.contains(mouseX, mouseY);
        Mc189Compat.drawRect(button.x(), button.y(), button.x() + button.width(), button.y() + button.height(), hover ? withAlpha(ACCENT, 0xEE) : withAlpha(GLASS_SOFT, 0xBB));
        Mc189Compat.drawRect(button.x(), button.y(), button.x() + 2, button.y() + button.height(), ACCENT);
        centered(font, icon, button.x(), button.y() + button.height() / 2 - 3, button.width(), hover ? readableOn(ACCENT) : TEXT_PRIMARY);
    }

    static void tooltip(Object font, String text, int mouseX, int mouseY) {
        int width = Mc189Compat.stringWidth(font, text) + 12;
        int left = mouseX + 10;
        int top = mouseY + 10;
        Mc189Compat.drawRect(left + 2, top + 2, left + width + 2, top + 20, SHADOW);
        Mc189Compat.drawRect(left, top, left + width, top + 18, withAlpha(GLASS, 0xFF));
        Mc189Compat.drawRect(left, top, left + 2, top + 18, ACCENT);
        text(font, text, left + 6, top + 6, TEXT_PRIMARY);
    }

    static void avatar(Object font, int x, int y, String username) {
        Mc189Compat.drawRect(x, y, x + 24, y + 24, withAlpha(ACCENT, 0xCC));
        Mc189Compat.drawRect(x + 3, y + 3, x + 21, y + 21, withAlpha(GLASS, 0xFF));
        Mc189Compat.drawRect(x + 6, y + 8, x + 10, y + 12, ACCENT_DARK);
        Mc189Compat.drawRect(x + 14, y + 8, x + 18, y + 12, ACCENT_DARK);
        Mc189Compat.drawRect(x + 8, y + 16, x + 16, y + 18, withAlpha(TEXT_SECONDARY, 0x99));
        String initial = username == null || username.length() == 0 ? "A" : username.substring(0, 1).toUpperCase();
        centered(font, initial, x, y + 7, 24, ACCENT_DARK);
    }

    static void cloud(int x, int y, int width, int color) {
        int height = Math.max(14, width / 5);
        Mc189Compat.drawRect(x, y + height / 2, x + width, y + height, color);
        Mc189Compat.drawRect(x + width / 8, y + height / 4, x + width / 3, y + height, color);
        Mc189Compat.drawRect(x + width / 3, y, x + width * 2 / 3, y + height, color);
        Mc189Compat.drawRect(x + width * 3 / 5, y + height / 4, x + width * 7 / 8, y + height, color);
    }

    static void island(int centerX, int centerY, int width) {
        int left = centerX - width / 2;
        int right = centerX + width / 2;
        int foliage = LIGHT_SURFACE ? 0xCC9FD8B4 : 0xCC5C8F74;
        int grass = LIGHT_SURFACE ? 0xDD6BA06B : 0xDD416B4A;
        int dirt = LIGHT_SURFACE ? 0xDD7A5C42 : 0xDD503C2C;
        int stone = LIGHT_SURFACE ? 0xAA4E4038 : 0xAA3A3029;
        Mc189Compat.drawRect(left + width / 8, centerY, right - width / 8, centerY + width / 12, foliage);
        Mc189Compat.drawRect(left, centerY + width / 12, right, centerY + width / 6, grass);
        Mc189Compat.drawRect(left + width / 5, centerY + width / 6, right - width / 5, centerY + width / 4, dirt);
        Mc189Compat.drawRect(centerX - width / 8, centerY + width / 4, centerX + width / 9, centerY + width / 2, stone);
    }

    public static int withAlpha(int color, int alpha) {
        return ((Math.max(0, Math.min(255, alpha)) & 0xFF) << 24) | (color & 0x00FFFFFF);
    }

    static String trim(Object font, String text, int maxWidth) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        if (Mc189Compat.stringWidth(font, text) <= maxWidth) {
            return text;
        }
        String suffix = "...";
        int suffixWidth = Mc189Compat.stringWidth(font, suffix);
        if (maxWidth < suffixWidth) {
            return "";
        }
        for (int i = text.length() - 1; i >= 0; i--) {
            String sub = text.substring(0, i);
            if (Mc189Compat.stringWidth(font, sub) + suffixWidth <= maxWidth) {
                return sub + suffix;
            }
        }
        return suffix;
    }
}

final class AetherButton {
    private final String label;
    private final int x;
    private final int y;
    private final int width;
    private final int height;
    private final ScreenAction action;

    AetherButton(String label, int x, int y, int width, int height, ScreenAction action) {
        this.label = label;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.action = action;
    }

    String label() {
        return label;
    }

    int x() {
        return x;
    }

    int y() {
        return y;
    }

    int width() {
        return width;
    }

    int height() {
        return height;
    }

    boolean contains(int mouseX, int mouseY) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }

    void click() {
        action.run();
    }
}

interface ScreenAction {
    void run();
}
