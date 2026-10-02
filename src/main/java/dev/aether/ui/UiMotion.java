package dev.aether.ui;

import dev.aether.animation.FrameClock;

/**
 * The menu's motion primitive: an exponentially smoothed value, the feel the reference
 * client gets from its {@code SimpleAnimation} speeds (14-20 for hover/selection
 * movement, 14 for scroll). Smoothing runs through {@link FrameClock} deltas, so the
 * motion is identical at 30 and 240 FPS.
 * <p>
 * The speed parameter maps directly: a higher number settles faster, with 15 being
 * the default "the pill glides to the selected slot" feel.
 */
public final class UiMotion {

    private float value;
    private float target;
    private float speed;

    public UiMotion(float initial) {
        this(initial, 15.0F);
    }

    public UiMotion(float initial, float speed) {
        this.value = initial;
        this.target = initial;
        this.speed = speed;
    }

    public UiMotion speed(float speed) {
        this.speed = Math.max(1.0F, speed);
        return this;
    }

    /** Sets the destination the value glides towards. */
    public UiMotion target(float target) {
        this.target = target;
        return this;
    }

    /** Snaps without animating (first layout, resets). */
    public UiMotion snap(float value) {
        this.value = value;
        this.target = value;
        return this;
    }

    /** Advances the smoothing by the frame delta and returns the current value. */
    public float update() {
        float delta = FrameClock.deltaMillis();
        if (Math.abs(target - value) < 0.001F) {
            value = target;
            return value;
        }
        // Exponential smoothing with a half-life derived from the speed: speed 15
        // settles in roughly a 60ms half-life, matching the reference's glide.
        float halfLife = 900.0F / speed;
        float factor = 1.0F - (float) Math.exp(-delta / halfLife);
        value += (target - value) * Math.min(1.0F, factor);
        return value;
    }

    public float value() {
        return value;
    }

    public float target() {
        return target;
    }

    public boolean settled() {
        return Math.abs(target - value) < 0.001F;
    }
}
