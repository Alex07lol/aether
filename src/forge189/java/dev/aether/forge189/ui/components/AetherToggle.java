package dev.aether.forge189.ui.components;

import dev.aether.forge189.AetherUi;
import dev.aether.forge189.Mc189Compat;

public final class AetherToggle {
    private final int x, y, w, h;
    private final boolean on;

    public AetherToggle() {
        this(0, 0, 26, 13, false);
    }

    public AetherToggle(int x, int y, int w, int h, boolean on) {
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;
        this.on = on;
    }

    public void render(boolean hover) {
        int fill = on ? AetherUi.ACCENT : AetherUi.TOGGLE_BG;
        int border = hover
                ? AetherUi.withAlpha(AetherUi.ACCENT_ON, 0xAA)
                : AetherUi.withAlpha(AetherUi.PANEL_EDGE, 0x55);
        int radius = h / 2;

        // Background track
        AetherUi.drawRoundRect(x, y, x + w, y + h, radius, fill);
        // Thin border
        AetherUi.outline(x, y, x + w, y + h, border);
        // Sliding handle
        int knob = h - 4;
        int knobX = on ? x + w - knob - 2 : x + 2;
        AetherUi.drawRoundRect(knobX, y + 2, knobX + knob, y + 2 + knob, knob / 2,
                on ? AetherUi.SURFACE : AetherUi.TEXT_DISABLED);
    }

    public boolean contains(int mouseX, int mouseY) {
        return mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + h;
    }
}