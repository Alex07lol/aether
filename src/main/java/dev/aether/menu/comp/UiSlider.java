package dev.aether.menu.comp;

import dev.aether.gui.AetherFont;
import dev.aether.ui.UiCanvas;
import dev.aether.ui.UiTheme;

/**
 * The number control of the grammar: a 4px track (radius 2) with an accent fill, an
 * 8x8 gradient thumb riding the fill edge, and a 7px value label floating above the
 * thumb while the slider is hovered or dragged.
 */
public final class UiSlider {

    public interface Read {
        double read();
    }

    public interface Write {
        void write(double value);
    }

    private final Read reader;
    private final Write writer;
    private final double min;
    private final double max;
    private final double step;
    private final String suffix;

    private boolean dragging;
    private boolean hover;
    private float lastX;
    private float lastY;

    public UiSlider(double min, double max, double step, String suffix, Read reader, Write writer) {
        this.min = min;
        this.max = max;
        this.step = step <= 0.0D ? 1.0D : step;
        this.suffix = suffix == null ? "" : suffix;
        this.reader = reader;
        this.writer = writer;
    }

    public void draw(float x, float y, float trackW, double mx, double my) {
        this.hover = mx >= x - 4.0F && mx < x + trackW + 4.0F && my >= y - 6.0F && my < y + 12.0F;
        double value = reader.read();
        double fraction = max > min ? (value - min) / (max - min) : 0.0D;
        fraction = Math.max(0.0D, Math.min(1.0D, fraction));

        UiCanvas.roundRect(x, y, trackW, 4.0F, 2.0F, UiTheme.withAlpha(UiTheme.cardHover(), 255));
        int fillW = Math.round(trackW * (float) fraction);
        if (fillW > 0) {
            UiCanvas.roundRect(x, y, fillW, 4.0F, 2.0F, UiTheme.accent());
        }
        float thumbX = x + fillW - 4.0F;
        float thumbY = y - 2.0F;
        UiCanvas.gradientRoundRect(thumbX, thumbY, 8.0F, 8.0F, 4.0F, UiTheme.accent(), UiTheme.accentDeep());

        if (hover || dragging) {
            String label = format(value) + suffix;
            int labelW = AetherFont.width(7.0F, label);
            AetherFont.draw(7.0F, label, thumbX + 4.0F - labelW / 2.0F, y - 12.0F, UiTheme.text());
        }
        this.lastX = x;
        this.lastY = y;
    }

    private String format(double value) {
        double snapped = snap(value);
        if (step >= 1.0D) {
            return String.valueOf((int) Math.round(snapped));
        }
        return String.format(java.util.Locale.ENGLISH, "%.2f", Double.valueOf(snapped));
    }

    private double snap(double value) {
        double offset = value - min;
        double snapped = min + Math.round(offset / step) * step;
        return Math.max(min, Math.min(max, snapped));
    }

    /** Begins a drag (click anywhere on the slider row's control zone). */
    public boolean click(float x, float y, float trackW, double mx, double my, int button) {
        if (button != 0 || my < y - 8.0F || my > y + 14.0F || mx < x - 8.0F || mx > x + trackW + 8.0F) {
            return false;
        }
        dragging = true;
        apply(mx, x, trackW);
        return true;
    }

    /** Continues a drag; the screen calls this every frame while the button is held. */
    public boolean drag(float x, float trackW, double mx, boolean buttonDown) {
        if (!dragging) {
            return false;
        }
        if (!buttonDown) {
            dragging = false;
            return true;
        }
        apply(mx, x, trackW);
        return true;
    }

    private void apply(double mx, float x, float trackW) {
        double fraction = (mx - x) / (double) Math.max(1.0F, trackW);
        fraction = Math.max(0.0D, Math.min(1.0D, fraction));
        double value = min + fraction * (max - min);
        double snapped = snap(value);
        double current = reader.read();
        if (Math.abs(snapped - current) > 1.0E-9D) {
            writer.write(snapped);
        }
    }

    public boolean dragging() {
        return dragging;
    }
}
