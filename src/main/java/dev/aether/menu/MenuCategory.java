package dev.aether.menu;

import dev.aether.AetherClient;
import dev.aether.animation.FrameClock;
import dev.aether.ui.UiCanvas;

/**
 * One routed destination inside the menu window: Mods, Cosmetics, Appearance,
 * Profiles, Settings or Home. The category owns its content coordinates (the area
 * right of the 32px rail), its scroll offset and its transient state; the screen
 * routes draw/input and supplies the geometry.
 * <p>
 * Coordinates are GUI-scale pixels throughout. {@link #contentX()}/{@link #contentY()}
 * are the absolute origin of the content area; subclasses lay out from there with the
 * grammar's 15px margin. Virtual lifecycle: draw, then input, plus key and wheel.
 */
public abstract class MenuCategory {

    protected final AetherMenuScreen screen;
    protected final AetherClient client;

    /** Scroll offset in GUI pixels, animated towards {@code scrollTarget}. */
    protected float scroll;
    protected float scrollTarget;
    /** How far the content can scroll (computed per frame by the category). */
    protected float maxScroll;
    /** Rise+fade of the content when the category becomes active. */
    protected float transition;

    protected MenuCategory(AetherMenuScreen screen, AetherClient client) {
        this.screen = screen;
        this.client = client;
    }

    /* ── geometry (GUI pixels) ──────────────────────────────────────────── */

    public float windowX() {
        return screen.windowX();
    }

    public float windowY() {
        return screen.windowY();
    }

    public float windowW() {
        return screen.windowW();
    }

    public float windowH() {
        return screen.windowH();
    }

    /** Absolute left edge of the content area (right of the rail). */
    public float contentX() {
        return screen.windowX() + AetherMenuScreen.RAIL_W;
    }

    public float contentY() {
        return screen.windowY() + AetherMenuScreen.HEADER_H;
    }

    public float contentW() {
        return screen.windowW() - AetherMenuScreen.RAIL_W;
    }

    public float contentH() {
        return screen.windowH() - AetherMenuScreen.HEADER_H;
    }

    /** The 15px inner margin the grammar lays content out from. */
    public float innerX() {
        return contentX() + 15.0F;
    }

    public float innerW() {
        return contentW() - 30.0F;
    }

    /** True when the point (GUI pixels) is inside the content area. */
    public boolean insideContent(double mx, double my) {
        return mx >= contentX() && mx < contentX() + contentW()
            && my >= contentY() && my < contentY() + contentH();
    }

    /* ── scroll ─────────────────────────────────────────────────────────── */

    /** Recomputes {@code maxScroll} from the laid-out content height. */
    protected void setContentHeight(float height) {
        this.maxScroll = Math.max(0.0F, height - contentH());
    }

    protected void updateScroll() {
        float delta = Math.abs(scrollTarget - scroll);
        if (delta < 0.01F) {
            scroll = scrollTarget;
        } else {
            scroll += (scrollTarget - scroll) * Math.min(1.0F,
                1.0F - (float) Math.exp(-FrameClock.deltaMillis() / 70.0F));
        }
        scrollTarget = Math.max(0.0F, Math.min(maxScroll, scrollTarget));
        scroll = Math.max(0.0F, Math.min(maxScroll, scroll));
    }

    public void onWheel(double mx, double my, int delta) {
        scrollTarget = Math.max(0.0F, Math.min(maxScroll, scrollTarget - delta * 22.0F));
    }

    /** Clamps the scroll after the content shrinks (a filter change, for instance). */
    protected void clampScroll() {
        scrollTarget = Math.max(0.0F, Math.min(maxScroll, scrollTarget));
        scroll = Math.max(0.0F, Math.min(maxScroll, scroll));
    }

    /* ── lifecycle ──────────────────────────────────────────────────────── */

    /** Called when the category becomes the active one. */
    public void onShow() {
        this.transition = 0.0F;
        this.scroll = 0.0F;
        this.scrollTarget = 0.0F;
    }

    /** Advances animations; called before draw every frame. */
    public void update() {
        this.transition = Math.min(1.0F, this.transition + FrameClock.deltaMillis() / 220.0F);
        updateScroll();
    }

    /** Draws the category content inside the screen's content clip. */
    public abstract void draw(double mx, double my);

    /**
     * A click inside the content area.
     *
     * @return true when the click was consumed
     */
    public boolean click(double mx, double my, int button) {
        return true;
    }

    /** A mouse release. */
    public void release(double mx, double my, int button) {
    }

    /** A key press while the category is active.
     *
     * @return true when the key was consumed
     */
    public boolean key(char typedChar, int keyCode) {
        return false;
    }

    /** Draws the category's header extras (search field, buttons) in the window header. */
    public void drawHeaderExtras(double mx, double my) {
    }

    /** Header-area click handling; the screen calls this before routing to content. */
    public boolean clickHeaderExtras(double mx, double my, int button) {
        return false;
    }

    /** Key handling for header controls (a focused search field). */
    public boolean keyHeaderExtras(char typedChar, int keyCode) {
        return false;
    }

    /** @return true while any control inside this category captures the keyboard. */
    public boolean capturesKeyboard() {
        return false;
    }

    /** Steps back one view (a detail scene); @return true when something stepped back. */
    public boolean stepBack() {
        return false;
    }

    /** Releases anything the category holds when the menu closes. */
    public void dispose() {
    }

    /** Convenience: opens a rounded clip over the content area for scrolled drawing. */
    protected void clipContent() {
        UiCanvas.scissor(contentX(), contentY(), contentW(), contentH());
    }

    /** The title the header shows for this category. */
    public abstract String title();

    /** Whether the header shows the search field for this category. */
    public boolean hasSearch() {
        return false;
    }
}
