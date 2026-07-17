package dev.aether.forge189;

import java.awt.Color;

final class AetherUi {
    static final int COLOR_BACKGROUND = 0xFF0B0E14;
    static final int COLOR_PANEL = 0xEE10141B;
    static final int COLOR_CARD = 0xFF191E29;
    static final int COLOR_CARD_HOVER = 0xFF232834;
    static final int COLOR_SEARCH = 0xFF1F232E;
    static final int COLOR_SEARCH_FOCUS = 0xFF2A2F3C;

    static final int COLOR_TEXT_PRIMARY = 0xFFEAEFFB;
    static final int COLOR_TEXT_SECONDARY = 0xFF8A99B5;
    static final int COLOR_TEXT_DISABLED = 0xFF56637C;

    static final int MODERN_UI_ACCENT = 0xFF387DFF;
    static final int COLOR_ACCENT_GLOW = 0x55387DFF;
    static final int COLOR_BORDER = 0x22FFFFFF;
    static final int MODERN_UI_SHADOW = 0x50000000;
    static final int COLOR_TOGGLE_BG = 0xFF343A49;

    // Old constants for legacy screens
    static final int TEXT = 0xF5FBFF;
    static final int TEXT_DARK = 0x164A70;
    static final int TEXT_MUTED = 0x6C8795;
    static final int ACCENT = 0x52BEEB;
    static final int ACCENT_DARK = 0x1B8FB8;
    static final int GLASS = 0xDDF5FBFF;
    static final int GLASS_SOFT = 0xBBEAF8FF;
    static final int SHADOW = 0x660A2234;

    private AetherUi() {
    }

    // --- New methods for modern UI ---

    static void drawScreen(int width, int height) {
        Mc189Compat.drawRect(0, 0, width, height, COLOR_BACKGROUND);
    }

    static void drawRoundRect(int left, int top, int right, int bottom, int radius, int color) {
        Mc189Compat.drawRect(left + radius, top, right - radius, bottom, color);
        Mc189Compat.drawRect(left, top + radius, right, bottom - radius, color);
        drawCircle(left + radius, top + radius, radius, color);
        drawCircle(right - radius, top + radius, radius, color);
        drawCircle(left + radius, bottom - radius, radius, color);
        drawCircle(right - radius, bottom - radius, radius, color);
    }

