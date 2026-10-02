package dev.aether.gui.components;

import java.util.ArrayList;
import java.util.List;

import dev.aether.animation.Anim;
import dev.aether.animation.AnimationMath;
import dev.aether.animation.Easing;
import dev.aether.forge189.AetherUi;
import dev.aether.gui.AetherFont;
import dev.aether.gui.GuiScale;
import dev.aether.gui.core.UiComponent;

/**
 * A horizontal row of filter chips - the category selector the module browser and the cosmetics
 * gallery share.
 * <p>
 * The selection marker is a single animated rectangle that glides from the old chip to the new one
 * (a 260 ms ease-out-cubic move, Glide's segmented-control feel) instead of two chips blinking off
 * and on, and the chip under the cursor gets a low-alpha wash. The labels are supplied by the
 * caller, so a screen never hard-codes a category list here.
 */
public final class ChipBar extends UiComponent {

    private static final int CHIP_HEIGHT = 34;
    private static final int CHIP_PADDING = 16;
    private static final int CHIP_GAP = 6;
    private static final int CHIP_RADIUS = 8;

    private final List<String> labels = new ArrayList<String>();
    private final Runnable onSelect;
    private final Anim markerX = new Anim(0.0F, 260.0F, Easing.EASE_OUT_CUBIC);
    private final Anim markerWidth = new Anim(0.0F, 260.0F, Easing.EASE_OUT_CUBIC);
    private final Anim hover = new Anim(0.0F, 110.0F, Easing.EASE_OUT_QUAD);

    private int selected;
    private int hoveredIndex = -1;
    private boolean placed;

    public ChipBar(Runnable onSelect) {
        this.onSelect = onSelect;
        size(0, CHIP_HEIGHT);
    }

    public ChipBar place(int x, int y, int width) {
        at(x, y).size(width, CHIP_HEIGHT);
        return this;
    }

    public void labels(List<String> next) {
        this.labels.clear();
        if (next != null) {
            this.labels.addAll(next);
        }
        if (this.selected >= this.labels.size()) {
            this.selected = Math.max(0, this.labels.size() - 1);
        }
        this.placed = false;
    }

    public int selected() {
        return this.selected;
    }

    public String selectedLabel() {
        return this.selected >= 0 && this.selected < this.labels.size() ? this.labels.get(this.selected) : "";
    }

    /** Selects a chip without running the callback; used when the screen restores its filter. */
    public void select(int index) {
        if (index < 0 || index >= this.labels.size() || index == this.selected) {
            return;
        }
        this.selected = index;
    }

    public void update() {
        int[] marker = markerRect();
        if (marker[0] < 0) {
            return;
        }
        if (!this.placed) {
            // First layout after a label change lands instantly: a list that just appeared has
            // nothing to glide from.
            this.markerX.set(marker[0]);
            this.markerWidth.set(marker[2]);
            this.placed = true;
        } else {
            this.markerX.target(marker[0]);
            this.markerWidth.target(marker[2]);
        }
        this.markerX.update();
        this.markerWidth.update();
        this.hover.update();
    }

    @Override
    public void render() {
        int cursor = gx();
        int top = gy();
        int height = GuiScale.h(CHIP_HEIGHT);
        int radius = GuiScale.w(CHIP_RADIUS);
        for (int i = 0; i < this.labels.size(); i++) {
            int[] rect = chipRect(i, cursor, top);
            if (rect[2] <= 0) {
                break;
            }
            boolean active = i == this.selected;
            if (active) {
                AetherUi.drawRoundRect((int) Math.round(this.markerX.value()), top,
                    (int) Math.round(this.markerX.value() + this.markerWidth.value()), top + height,
                    radius, AnimationMath.scaleAlpha(AetherUi.TEXT_PRIMARY, 0.14F));
            } else if (i == this.hoveredIndex) {
                AetherUi.drawRoundRect(rect[0], top, rect[0] + rect[2], top + height, radius,
                    AnimationMath.scaleAlpha(AetherUi.TEXT_PRIMARY, this.hover.value() * 0.06F));
            }
            int color = active ? AetherUi.TEXT_PRIMARY
                : (i == this.hoveredIndex ? AetherUi.blend(AetherUi.TEXT_DISABLED, AetherUi.TEXT_SECONDARY, this.hover.value())
                    : AetherUi.TEXT_SECONDARY);
            int textY = top + (height - AetherFont.height(AetherFont.Size.CAPTION)) / 2;
            AetherFont.draw(AetherFont.Size.CAPTION, this.labels.get(i), rect[0] + GuiScale.w(CHIP_PADDING),
                textY, color);
        }
    }

    @Override
    public void onMouseMove(double mouseX, double mouseY) {
        int next = indexAt(mouseX, mouseY);
        if (next != this.hoveredIndex) {
            this.hoveredIndex = next;
            this.hover.set(0.0F);
        }
        this.hover.target(next >= 0 ? 1.0F : 0.0F);
    }

    @Override
    public boolean onMouseClick(double mouseX, double mouseY, int button) {
        int index = indexAt(mouseX, mouseY);
        if (button != 0 || index < 0) {
            return false;
        }
        if (index != this.selected) {
            this.selected = index;
            if (this.onSelect != null) {
                this.onSelect.run();
            }
        }
        return true;
    }

    private int indexAt(double mouseX, double mouseY) {
        if (this.labels.isEmpty()) {
            return -1;
        }
        int top = gy();
        if (mouseY < top || mouseY > top + GuiScale.h(CHIP_HEIGHT)) {
            return -1;
        }
        int cursor = gx();
        for (int i = 0; i < this.labels.size(); i++) {
            int[] rect = chipRect(i, cursor, top);
            if (mouseX >= rect[0] && mouseX <= rect[0] + rect[2]) {
                return i;
            }
        }
        return -1;
    }

    /** {x, y, width} of one chip in GUI pixels, laid out from the bar's left edge. */
    private int[] chipRect(int index, int cursor, int top) {
        int x = cursor;
        for (int i = 0; i < index && i < this.labels.size(); i++) {
            x += chipWidth(i) + GuiScale.w(CHIP_GAP);
        }
        return new int[] {x, top, chipWidth(index)};
    }

    private int chipWidth(int index) {
        return AetherFont.width(AetherFont.Size.CAPTION, this.labels.get(index)) + GuiScale.w(CHIP_PADDING * 2);
    }

    /** The selection marker's target rectangle, or {-1} when there is nothing to mark. */
    private int[] markerRect() {
        if (this.labels.isEmpty() || this.selected < 0 || this.selected >= this.labels.size()) {
            return new int[] {-1, 0, 0};
        }
        int[] rect = chipRect(this.selected, gx(), gy());
        return new int[] {rect[0], rect[1], rect[2]};
    }

    /** The total width the chips need, so a screen can right-align the bar. */
    public int requiredWidth() {
        int total = 0;
        for (int i = 0; i < this.labels.size(); i++) {
            total += chipWidth(i);
            if (i < this.labels.size() - 1) {
                total += GuiScale.w(CHIP_GAP);
            }
        }
        return total;
    }
}
