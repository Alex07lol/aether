package dev.aether.gui.components;

import dev.aether.animation.Anim;
import dev.aether.animation.AnimationMath;
import dev.aether.animation.Easing;
import dev.aether.animation.FrameClock;
import dev.aether.forge189.AetherUi;
import dev.aether.gui.AetherFont;
import dev.aether.gui.GuiScale;
import dev.aether.gui.core.UiComponent;
import dev.aether.module.ClientModule.ModuleCategory;

/**
 * One module in the browser: a rounded 40-unit row with a 28-unit icon tile, the module's name and
 * one-line description, and the two controls on the right - the on/off switch and, when the module
 * has settings, the gear that opens them (the shape Glide's module list uses, rebuilt on Aether's
 * own components and palette).
 * <p>
 * Every state is animated from a frame delta rather than stepped from a tick handler, and the
 * durations are short enough to feel like response rather than decoration: hover 120 ms, the on/off
 * transition 200 ms, the press 90 ms. Rows also appear with a small staggered flourish when a page
 * or a category changes, which is what keeps the list from snapping when the filter changes.
 * <p>
 * The row is presentation only. What "on" means, and what opening the settings does, is supplied by
 * the screen as two callbacks, so no module logic can leak into the component.
 */
public final class ModuleRow extends UiComponent {

    public static final int ROW_HEIGHT = 40;
    public static final int ROW_RADIUS = 8;
    public static final int ROW_PITCH = 46;
    private static final int ICON_SIZE = 28;
    private static final int ICON_RADIUS = 6;
    private static final int GEAR_SIZE = 24;
    private static final int SWITCH_WIDTH = 34;
    private static final int SWITCH_HEIGHT = 18;
    private static final float APPEAR_MILLIS = 180.0F;
    private static final float APPEAR_STAGGER_MILLIS = 18.0F;

    private final String name;
    private final String description;
    private final ModuleCategory category;
    private final boolean hasSettings;
    private final Runnable onToggle;
    private final Runnable onOpenSettings;

    private final Anim hover = new Anim(0.0F, 120.0F, Easing.EASE_OUT_QUAD);
    private final Anim enabled = new Anim(0.0F, 200.0F, Easing.EASE_OUT_CUBIC);
    private final Anim press = new Anim(0.0F, 90.0F, Easing.EASE_OUT_QUAD);
    private final Anim gearHover = new Anim(0.0F, 110.0F, Easing.EASE_OUT_QUAD);
    private final Anim appear = new Anim(0.0F, APPEAR_MILLIS, Easing.EASE_OUT_CUBIC);

    private float appearDelay;
    private boolean gearHovered;
    private boolean pressed;
    private int index;

    public ModuleRow(String name, String description, ModuleCategory category, boolean enabled,
                     boolean hasSettings, Runnable onToggle, Runnable onOpenSettings) {
        this.name = name == null ? "" : name;
        this.description = description == null ? "" : description;
        this.category = category;
        this.hasSettings = hasSettings;
        this.onToggle = onToggle;
        this.onOpenSettings = onOpenSettings;
        this.enabled.set(enabled ? 1.0F : 0.0F);
        size(0, ROW_HEIGHT);
    }

    /** Places the row and starts its entrance animation, staggered by list position. */
    public ModuleRow place(int x, int y, int width, int index, boolean animated) {
        at(x, y).size(width, ROW_HEIGHT);
        this.index = index;
        this.appearDelay = animated ? index * APPEAR_STAGGER_MILLIS : 0.0F;
        if (!animated) {
            this.appear.set(1.0F);
        }
        return this;
    }

    /** Updates the animated state. Called once per frame by the screen. */
    public void update() {
        if (this.appear.value() < 1.0F) {
            if (this.appearDelay > 0.0F) {
                this.appearDelay -= FrameClock.deltaMillis();
            } else {
                this.appear.target(1.0F);
                this.appear.update();
            }
        }
        this.hover.update();
        this.enabled.update();
        this.press.update();
        this.gearHover.update();
    }

