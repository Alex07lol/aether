package dev.aether.gui.core;

import dev.aether.gui.GuiScale;

/**
 * Base class of every Aether GUI control - the equivalent of Leaf Client's
 * {@code com.leafclient.screen.ui.UIBase} lifecycle (render / onMouseMove /
 * onMouseClick), extended with the rest of the lifecycle a screen actually needs:
 * mouse release, keyboard, wheel, resize and dispose. Adapted structure; the
 * implementation is Aether's own. See docs/GUI_REBUILD.md for the license note.
 * <p>
 * Coordinates are design units (see {@link GuiScale}). Components store their own
 * rectangle, draw through the central conversion, and answer hit-tests in the same
 * space, so a component can never be drawn in one coordinate system and clicked in
 * another.
 * <p>
 * A component must not keep screen-instance state that survives a screen change: the
 * screens create fresh component trees on open, and {@link #dispose()} is the hook for
 * anything that has to be released earlier (currently: nothing, because every control
 * is procedural - the hook exists so a future texture-backed control cannot leak).
 */
public abstract class UiComponent {

    /** Rectangle in design units. */
    protected int x;
    protected int y;
    protected int width;
    protected int height;

    /** Visible components render and receive input; invisible ones do neither. */
    protected boolean visible = true;

    /**
     * Vertical translation applied while rendering, in design units. Set by a
     * {@link ScrollContainer} so a child draws at its scrolled position while its own
     * stored rectangle (and every hit-test against it) stays in content space.
     */
    protected double renderOffsetY;

    /**
     * Horizontal translation applied while rendering, in design units. A scroll
     * container shifts its children by its own left edge: the children store
     * content-relative x, and this is what puts them inside the viewport.
     */
    protected double renderOffsetX;

    protected UiComponent() {
    }

    protected UiComponent(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    /* ── lifecycle ──────────────────────────────────────────────────────── */

    /** Draws the component. Called once per frame, inside the correct clip. */
    public abstract void render();

    /** The cursor moved. Coordinates are design units, already content-relative. */
    public void onMouseMove(double mouseX, double mouseY) {
    }

    /**
     * A mouse button was pressed over the component.
     *
     * @return true when the component consumed the click
     */
    public boolean onMouseClick(double mouseX, double mouseY, int button) {
        return false;
    }

    /** A mouse button was released. Always delivered to the component that took the click. */
    public void onMouseRelease(double mouseX, double mouseY, int button) {
    }

    /**
     * A key was typed while the component owns the keyboard.
     *
     * @return true when the component consumed the key
     */
    public boolean onKeyTyped(char typedChar, int keyCode) {
        return false;
    }

    /**
     * The wheel turned while the cursor was over the component.
     *
     * @return true when the component consumed the wheel delta
     */
    public boolean onWheel(double mouseX, double mouseY, int delta) {
        return false;
    }

    /** The window or content area changed size; recompute derived geometry. */
    public void onResize() {
    }

    /** Releases anything the component holds. Called when its screen closes. */
    public void dispose() {
    }

    /* ── geometry ───────────────────────────────────────────────────────── */

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public UiComponent at(int x, int y) {
        this.x = x;
        this.y = y;
        return this;
    }

    public UiComponent size(int width, int height) {
        this.width = width;
        this.height = height;
        return this;
    }

    public boolean isVisible() {
        return visible;
    }

    public UiComponent setVisible(boolean visible) {
        this.visible = visible;
        return this;
    }

    /**
     * Sets the translation a container applies while rendering this component (a scrolled list
     * shifts its rows by the scroll offset). Hit tests stay in the component's own coordinates, so
     * the container feeds the cursor through the same translation.
     */
    public UiComponent renderOffset(double x, double y) {
        this.renderOffsetX = x;
        this.renderOffsetY = y;
        return this;
    }

    /** True when the point (design units) is inside this component's rectangle. */
    public boolean contains(double px, double py) {
        return visible && px >= x && px < x + width && py >= y && py < y + height;
    }

    /* ── conversion helpers for implementors ────────────────────────────── */

    /** Left edge in GUI pixels, including any translation from a container. */
    protected final int gx() {
        return GuiScale.x(x + renderOffsetX);
    }

    /**
     * Renders a composed child control (one this component positions itself, such as a
     * toggle inside a settings row) with this component's render translation, so a
     * control nested one level below a scroll container's direct children still lands
     * in the scrolled viewport.
     */
    protected final void renderChild(UiComponent child) {
        child.renderOffsetX = this.renderOffsetX;
        child.renderOffsetY = this.renderOffsetY;
        try {
            child.render();
        } finally {
            child.renderOffsetX = 0.0D;
            child.renderOffsetY = 0.0D;
        }
    }

    /** Top edge in GUI pixels, including any scroll translation from a container. */
    protected final int gy() {
        return GuiScale.y(y + renderOffsetY);
    }

    /** Width in GUI pixels. */
    protected final int gw() {
        return GuiScale.w(width);
    }

    /** Height in GUI pixels. */
    protected final int gh() {
        return GuiScale.h(height);
    }
}
