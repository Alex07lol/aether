package dev.aether.gui.components;

import dev.aether.gui.AetherFont;
import dev.aether.gui.GuiScale;
import dev.aether.gui.core.UiComponent;
import dev.aether.forge189.AetherUi;

import java.util.function.Consumer;

/**
 * The slider of the Aether GUI for bounded number settings, replacing Leaf Client's
 * {@code Bar} concept with Aether's own procedural track (see docs/GUI_REBUILD.md).
 * <p>
 * The value lives in the caller's model (a {@code Setting}); the slider reads it,
 * proposes changes through the callback, and snaps to the setting's step. A drag keeps
 * receiving movement after the cursor leaves the track: {@link #endDrag()} is called by
 * the screen when the button is released anywhere.
 */
public final class Slider extends UiComponent {

    private final String label;
    private final DoubleReader reader;
    private final DoubleWriter writer;
    private final double min;
    private final double max;
    private final double step;
    private final String suffix;
    private final Consumer<Slider> onDragStart;
    private boolean dragging;
    private boolean hover;

    /** Reads the current value from the model. */
    public interface DoubleReader {
        double read();
    }

    /** Writes a new value to the model. */
    public interface DoubleWriter {
        void write(double value);
    }

    public Slider(String label, double min, double max, double step, String suffix,
                  DoubleReader reader, DoubleWriter writer, Consumer<Slider> onDragStart) {
        this.label = label;
        this.min = min;
        this.max = max;
        this.step = step <= 0.0D ? 1.0D : step;
        this.suffix = suffix == null ? "" : suffix;
        this.reader = reader;
        this.writer = writer;
        this.onDragStart = onDragStart;
        this.height = 46;
    }

    public String label() {
        return label;
    }

    public boolean isDragging() {
        return dragging;
    }

    /** Stops an active drag; the screen calls this when the mouse button is released. */
    public void endDrag() {
        dragging = false;
    }

    @Override
    public void render() {
        int left = gx();
        int top = gy();
        int w = gw();
        int h = gh();
        double value = reader.read();
        double fraction = max > min ? (value - min) / (max - min) : 0.0D;
        fraction = Math.max(0.0D, Math.min(1.0D, fraction));

        AetherFont.draw(AetherFont.Size.SMALL, label, left, top + GuiScale.h(2), AetherUi.TEXT_SECONDARY);
        String valueText = formatValue(value) + suffix;
        AetherFont.drawRight(AetherFont.Size.SMALL, valueText, left + w, top + GuiScale.h(2), AetherUi.TEXT_PRIMARY);

        int trackTop = top + h - GuiScale.h(14);
        int trackH = GuiScale.h(5);
        AetherUi.drawRoundRect(left, trackTop, left + w, trackTop + trackH, trackH / 2, AetherUi.TRACK);
        int fillW = (int) (w * fraction);
        if (fillW > 0) {
            AetherUi.drawRoundRect(left, trackTop, left + fillW, trackTop + trackH, trackH / 2, AetherUi.ACCENT);
        }
        int knobR = GuiScale.h(8);
        int knobX = left + fillW - knobR / 2;
        int knobY = trackTop + trackH / 2 - knobR / 2;
        AetherUi.drawRoundRect(knobX, knobY, knobX + knobR, knobY + knobR, knobR / 2,
            dragging || hover ? 0xFFFFFFFF : 0xFFD8DAE6);
    }

    private String formatValue(double value) {
        double snapped = snap(value);
        if (step >= 1.0D) {
            return String.valueOf((int) Math.round(snapped));
        }
        return String.format(java.util.Locale.ENGLISH, "%.2f", Double.valueOf(snapped));
    }

    private double snap(double value) {
        double offset = value - min;
        double steps = Math.round(offset / step);
        double snapped = min + steps * step;
        return Math.max(min, Math.min(max, snapped));
    }

    @Override
    public void onMouseMove(double mouseX, double mouseY) {
        hover = contains(mouseX, mouseY);
        if (dragging) {
            applyMouse(mouseX);
        }
    }

    @Override
    public boolean onMouseClick(double mouseX, double mouseY, int button) {
        if (button != 0 || !contains(mouseX, mouseY)) {
            return false;
        }
        dragging = true;
        if (onDragStart != null) {
            onDragStart.accept(this);
        }
        applyMouse(mouseX);
        return true;
    }

    @Override
    public void onMouseRelease(double mouseX, double mouseY, int button) {
        dragging = false;
        hover = contains(mouseX, mouseY);
    }

    private void applyMouse(double mouseX) {
        double fraction = (mouseX - x) / (double) Math.max(1, width);
        fraction = Math.max(0.0D, Math.min(1.0D, fraction));
        double value = min + fraction * (max - min);
        double snapped = snap(value);
        double current = reader.read();
        if (Math.abs(snapped - current) > 1.0E-9D) {
            writer.write(snapped);
        }
    }
}
