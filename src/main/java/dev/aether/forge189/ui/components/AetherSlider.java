package dev.aether.forge189.ui.components;

import dev.aether.forge189.AetherUi;
import dev.aether.forge189.Mc189Compat;

public final class AetherSlider {
    private final int x, y, w, h;
    private final float percent;
    private final boolean hover;
    private final boolean dragging;

    public AetherSlider() {
        this(0, 0, 120, 8, 0f, false, false);
    }

    public AetherSlider(int x, int y, int w, int h,
                        float percent, boolean hover, boolean dragging) {
        this.x = x; this.y = y; this.w = w; this.h = h;
        this.percent = percent;
        this.hover = hover;
        this.dragging = dragging;
    }

    public void render() {
        AetherUi.roundRect(x, y, x + w, y + h, 3,
                           AetherUi.TRACK);
        int fillW = (int) (percent * w);
        if (fillW > 0) {
            int fill = hover || dragging ? AetherUi.ACCENT_ON : AetherUi.ACCENT;
            AetherUi.roundRect(x, y, x + Math.max(3, fillW), y + h,
                               3, fill);
        }
        int knobX = x + fillW - 1;
        Mc189Compat.drawRect(knobX - 1, y - 2,
                             knobX + 2, y + h + 2,
                             AetherUi.ACCENT);
    }
}