    @Override
    public void render() {
        // Entrance: the row slides up a few units and fades in, so a filter change reads as a
        // rearrangement rather than a repaint.
        float appearValue = this.appear.value();
        int slide = Math.round((1.0F - appearValue) * 8.0F);
        int left = gx();
        int top = gy() + slide;
        int right = left + gw();
        int bottom = top + gh();

        float on = this.enabled.value();
        float hot = this.hover.value();
        float pressedAmount = this.press.value();

        int surface = AetherUi.blend(AetherUi.CARD, AetherUi.CARD_HOVER, hot);
        surface = AetherUi.blend(surface, AetherUi.PANEL, pressedAmount * 0.5F);
        AetherUi.drawRoundRect(left, top, right, bottom, ROW_RADIUS,
            AnimationMath.scaleAlpha(surface, appearValue * (0.92F + hot * 0.08F)));

        // The on state is a wash plus a bar on the leading edge: colour enough to read at a glance,
        // never bright enough to fight the black glass.
        if (on > 0.01F || hot > 0.01F) {
            AetherUi.drawRoundRect(left, top, right, bottom, ROW_RADIUS,
                AnimationMath.scaleAlpha(AetherUi.TEXT_PRIMARY, appearValue * (on * 0.06F + hot * 0.04F)));
        }
        if (on > 0.01F) {
            AetherUi.drawRoundRect(left + 4, top + 8, left + 7, bottom - 8, 2,
                AnimationMath.scaleAlpha(AetherUi.ACCENT, appearValue * on));
        }

        drawIcon(left, top, appearValue, on, hot);
        drawText(left, top, appearValue, on, hot);
        drawSwitch(right, top, appearValue, on);
        if (this.hasSettings) {
            drawGear(right, top, appearValue, hot);
        }
    }

    private void drawIcon(int left, int top, float appearValue, float on, float hot) {
        int iconLeft = left + 6;
        int iconTop = top + (ROW_HEIGHT - ICON_SIZE) / 2;
        int fill = AetherUi.blend(AetherUi.CARD_HOVER, AetherUi.PANEL, 1.0F - Math.max(on, hot * 0.6F));
        AetherUi.drawRoundRect(iconLeft, iconTop, iconLeft + ICON_SIZE, iconTop + ICON_SIZE, ICON_RADIUS,
            AnimationMath.scaleAlpha(fill, appearValue * 0.9F));
        int glyphColor = AetherUi.blend(AetherUi.TEXT_SECONDARY, AetherUi.TEXT_PRIMARY, Math.max(on, hot));
        AetherUi.drawModuleGlyph(this.category, iconLeft + 6, iconTop + 6, 16,
            AnimationMath.scaleAlpha(glyphColor, appearValue));
    }

    private void drawText(int left, int top, float appearValue, float on, float hot) {
        int textX = left + 42;
        int titleY = top + 5;
        String shownName = AetherFont.trimTo(AetherFont.Size.BODY, this.name,
            gw() - 42 - SWITCH_WIDTH - GEAR_SIZE - GuiScale.w(30));
        int titleColor = AetherUi.blend(AetherUi.TEXT_SECONDARY, AetherUi.TEXT_PRIMARY, Math.max(on, hot));
        AetherFont.draw(AetherFont.Size.BODY, shownName, textX, titleY,
            AnimationMath.scaleAlpha(titleColor, appearValue));
        AetherFont.draw(AetherFont.Size.CAPTION, AetherFont.trimTo(AetherFont.Size.CAPTION, this.description,
            gw() - 42 - SWITCH_WIDTH - GEAR_SIZE - GuiScale.w(30)), textX, top + 23,
            AnimationMath.scaleAlpha(AetherUi.TEXT_DISABLED, appearValue * 0.9F));
    }

