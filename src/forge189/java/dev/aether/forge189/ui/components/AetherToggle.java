package dev.aether.forge189.ui.components;

import dev.aether.forge189.AetherUi;
import dev.aether.forge189.Mc189Compat;

public final class AetherToggle {
    private final int x, y, w, h;
    private final boolean on;

    public AetherToggle() {
        this(0, 0, 40, 20, false);
    }

    public AetherToggle(int x, int y, int w, int h, boolean on) {
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;
        this.on = on;
    }

    public void render(boolean hover) {
        int bg = on ? AetherUi.withAlpha(AetherUi.ACCENT_ON, 0x55) : AetherUi.TRACK;
        int border = on ? AetherUi.ACCENT_ON : AetherUi.withAlpha(AetherUi.PANEL_EDGE, 0x33);
        AetherUi.roundRect(x, y, x + w, y + h, h / 2, bg);
        AetherUi.outline(x, y, x + w, y + h, border);
        int knob = h - 4;
        int knobX = on ? x + w - knob - 2 : x + 2;
        AetherUi.roundRect(knobX, y + 2, knobX + knob, y + 2 + knob, knob / 2, on ? AetherUi.PANEL_EDGE : AetherUi.TEXT_DISABLED);
    }

    public boolean contains(int mouseX, int mouseY) {
        return mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + h;
    }
}