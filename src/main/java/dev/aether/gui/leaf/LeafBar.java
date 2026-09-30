package dev.aether.gui.leaf;

import org.lwjgl.input.Mouse;

import dev.aether.gui.AetherFont;
import dev.aether.gui.GuiScale;
import dev.aether.gui.core.UiComponent;
import dev.aether.forge189.AetherUi;

/**
 * Port of Leaf Client's {@code com.leafclient.screen.ui.Bar} (GPLv3, see docs/GUI_REBUILD.md): a
 * slider drawn as Leaf's {@code bar_main.png} groove with {@code bar_point.png} as its knob, whose
 * row label sits 250 design units left of the track, whose knob is a square as tall as the bar
 * (Leaf draws it at {@code (x + x_point) - h/2}), and whose value text floats above the knob while
 * the button is held. The commit fires when the button is released, like Leaf's {@code doThings()}.
 */
public final class LeafBar extends UiComponent {

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

        LeafArt.draw(LeafArt.BAR_TRACK, left, top, w, h, LeafArt.NORMAL);

        double value = reader.read();
        double fraction = fraction(value);
        int knobX = left + (int) Math.round(w * fraction) - h / 2;
        LeafArt.draw(LeafArt.BAR_KNOB, knobX, top, h, h,
            dragging || hover ? LeafArt.BRIGHT : LeafArt.NORMAL);

        String valueText = format(value) + suffix;
        AetherFont.drawShadowed(AetherFont.Size.SMALL, valueText,
            knobX + h / 2 - AetherFont.width(AetherFont.Size.SMALL, valueText) / 2,
            top - AetherFont.height(AetherFont.Size.SMALL) - GuiScale.h(6), AetherUi.TEXT_PRIMARY);
    }

    private double fraction(double value) {
        double raw = max > min ? (value - min) / (max - min) : 0.0D;
        return Math.max(0.0D, Math.min(1.0D, raw));
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

    private double valueAt(double mouseX) {
        double fraction = (mouseX - x) / (double) Math.max(1, width);
        fraction = Math.max(0.0D, Math.min(1.0D, fraction));
        return snap(min + fraction * (max - min));
    }

    @Override
    public void onMouseMove(double mouseX, double mouseY) {
        hover = contains(mouseX, mouseY);
        if (!dragging) {
            return;
        }
        // Leaf keeps following the cursor while the button is down and polls the button in the move
        // handler, so a drag that leaves the bar still updates and still commits on release.
        if (Mouse.isButtonDown(0)) {
            double value = valueAt(mouseX);
            if (Math.abs(value - reader.read()) > 1.0E-9D) {
                writer.write(value);
            }
        } else {
            dragging = false;
            if (onCommit != null) {
                onCommit.run();
            }
        }
    }

    @Override
    public boolean onMouseClick(double mouseX, double mouseY, int button) {
        if (button != 0 || !contains(mouseX, mouseY)) {
            return false;
        }
        dragging = true;
        writer.write(valueAt(mouseX));
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
