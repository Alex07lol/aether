package dev.aether.gui.components;

import dev.aether.animation.AnimationMath;
import dev.aether.animation.Easing;
import dev.aether.animation.FrameClock;
import dev.aether.forge189.AetherUi;
import dev.aether.gui.GuiScale;

/**
 * A modern Aether list viewport: fractional scrolling with a frame-rate-independent glide, and a
 * rounded thumb that fades in while the list is being scrolled.
 * <p>
 * The older Leaf-derived screens page a list one screenful at a time through {@code PageBar}, which
 * is exactly what Leaf did. A module list with forty rows in it is better served by a scroll that
 * follows the wheel proportionally and glides to a stop, so this is the component the new screens
 * use - one implementation, shared by the module browser and the cosmetics gallery, rather than each
 * screen growing its own offset field.
 * <p>
 * Nothing here allocates per frame: the offset is two doubles and the thumb is a few rounded rects.
 * {@link #update()} is called once per frame by the screen, which is what makes the glide
 * frame-rate independent, and {@link #wheel(double, double, int)} takes the direction already
 * normalised by {@code AetherGuiScreen} (positive = forward/down), so the user's scroll preference
 * reaches it without the component knowing about preferences at all.
 */
public final class ScrollView {

    /** Height of a row that the wheel moves by default, in design units. */
    private static final double WHEEL_STEP = 52.0D;
    /** Rate of the glide towards the requested offset; ~63% of the distance in this many ms. */
    private static final float GLIDE_MILLIS = 90.0F;
    private static final int BAR_WIDTH = 8;
    private static final int BAR_MARGIN = 4;
    private static final int MIN_THUMB = 32;

    private int x;
    private int y;
    private int width;
    private int height;
    private int contentHeight;

    private double offset;
    private double target;

    private final dev.aether.animation.Anim barAlpha = new dev.aether.animation.Anim(0.0F, 180.0F, Easing.EASE_OUT_QUAD);
    private float idleMillis;
    private boolean dragging;
    private double grabOffset;

    public ScrollView bounds(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        clamp();
        return this;
    }

    /** Tells the view how tall the content is; a change rescales the thumb immediately. */
    public void content(int contentHeight) {
        this.contentHeight = Math.max(0, contentHeight);
        clamp();
    }

    public int contentHeight() {
        return this.contentHeight;
    }

    public int viewportHeight() {
        return this.height;
    }

    /** The furthest the content can travel; 0 when everything fits. */
    public int maxOffset() {
        return Math.max(0, this.contentHeight - this.height);
    }

    public boolean scrollable() {
        return maxOffset() > 0;
    }

    /** The current offset in design units, for positioning rows. */
    public double offset() {
        return this.offset;
    }

    public void setOffset(double value) {
        this.offset = value;
        this.target = value;
        clamp();
    }

    /** Scrolls by a delta in design units; positive moves the content up (further down the list). */
    public void scrollBy(double delta) {
        this.target = this.target + delta;
        clamp();
    }

    /** One wheel notch, in the screen's normalised direction (positive = forward/down). */
    public void wheel(int direction) {
        scrollBy(direction > 0 ? WHEEL_STEP : -WHEEL_STEP);
        poke();
    }

    /** Scrolls so that a row is fully visible; used when a selection moves off screen. */
    public void ensureVisible(double top, double bottom) {
        if (top < this.offset) {
            this.target = top;
            poke();
        } else if (bottom > this.offset + this.height) {
            this.target = bottom - this.height;
            poke();
        }
        clamp();
    }

    /** Whether the cursor is over the viewport, which is what decides who gets the wheel. */
    public boolean contains(double mouseX, double mouseY) {
        return mouseX >= this.x && mouseX <= this.x + this.width
            && mouseY >= this.y && mouseY <= this.y + this.height;
    }

    public boolean onMouseClick(double mouseX, double mouseY, int button) {
        if (button != 0 || !scrollable() || !contains(mouseX, mouseY)) {
            return false;
        }
        int thumbTop = thumbTop();
        int thumbHeight = thumbHeight();
        if (mouseX >= barX() - BAR_MARGIN && mouseX <= barX() + BAR_WIDTH + BAR_MARGIN) {
            if (mouseY >= thumbTop && mouseY <= thumbTop + thumbHeight) {
                this.dragging = true;
                this.grabOffset = mouseY - thumbTop;
            } else {
                // Clicking the track jumps the thumb to the cursor, centred on it.
                this.target = offsetForThumbTop(mouseY - thumbHeight / 2.0D);
                poke();
            }
            clamp();
            return true;
        }
        return false;
    }