    static void drawCircle(int cx, int cy, int r, int color) {
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
            int shadowColor = (alpha << 24) | (MODERN_UI_SHADOW & 0x00FFFFFF);
            drawRoundRect(left - i, top - i, right + i, bottom + i, radius + i, shadowColor);
        }
    }

    static void text(Object font, String text, int x, int y, int color) {
        Mc189Compat.drawStringWithShadow(font, text, x, y, color);
    }

    static void centered(Object font, String text, int x, int y, int width, int color) {
        int textX = x + (width - Mc189Compat.stringWidth(font, text)) / 2;
        text(font, text, textX, y, color);
    }

    static void drawSearchIcon(int x, int y, int color) {
        // Magnifying glass
        drawCircle(x + 4, y + 4, 3, color);
        Mc189Compat.drawRect(x + 3, y + 4, x + 5, y + 5, COLOR_SEARCH); // Clear inside of magnifying glass
        Mc189Compat.drawRect(x + 7, y + 7, x + 9, y + 8, color);
        Mc189Compat.drawRect(x + 8, y + 8, x + 10, y + 9, color);
    }

    static void drawHomeIcon(int x, int y, int color) {
        Mc189Compat.drawRect(x + 2, y + 5, x + 10, y + 11, color); // base
        Mc189Compat.drawRect(x + 1, y + 4, x + 11, y + 5, color); // roof slant
        Mc189Compat.drawRect(x + 5, y + 1, x + 7, y + 4, color); // roof top
        Mc189Compat.drawRect(x + 4, y + 8, x + 8, y + 11, COLOR_PANEL); // door
    }

    static void drawAccountIcon(int x, int y, int color) {
        // Simple person icon
        drawCircle(x + 6, y + 4, 2, color); // head
        Mc189Compat.drawRect(x + 2, y + 7, x + 10, y + 8, color); // shoulders
        Mc189Compat.drawRect(x + 4, y + 8, x + 8, y + 11, color); // body
    }

    static void drawEditIcon(int x, int y, int color) {
        // Simple pencil icon
        Mc189Compat.drawRect(x + 4, y + 2, x + 8, y + 9, color); // body
        Mc189Compat.drawRect(x + 3, y + 9, x + 9, y + 10, color); // tip base
        Mc189Compat.drawRect(x + 5, y + 10, x + 7, y + 11, color); // tip
        Mc189Compat.drawRect(x + 4, y + 1, x + 8, y + 2, color); // eraser
    }

    static void drawHudIcon(int x, int y, int color) {
        Mc189Compat.drawRect(x + 2, y + 2, x + 10, y + 3, color);
        Mc189Compat.drawRect(x + 2, y + 9, x + 10, y + 10, color);
        Mc189Compat.drawRect(x + 2, y + 2, x + 3, y + 10, color);
        Mc189Compat.drawRect(x + 9, y + 2, x + 10, y + 10, color);
        Mc189Compat.drawRect(x + 4, y + 4, x + 8, y + 5, color);
    }

    static void drawGameplayIcon(int x, int y, int color) {
        Mc189Compat.drawRect(x + 5, y + 2, x + 7, y + 10, color); // sword hilt
        Mc189Compat.drawRect(x + 3, y + 4, x + 9, y + 5, color); // sword guard
        Mc189Compat.drawRect(x + 6, y + 1, x + 7, y + 2, color); // sword tip
    }

    static void drawRenderIcon(int x, int y, int color) {
        // eye
        drawCircle(x + 6, y + 6, 4, color);
        drawCircle(x + 6, y + 6, 3, COLOR_PANEL);
        drawCircle(x + 6, y + 6, 1, color);
    }

    static void drawPerformanceIcon(int x, int y, int color) {
        // bolt
        Mc189Compat.drawRect(x + 5, y + 1, x + 7, y + 4, color);
        Mc189Compat.drawRect(x + 4, y + 4, x + 8, y + 7, color);
        Mc189Compat.drawRect(x + 5, y + 7, x + 7, y + 11, color);
    }

    static void drawCosmeticsIcon(int x, int y, int color) {
        // shirt
        Mc189Compat.drawRect(x + 2, y + 2, x + 10, y + 10, color);
        Mc189Compat.drawRect(x + 4, y, x + 8, y + 2, color);
        Mc189Compat.drawRect(x, y + 2, x + 2, y + 5, color);
        Mc189Compat.drawRect(x + 10, y + 2, x + 12, y + 5, color);
    }

    static void drawClientIcon(int x, int y, int color) {
        // gear
        drawCircle(x + 6, y + 6, 4, color);
        drawCircle(x + 6, y + 6, 2, COLOR_PANEL);
        for (int i = 0; i < 4; i++) {
            Mc189Compat.drawRect(x + 5, y - 1 + i * 4, x + 7, y + 1 + i * 4, color);
            Mc189Compat.drawRect(x - 1 + i * 4, y + 5, x + 1 + i * 4, y + 7, color);
        }
    }

    static float lerp(float a, float b, float t) {
        return a + t * (b - a);
    }

    static int lerpColor(int from, int to, float t) {
        Color fromColor = new Color(from, true);
        Color toColor = new Color(to, true);
        int r = (int) lerp(fromColor.getRed(), toColor.getRed(), t);
        int g = (int) lerp(fromColor.getGreen(), toColor.getGreen(), t);
        int b = (int) lerp(fromColor.getBlue(), toColor.getBlue(), t);
        int a = (int) lerp(fromColor.getAlpha(), toColor.getAlpha(), t);
        return new Color(r, g, b, a).getRGB();
    }

    // --- Old methods for legacy screens ---

    static void background(int width, int height, long time) {
        Mc189Compat.drawRect(0, 0, width, height, 0xFFBDEFFF);
        Mc189Compat.drawRect(0, height / 2, width, height, 0xFFEAF8FF);
        for (int i = 0; i < 18; i++) {
            int x = (int) ((i * 73L + time / 45L) % Math.max(1, width + 40)) - 20;
            int y = 18 + (i * 29) % Math.max(1, height - 36);
            int size = 2 + (i % 3);
            Mc189Compat.drawRect(x, y, x + size, y + size, 0x66FFFFFF);
        }
    }

    static void panel(int left, int top, int right, int bottom) {
        panel(left, top, right, bottom, false);
    }

    static void panel(int left, int top, int right, int bottom, boolean hover) {
        Mc189Compat.drawRect(left + 2, top + 2, right + 2, bottom + 2, hover ? 0x774ABEEB : SHADOW);
        Mc189Compat.drawRect(left, top, right, bottom, hover ? 0xEEF8FDFF : GLASS);
        Mc189Compat.drawRect(left, top, right, top + 2, hover ? 0xDDFFFFFF : 0xFFFFFFFF);
        Mc189Compat.drawRect(left, bottom - 1, right, bottom, 0x7752BEEB);
    }

    static void button(Object font, AetherButton button, int mouseX, int mouseY) {
        boolean hover = button.contains(mouseX, mouseY);
        int fill = hover ? 0xEE52BEEB : 0xCCF5FBFF;
        int text = hover ? 0xFFFFFFFF : TEXT_DARK;
        Mc189Compat.drawRect(button.x(), button.y(), button.x() + button.width(), button.y() + button.height(), fill);
        Mc189Compat.drawRect(button.x(), button.y(), button.x() + 2, button.y() + button.height(), 0xFF52BEEB);
        centered(font, button.label(), button.x(), button.y() + 7, button.width(), text);
    }

    static void iconButton(Object font, AetherButton button, String icon, int mouseX, int mouseY) {
        boolean hover = button.contains(mouseX, mouseY);
        Mc189Compat.drawRect(button.x(), button.y(), button.x() + button.width(), button.y() + button.height(), hover ? 0xEE52BEEB : 0xBBFFFFFF);
        Mc189Compat.drawRect(button.x(), button.y(), button.x() + 2, button.y() + button.height(), ACCENT);
        centered(font, icon, button.x(), button.y() + button.height() / 2 - 3, button.width(), hover ? 0xFFFFFFFF : TEXT_DARK);
    }

    static void tooltip(Object font, String text, int mouseX, int mouseY) {
        int width = Mc189Compat.stringWidth(font, text) + 12;
        int left = mouseX + 10;
        int top = mouseY + 10;
        Mc189Compat.drawRect(left + 2, top + 2, left + width + 2, top + 20, SHADOW);
        Mc189Compat.drawRect(left, top, left + width, top + 18, 0xEEF5FBFF);
        Mc189Compat.drawRect(left, top, left + 2, top + 18, ACCENT);
        text(font, text, left + 6, top + 6, TEXT_DARK);
    }

    static void avatar(Object font, int x, int y, String username) {
        Mc189Compat.drawRect(x, y, x + 24, y + 24, 0xCC52BEEB);
        Mc189Compat.drawRect(x + 3, y + 3, x + 21, y + 21, 0xFFEAF8FF);
        Mc189Compat.drawRect(x + 6, y + 8, x + 10, y + 12, ACCENT_DARK);
        Mc189Compat.drawRect(x + 14, y + 8, x + 18, y + 12, ACCENT_DARK);
        Mc189Compat.drawRect(x + 8, y + 16, x + 16, y + 18, 0x996C8795);
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
        Mc189Compat.drawRect(left + width / 8, centerY, right - width / 8, centerY + width / 12, 0xCCB9EAD6);
        Mc189Compat.drawRect(left, centerY + width / 12, right, centerY + width / 6, 0xDD7BC69D);
        Mc189Compat.drawRect(left + width / 5, centerY + width / 6, right - width / 5, centerY + width / 4, 0xDD8B6A4A);
        Mc189Compat.drawRect(centerX - width / 8, centerY + width / 4, centerX + width / 9, centerY + width / 2, 0xAA6A513D);
    }

    static int withAlpha(int color, int alpha) {
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
