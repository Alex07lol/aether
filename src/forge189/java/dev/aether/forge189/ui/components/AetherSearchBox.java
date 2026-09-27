package dev.aether.forge189.ui.components;

import dev.aether.forge189.AetherUi;
import dev.aether.forge189.Mc189Compat;

public final class AetherSearchBox {
    private int x, y, w, h;
    private String query = "";
    private boolean focused = false;
    private boolean hover = false;

    public AetherSearchBox() {}

    public void render(Object font, int x, int y) {
        this.x = x;
        this.y = y;
        this.w = 200;
        this.h = 20;

        int bg = focused ? AetherUi.withAlpha(AetherUi.SURFACE, 0xF0)
                         : AetherUi.withAlpha(AetherUi.SURFACE, 0xB0);
        AetherUi.roundRect(x, y, x + w, y + h, 4, bg);
        AetherUi.outline(x, y, x + w, y + h,
                         focused ? AetherUi.ACCENT : AetherUi.withAlpha(AetherUi.PANEL_EDGE, 0x22));
        AetherUi.drawSearchGlyph(x + 8, y + 6,
                                 focused ? AetherUi.ACCENT : AetherUi.TEXT_DISABLED);
        String shown = query.isEmpty() && !focused ? "type to search" : query;
        int textColor = query.isEmpty() && !focused ? AetherUi.TEXT_DISABLED : AetherUi.TEXT_PRIMARY;
        AetherUi.text(font, shown, x + 22, y + 6, textColor);
    }

    public void render(Object font) {
        // Default render with no position update
    }

    public boolean contains(int mx, int my) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }

    public void setQuery(String query, boolean focused) {
        this.query = query;
        this.focused = focused;
    }
}