    public void onMouseDrag(double mouseX, double mouseY) {
        if (!this.dragging) {
            return;
        }
        this.target = offsetForThumbTop(mouseY - this.grabOffset);
        this.offset = this.target;
        poke();
        clamp();
    }

    public void onMouseRelease() {
        this.dragging = false;
    }

    public boolean isDragging() {
        return this.dragging;
    }

    /** Advances the glide. Call once per frame, before the rows are drawn. */
    public void update() {
        float delta = FrameClock.deltaMillis();
        if (this.dragging) {
            this.offset = this.target;
        } else {
            this.offset = AnimationMath.approach((float) this.offset, (float) this.target, delta, GLIDE_MILLIS);
            if (Math.abs(this.target - this.offset) < 0.25D) {
                this.offset = this.target;
            }
        }
        clamp();
        this.idleMillis = this.barAlpha.target() > 0.5F ? 0.0F : this.idleMillis + delta;
        this.barAlpha.target(this.scrollable() && (this.dragging || this.idleMillis < 900.0F) ? 1.0F : 0.0F);
        this.barAlpha.update();
    }

    /** Keeps the thumb awake for a moment after the last interaction. */
    private void poke() {
        this.idleMillis = 0.0F;
        this.barAlpha.target(1.0F);
    }

    /** Draws the thumb over the content, inside the screen's clip. */
    public void render() {
        float alpha = this.barAlpha.value();
        if (alpha <= 0.01F || !scrollable()) {
            return;
        }
        AetherUi.drawRoundRect(barX(), this.y, barX() + BAR_WIDTH, this.y + this.height, BAR_WIDTH / 2,
            AnimationMath.scaleAlpha(AetherUi.TRACK, alpha * 0.7F));
        int thumbTop = thumbTop();
        int thumbHeight = thumbHeight();
        AetherUi.drawRoundRect(barX(), thumbTop, barX() + BAR_WIDTH, thumbTop + thumbHeight, BAR_WIDTH / 2,
            AnimationMath.scaleAlpha(AetherUi.TEXT_PRIMARY, alpha * 0.45F));
    }

    private int barX() {
        return this.x + this.width + GuiScale.w(BAR_MARGIN);
    }

    private int thumbHeight() {
        int max = maxOffset();
        if (max <= 0) {
            return this.height;
        }
        double ratio = (double) this.height / (double) Math.max(1, this.contentHeight);
        return (int) Math.max(MIN_THUMB, Math.round(this.height * ratio));
    }

    private int thumbTop() {
        int max = maxOffset();
        if (max <= 0) {
            return this.y;
        }
        double travel = this.height - thumbHeight();
        return (int) Math.round(this.y + travel * (this.offset / max));
    }

    private double offsetForThumbTop(double thumbTop) {
        int max = maxOffset();
        if (max <= 0) {
            return 0.0D;
        }
        double travel = this.height - thumbHeight();
        if (travel <= 0.0D) {
            return 0.0D;
        }
        return AnimationMath.clamp((float) ((thumbTop - this.y) / travel), 0.0F, 1.0F) * max;
    }

    private void clamp() {
        int max = maxOffset();
        if (this.target < 0.0D) {
            this.target = 0.0D;
        }
        if (this.target > max) {
            this.target = max;
        }
        if (this.offset < 0.0D) {
            this.offset = 0.0D;
        }
        if (this.offset > max) {
            this.offset = max;
        }
    }

    /** Debug/test view of the state, so a headless test can assert the glide without GL. */
    @Override
    public String toString() {
        return "ScrollView{offset=" + Math.round(this.offset) + ", target=" + Math.round(this.target)
            + ", max=" + maxOffset() + '}';
    }

    /** The rectangle the thumb occupies, as {x, y, width, height}; also the test's view of it. */
    public int[] thumbForTest() {
        return new int[] {barX(), thumbTop(), BAR_WIDTH, thumbHeight()};
    }
}
