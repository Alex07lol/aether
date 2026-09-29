package dev.aether.gui.core;

import dev.aether.gui.GuiScale;
import dev.aether.forge189.AetherUi;
import dev.aether.forge189.Mc189Compat;

import java.util.ArrayList;
import java.util.List;

/**
 * The one scroll implementation of the Aether GUI: a viewport that clips, translates
 * and culls its children.
 * <p>
 * Children are positioned in content space (their {@code y} is relative to the top of
 * the content, not the viewport). Rendering opens a scissor over the viewport, draws
 * only the children whose content rectangle intersects the visible window, and moves
 * each of them by the current offset; input is converted into content space with the
 * same offset before dispatch, so what is drawn and what is clicked can never disagree.
 * The offset is clamped to {@code [0, contentHeight - viewportHeight]}.
 * <p>
 * This replaces the index-paging scrollbar of Leaf Client's 1.8.9 GUI (see
 * docs/GUI_REBUILD.md): partial rows stay partial, and a filter change that shrinks the
 * list simply re-clamps the offset instead of desyncing paging state.
 */
public final class ScrollContainer extends UiComponent {

    /** Extra space added below the last child when computing the content height. */
    private static final int BOTTOM_PADDING = 12;
    /** Design-unit thickness of the scrollbar. */
    private static final int BAR_WIDTH = 5;
    private static final int MIN_THUMB = 28;

    private final List<UiComponent> children = new ArrayList<UiComponent>();
    private double offset;
    private double contentHeight;
    /** The component that currently holds a pressed mouse button, if any. */
    private UiComponent pressed;

    /** Registers a child. Callers keep laying out children themselves. */
    public void add(UiComponent child) {
        children.add(child);
        contentHeight = -1.0D; // recomputed lazily on next use
    }

    public void clear() {
        children.clear();
        pressed = null;
        contentHeight = -1.0D;
    }

    public int childCount() {
        return children.size();
    }

    @Override
    public void onResize() {
        clampOffset();
    }

    /* ── geometry ───────────────────────────────────────────────────────── */

    /** Height of the content in design units, bottom padding included. */
    public double contentHeight() {
        if (contentHeight < 0.0D) {
            double bottom = 0.0D;
            for (UiComponent child : children) {
                bottom = Math.max(bottom, child.getY() + child.getHeight());
            }
            contentHeight = bottom + BOTTOM_PADDING;
        }
        return contentHeight;
    }

    /** How far the content can scroll, in design units (0 when everything fits). */
    public double maxScroll() {
        return Math.max(0.0D, contentHeight() - height);
    }

    public double offset() {
        return offset;
    }

    public void scrollTo(double value) {
        offset = value;
        clampOffset();
    }

    public void scrollBy(double delta) {
        scrollTo(offset + delta);
    }

    /** Instantly re-clamps; called after the content shrinks under a filter change. */
    public void clampOffset() {
        double max = maxScroll();
        if (offset > max) {
            offset = max;
        }
        if (offset < 0.0D) {
            offset = 0.0D;
        }
    }

    /* ── rendering ──────────────────────────────────────────────────────── */

    @Override
    public void render() {
        if (!visible) {
            return;
        }
        clampOffset();
        int left = gx();
        int top = gy();
        int viewportW = gw();
        int viewportH = gh();
        double viewportHUnits = height;
        Mc189Compat.pushScissor(left, top, viewportW, viewportH);
        try {
            for (UiComponent child : children) {
                if (!child.isVisible()) {
                    continue;
                }
                double childTop = child.getY();
                double childBottom = childTop + child.getHeight();
                // Cull children entirely outside the visible window of the content.
                if (childBottom <= offset || childTop >= offset + viewportHUnits) {
                    continue;
                }
                child.renderOffsetX = this.x;
                child.renderOffsetY = -offset;
                try {
                    child.render();
                } finally {
                    child.renderOffsetX = 0.0D;
                    child.renderOffsetY = 0.0D;
                }
            }
            drawScrollbar(left, top, viewportW, viewportH);
        } finally {
            Mc189Compat.popScissor();
        }
    }

    private void drawScrollbar(int leftGui, int topGui, int viewportWGui, int viewportHGui) {
        double max = maxScroll();
        if (max <= 0.0D) {
            return;
        }
        int barX = leftGui + viewportWGui - GuiScale.w(BAR_WIDTH + 2);
        int trackTop = topGui + GuiScale.h(2);
        int trackH = viewportHGui - GuiScale.h(4);
        Mc189Compat.drawRect(barX, trackTop, barX + GuiScale.w(BAR_WIDTH), trackTop + trackH,
            AetherUi.withAlpha(AetherUi.PANEL_EDGE, 0x30));
        // The thumb is the fraction of the content that is visible, in track pixels.
        double visibleFraction = Math.min(1.0D, height / contentHeight());
        int thumbH = Math.max(GuiScale.h(MIN_THUMB), (int) (trackH * visibleFraction));
        double progress = offset / max;
        int thumbY = trackTop + (int) ((double) (trackH - thumbH) * progress);
        Mc189Compat.drawRect(barX, thumbY, barX + GuiScale.w(BAR_WIDTH), thumbY + thumbH,
            AetherUi.withAlpha(AetherUi.ACCENT, pressed != null ? 0x99 : 0x66));
    }

    /* ── input ──────────────────────────────────────────────────────────── */

    @Override
    public void onMouseMove(double mouseX, double mouseY) {
        double contentX = mouseX - this.x;
        UiComponent target = pressed != null ? pressed : hit(contentX, mouseY);
        if (target != null) {
            target.onMouseMove(contentX, mouseY + offset);
        }
    }

    @Override
    public boolean onMouseClick(double mouseX, double mouseY, int button) {
        if (!visible || !contains(mouseX, mouseY)) {
            return false;
        }
        double contentX = mouseX - this.x;
        UiComponent target = hit(contentX, mouseY);
        if (target != null && target.onMouseClick(contentX, mouseY + offset, button)) {
            pressed = target;
        }
        // The viewport swallows the click either way: nothing behind it may react to a
        // click that landed inside the scroll area.
        return true;
    }

    @Override
    public void onMouseRelease(double mouseX, double mouseY, int button) {
        if (pressed != null) {
            pressed.onMouseRelease(mouseX - this.x, mouseY + offset, button);
            pressed = null;
        }
    }

    @Override
    public boolean onWheel(double mouseX, double mouseY, int delta) {
        if (!visible || !contains(mouseX, mouseY)) {
            return false;
        }
        scrollBy(delta);
        return true;
    }

    @Override
    public boolean onKeyTyped(char typedChar, int keyCode) {
        if (pressed != null && pressed.onKeyTyped(typedChar, keyCode)) {
            return true;
        }
        // Focused controls (text fields) receive keys regardless of what was pressed.
        for (UiComponent child : children) {
            if (child.onKeyTyped(typedChar, keyCode)) {
                return true;
            }
        }
        return false;
    }

    /** Applies an operation to every child, for screen-level passes such as defocusing. */
    public void forEachChild(java.util.function.Consumer<UiComponent> operation) {
        for (UiComponent child : children) {
            operation.accept(child);
        }
    }

    @Override
    public void dispose() {
        for (UiComponent child : children) {
            child.dispose();
        }
        children.clear();
        pressed = null;
    }

    /** Topmost child under the content-space point, or null. */
    private UiComponent hit(double mouseX, double mouseY) {
        for (int i = children.size() - 1; i >= 0; i--) {
            UiComponent child = children.get(i);
            if (child.contains(mouseX, mouseY + offset)) {
                return child;
            }
        }
        return null;
    }
}
