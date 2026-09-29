package dev.aether.forge189.ui.components;

import dev.aether.forge189.AetherUi;
import dev.aether.forge189.Mc189Compat;

public final class AetherDropdown {
    private final int x, y, w, h;
    private final String label;
    private final boolean hover;
    private final boolean open;

    public AetherDropdown() {
        this(0, 0, 120, 24, "", false);
    }

    public AetherDropdown(int x, int y, int w, int h,
                         String label, boolean hover) {
        this.x = x; this.y = y; this.w = w; this.h = h;
        this.label = label;
        this.hover = hover;
        this.open = false;
    }

    public void render(Object font) {
        int bg = hover ? AetherUi.withAlpha(AetherUi.PANEL_EDGE, 0x1E)
                       : AetherUi.withAlpha(AetherUi.PANEL_EDGE, 0x1E);
        AetherUi.roundRect(x, y, x + w, y + h, 3, bg);
        int txt = hover ? AetherUi.TEXT_PRIMARY
                        : AetherUi.TEXT_SECONDARY;
        AetherUi.textSmooth(font, label, x + 6, y + 4, txt);
        AetherUi.drawChevron(x + w - 12, y + h / 2 - 2, 1, txt);
        AetherUi.drawChevron(x + w - 6,  y + h / 2 - 2, -1, txt);
    }
}