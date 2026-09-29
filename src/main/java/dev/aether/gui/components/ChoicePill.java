package dev.aether.gui.components;

import dev.aether.gui.AetherFont;
import dev.aether.gui.GuiScale;
import dev.aether.gui.core.UiComponent;
import dev.aether.forge189.AetherUi;

import java.util.List;

/**
 * The cycling choice control of the Aether GUI, the equivalent of Leaf Client's
 * {@code SelectButton} ({@code "< value >"} that advances on click), rendered
 * procedurally (see docs/GUI_REBUILD.md).
 */
public final class ChoicePill extends UiComponent {

    private final String label;
    private final List<String> choices;
    private final IntReader indexReader;
    private final IntWriter indexWriter;
    private boolean hover;

    public interface IntReader {
        int read();
    }

    public interface IntWriter {
        void write(int index);
    }

    public ChoicePill(String label, List<String> choices, IntReader indexReader, IntWriter indexWriter) {
        this.label = label;
        this.choices = choices;
        this.indexReader = indexReader;
        this.indexWriter = indexWriter;
        this.height = 46;
    }

    @Override
    public void render() {
        int left = gx();
        int top = gy();
        int w = gw();
        int h = gh();
        if (label != null && !label.isEmpty()) {
            AetherFont.draw(AetherFont.Size.SMALL, label, left, top + GuiScale.h(2), AetherUi.TEXT_SECONDARY);
        }

        int pillH = GuiScale.h(Math.min(20, height - 4));
        int pillTop = top + h - pillH - GuiScale.h(2);
        AetherUi.drawRoundRect(left, pillTop, left + w, pillTop + pillH, GuiScale.h(6),
            hover ? AetherUi.ROW_HOVER : AetherUi.ROW_BG);
        AetherUi.outline(left, pillTop, left + w, pillTop + pillH, AetherUi.withAlpha(AetherUi.PANEL_EDGE, 0x30));

        String value = current();
        int arrow = GuiScale.w(16);
        AetherFont.drawCentered(AetherFont.Size.SMALL, value, left + arrow, pillTop + (pillH - AetherFont.height(AetherFont.Size.SMALL)) / 2,
            w - arrow * 2, AetherUi.TEXT_PRIMARY);
        int cy = pillTop + pillH / 2 - 4;
        AetherUi.drawChevron(left + GuiScale.w(8), cy, -1, AetherUi.ACCENT);
        AetherUi.drawChevron(left + w - GuiScale.w(11), cy, 1, AetherUi.ACCENT);
    }

    private String current() {
        if (choices == null || choices.isEmpty()) {
            return "-";
        }
        int index = indexReader.read();
        if (index < 0 || index >= choices.size()) {
            index = 0;
        }
        return choices.get(index);
    }

    @Override
    public void onMouseMove(double mouseX, double mouseY) {
        hover = contains(mouseX, mouseY);
    }

    @Override
    public boolean onMouseClick(double mouseX, double mouseY, int button) {
        if (!contains(mouseX, mouseY)) {
            return false;
        }
        if (choices == null || choices.isEmpty() || indexWriter == null) {
            return true;
        }
        int size = choices.size();
        int index = indexReader.read();
        if (index < 0 || index >= size) {
            index = 0;
        }
        index = button == 0 ? (index + 1) % size : (index - 1 + size) % size;
        indexWriter.write(index);
        return true;
    }

    @Override
    public void onMouseRelease(double mouseX, double mouseY, int button) {
        hover = contains(mouseX, mouseY);
    }
}