    private void drawSwitch(int right, int top, float appearValue, float on) {
        int switchRight = right - (this.hasSettings ? GEAR_SIZE + 22 : 12);
        int switchLeft = switchRight - SWITCH_WIDTH;
        int switchTop = top + (ROW_HEIGHT - SWITCH_HEIGHT) / 2;
        int track = AetherUi.blend(AetherUi.TRACK, AetherUi.ACCENT, on);
        AetherUi.drawRoundRect(switchLeft, switchTop, switchRight, switchTop + SWITCH_HEIGHT, SWITCH_HEIGHT / 2,
            AnimationMath.scaleAlpha(track, appearValue));
        int knobRadius = 7;
        float travel = (SWITCH_WIDTH - knobRadius * 2 - 4) * on;
        int knobX = (int) Math.round(switchLeft + 2 + knobRadius + travel);
        int knobY = switchTop + SWITCH_HEIGHT / 2;
        int knob = AetherUi.blend(AetherUi.TEXT_DISABLED, AetherUi.SURFACE, on);
        AetherUi.drawCircle(knobX, knobY, knobRadius, AnimationMath.scaleAlpha(knob, appearValue));
    }

    private void drawGear(int right, int top, float appearValue, float hot) {
        int gearRight = right - 8;
        int gearLeft = gearRight - GEAR_SIZE;
        int gearTop = top + (ROW_HEIGHT - GEAR_SIZE) / 2;
        float emphasis = Math.max(this.gearHover.value(), hot * 0.4F);
        AetherUi.drawRoundRect(gearLeft, gearTop, gearRight, gearTop + GEAR_SIZE, 6,
            AnimationMath.scaleAlpha(AetherUi.TEXT_PRIMARY, appearValue * emphasis * 0.12F));
        AetherUi.drawGearGlyph(gearLeft + 6, gearTop + 6, 12,
            AnimationMath.scaleAlpha(AetherUi.blend(AetherUi.TEXT_DISABLED, AetherUi.TEXT_PRIMARY, emphasis),
                appearValue));
    }

    @Override
    public void onMouseMove(double mouseX, double mouseY) {
        this.hover.target(contains(mouseX, mouseY) ? 1.0F : 0.0F);
        boolean overGear = this.hasSettings && gearRect().contains(mouseX, mouseY);
        this.gearHover.target(overGear ? 1.0F : 0.0F);
    }

    @Override
    public boolean onMouseClick(double mouseX, double mouseY, int button) {
        if (button != 0 || !contains(mouseX, mouseY)) {
            return false;
        }
        this.pressed = true;
        this.press.set(0.0F);
        this.press.target(1.0F);
        if (this.hasSettings && gearRect().contains(mouseX, mouseY)) {
            if (this.onOpenSettings != null) {
                this.onOpenSettings.run();
            }
            return true;
        }
        if (this.onToggle != null) {
            this.onToggle.run();
        }
        return true;
    }

    @Override
    public void onMouseRelease(double mouseX, double mouseY, int button) {
        if (this.pressed) {
            this.pressed = false;
            this.press.target(0.0F);
        }
    }

    /** Animates the switch back instead of snapping when the state changes elsewhere. */
    public void setEnabled(boolean enabled) {
        this.enabled.target(enabled ? 1.0F : 0.0F);
    }

    /** Wipes the row's own animations before the screen throws it away. */
    @Override
    public void dispose() {
        this.hover.retarget(0.0F);
    }

    /**
     * The gear's rectangle in content space, matching how the mouse is dispatched into a scrolled
     * list: the screen translates the cursor by the scroll offset, so hit tests stay in the same
     * coordinates as the row's own rectangle (the pixel conversion in {@code gx()} is for drawing).
     */
    private Rect gearRect() {
        int gearRight = getX() + getWidth() - 8;
        return new Rect(gearRight - GEAR_SIZE, getY() + (ROW_HEIGHT - GEAR_SIZE) / 2, GEAR_SIZE, GEAR_SIZE);
    }

    /** List position of the row, so a screen can map a click back to its module. */
    public int index() {
        return this.index;
    }

    /** Small hit rectangle holder, kept out of the allocation path by being tiny and short-lived. */
    private static final class Rect {
        private final int x;
        private final int y;
        private final int width;
        private final int height;

        Rect(int x, int y, int width, int height) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }

        boolean contains(double px, double py) {
            return px >= this.x && px <= this.x + this.width && py >= this.y && py <= this.y + this.height;
        }
    }
}
