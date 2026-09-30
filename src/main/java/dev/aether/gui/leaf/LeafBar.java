package dev.aether.gui.leaf;

import org.lwjgl.input.Mouse;

import dev.aether.gui.AetherFont;
import dev.aether.gui.GuiScale;
import dev.aether.gui.core.UiComponent;
import dev.aether.forge189.AetherUi;
import dev.aether.forge189.Mc189Compat;

/**
 * Port of Leaf Client's {@code com.leafclient.screen.ui.Bar} (GPLv3, see
 * docs/GUI_REBUILD.md): a slider whose row label is drawn 250 design units left of
 * the track, whose knob follows the mouse while the button is held (Leaf polls
 * {@code Mouse.isButtonDown(0)} inside the move handler), and whose value text floats
 * above the knob. The commit fires when the button is released, like Leaf's
 * {@code doThings()}.
 */
public final class LeafBar extends UiComponent {

    private static final int TRACK_RANGE = 255;

    private final String rowName;
    private final ValueReader reader;
    private final ValueWriter writer;
    private final double min;
    private final double max;
    private final double step;
    private final String suffix;
    private final Runnable onCommit;
    private boolean hover;
    private boolean dragging;

    public interface ValueReader {
        double read();
    }

    public interface ValueWriter {
        void write(double value);
    }

    public LeafBar(String rowName, int x, int y, int width, int height,
                   double min, double max, double step, String suffix,
                   ValueReader reader, ValueWriter writer, Runnable onCommit) {
        this.rowName = rowName;
        this.min = min;
        this.max = max;
        this.step = step <= 0.0D ? 1.0D : step;
        this.suffix = suffix == null ? "" : suffix;
        this.reader = reader;
        this.writer = writer;
        this.onCommit = onCommit;
        at(x, y).size(width, height);
    }

    @Override
    public void render() {
        int left = gx();
        int top = gy();
        int w = gw();
        int h = gh();

        if (rowName != null && !rowName.isEmpty()) {
            int labelSize = AetherFont.height(AetherFont.Size.BODY);
            AetherFont.draw(AetherFont.Size.BODY, rowName, left - GuiScale.w(250),
                top + (h - labelSize) / 2, AetherUi.TEXT_SECONDARY);
        }

        // Track centered vertically inside the hit area (Leaf's bar texture fills the
        // 90-high slot; the groove itself is a thin line in the middle).
        int trackH = GuiScale.h(8);
        int trackY = top + (h - trackH) / 2;
        AetherUi.drawRoundRect(left, trackY, left + w, trackY + trackH, trackH / 2, AetherUi.TRACK);

        double value = reader.read();
        double fraction = max > min ? (value - min) / (max - min) : 0.0D;
        fraction = Math.max(0.0D, Math.min(1.0D, fraction));
        int fillW = (int) (w * fraction);
        if (fillW > 0) {
            AetherUi.drawRoundRect(left, trackY, left + fillW, trackY + trackH, trackH / 2, AetherUi.ACCENT);
        }

        int knob = GuiScale.h(22);
        int knobX = left + fillW - knob / 2;
        int knobY = top + (h - knob) / 2;
        AetherUi.drawRoundRect(knobX, knobY, knobX + knob, knobY + knob, knob / 2,
            dragging || hover ? 0xFFFFFFFF : 0xFFD8DAE6);

        String valueText = format(value) + suffix;
        AetherFont.drawShadowed(AetherFont.Size.SMALL, valueText,
            knobX + knob / 2 - AetherFont.width(AetherFont.Size.SMALL, valueText) / 2,
            knobY - GuiScale.h(18), AetherUi.TEXT_PRIMARY);
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

    @Override
    public void onMouseMove(double mouseX, double mouseY) {
        hover = contains(mouseX, mouseY);
        if (hover && dragging) {
            if (Mouse.isButtonDown(0)) {
                double fraction = (mouseX - x) / (double) Math.max(1, width);
                fraction = Math.max(0.0D, Math.min(1.0D, fraction));
                double value = min + fraction * (max - min);
                double snapped = snap(value);
                double current = reader.read();
                if (Math.abs(snapped - current) > 1.0E-9D) {
                    writer.write(snapped);
                }
            } else {
                dragging = false;
                if (onCommit != null) {
                    onCommit.run();
                }
            }
        }
    }

    @Override
    public boolean onMouseClick(double mouseX, double mouseY, int button) {
        if (button != 0 || !contains(mouseX, mouseY)) {
            return false;
        }
        dragging = true;
        double fraction = (mouseX - x) / (double) Math.max(1, width);
        fraction = Math.max(0.0D, Math.min(1.0D, fraction));
        writer.write(snap(min + fraction * (max - min)));
        return true;
    }

    @Override
    public void onMouseRelease(double mouseX, double mouseY, int button) {
        if (dragging) {
            dragging = false;
            if (onCommit != null) {
                onCommit.run();
            }
        }
        hover = contains(mouseX, mouseY);
    }
}
