package dev.aether.menu.comp;

import dev.aether.ui.UiCanvas;
import dev.aether.ui.UiTheme;

/**
 * The boolean control of the grammar: a 34x16 rounded track (radius 7) whose fill
 * fades from the card colour to the accent, with an 11px knob gliding from the left
 * edge to the right. Drawn at 0.85 scale inside the settings grid, like the reference.
 */
public final class UiToggle {

    private static final float TRACK_W = 34.0F;
    private static final float TRACK_H = 16.0F;
    private static final float KNOB = 11.0F;

    private final dev.aether.ui.UiMotion fill = new dev.aether.ui.UiMotion(0.0F, 14.0F);

    /** Advances the animation; the screen calls this each frame. */
    public void update(boolean on) {
        fill.target(on ? 1.0F : 0.0F);
        fill.update();
    }

    /** Draws at the top-left (x, y) in GUI pixels, uniformly scaled (0.85 in settings). */
    public void draw(float x, float y, float scale, boolean on, boolean hover) {
        float w = TRACK_W * scale;
        float h = TRACK_H * scale;
        float value = fill.value();
        int base = UiTheme.withAlpha(UiTheme.cardHover(), hover ? 255 : 235);
        int fillTop = UiTheme.withAlpha(UiTheme.accent(), Math.round(255 * value));
        int fillBottom = UiTheme.withAlpha(UiTheme.accentDeep(), Math.round(255 * value));
        if (value < 0.999F) {
            UiCanvas.roundRect(x, y, w, h, 7.0F * scale, base);
        }
        if (value > 0.001F) {
            UiCanvas.gradientRoundRect(x, y, w, h, 7.0F * scale, fillTop, fillBottom);
        }
        float knobX = x + (2.5F + value * 18.0F) * scale;
        float knobY = y + (TRACK_H - KNOB) * scale / 2.0F;
        UiCanvas.circle(knobX + KNOB * scale / 2.0F, knobY + KNOB * scale / 2.0F,
            KNOB * scale / 2.0F, on ? 0xFFFFFFFF : UiTheme.text());
    }

    /** Hit test in GUI pixels at the same coordinates draw used (scale included). */
    public boolean hits(float x, float y, float scale, double mx, double my) {
        return mx >= x && mx < x + TRACK_W * scale && my >= y && my < y + TRACK_H * scale;
    }
}
