package dev.aether.forge189.ui;

public final class AetherUi {
    public static int SURFACE = 0xFF0B0E14;
    public static int PANEL = 0xEE10141B;
    public static int CARD = 0xFF191E29;
    public static int CARD_HOVER = 0xFF232834;
    public static int SEARCH = 0xFF1F232E;
    public static int SEARCH_FOCUS = 0xFF2A2F3C;
    public static int GLASS = 0xDD0F1620;
    public static int GLASS_SOFT = 0xBB0F1620;
    public static int TOGGLE_BG = 0xFF343A49;
    public static int TRACK = 0xFF2A3143;
    public static int BORDER = 0x22FFFFFF;
    public static int PANEL_EDGE = 0xE6FFFFFF;
    public static int SHADOW = 0x50000000;
    public static int TEXT_PRIMARY = 0xFFEAEFFB;
    public static int TEXT_SECONDARY = 0xFF8A99B5;
    public static int TEXT_DISABLED = 0xFF56637C;
    public static int ACCENT = 0xFF387DFF;
    public static int ACCENT_DARK = 0xFF1F5FD0;
    public static int ACCENT_SOFT = 0x66387DFF;
    public static int ACCENT_ON = 0xFF38E0A8;
    public static int ACCENT_GLOW = 0x55387DFF;
    public static int SCRIM_TOP = 0xF2070B14;
    public static int SCRIM_BOTTOM = 0xF20D1524;
    public static int DECK_BG = 0xE60B1220;
    public static int DECK_EDGE = 0x30FFFFFF;
    public static int ROW_BG = 0x12FFFFFF;
    public static int ROW_HOVER = 0x1EFFFFFF;
    public static int ROW_SELECTED = 0x264C8DFF;
    public static int ROW_ON_TINT = 0x1A38E0A8;
    public static int SKY_TOP = 0xFFBDEFFF;
    public static int SKY_BOTTOM = 0xFFEAF8FF;
    public static int SPARKLE = 0x66FFFFFF;
    public static int STAR = 0xFFFFD166;
    public static int WARN = 0xFFFF7A6B;
    public static boolean LIGHT_SURFACE = false;

    public static void drawScreen(int width, int height) {
        // Would call Mc189Compat.drawRect
    }

    public static void drawRect(int left, int top, int right, int bottom, int color) {
        // Would call Mc189Compat.drawRect
    }

    public static void roundRect(int left, int top, int right, int bottom, int radius, int color) {
        // Would draw rounded rectangle
    }

    public static void outline(int left, int top, int right, int bottom, int color) {
        // Would draw outline
    }

    public static void text(Object font, String text, int x, int y, int color) {
        // Would call Mc189Compat.drawStringWithShadow
    }

    public static void centered(Object font, String text, int x, int y, int width, int color) {
        // Would center and draw text
    }

    public static int width(Object font, String text) {
        // Would return string width
        return 0;
    }

    public static String trim(Object font, String text, int maxWidth) {
        // Would trim text
        return text != null ? text : "";
    }

    public static int withAlpha(int color, int alpha) {
        return ((Math.max(0, Math.min(255, alpha)) & 0xFF) << 24) | (color & 0x00FFFFFF);
    }

    public static int readableOn(int background) {
        int luminance = (red(background) * 299 + green(background) * 587 + blue(background) * 114) / 1000;
        return luminance > 150 ? 0xFF14202E : 0xFFF4F8FF;
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
}