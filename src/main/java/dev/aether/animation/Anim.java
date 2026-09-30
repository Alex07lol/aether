package dev.aether.animation;

/**
 * A persistent animated scalar: a value that travels towards a target over a configured duration
 * with a configured easing curve.
 * <p>
 * This is the Aether equivalent of the little per-feature animation objects Soar v4 keeps in its
 * mixins ({@code SimpleAnimation}, the per-key keystroke state, the hotbar selector, the block-info
 * appearance). The rules it enforces are the ones the brief calls out explicitly:
 * <ul>
 *   <li><b>Frame-rate independent.</b> Progress comes from {@link FrameClock}'s elapsed time, so
 *       the same animation takes the same wall-clock time at 30, 60, 120 or 240 FPS.</li>
 *   <li><b>No per-frame allocation.</b> An {@code Anim} is a field on whoever draws the value;
 *       {@link #update()} returns a primitive and creates nothing.</li>
 *   <li><b>No double stepping.</b> {@link #update()} advances at most once per frame (guarded by
 *       the frame index), so two callers in one frame cannot make an animation run at double
 *       speed.</li>
 *   <li><b>Interruptible.</b> Re-targeting mid-flight restarts from the current value, so a
 *       release during a press continues from where the press got to instead of snapping.</li>
 * </ul>
 * Setting {@code durationMillis} to {@code 0} makes the animation snap, which is how a module's
 * "Animation: off" or "Fade Time: 0" setting is honoured without a second code path.
 */
public final class Anim {

    private float value;
    private float origin;
    private float target;
    private float durationMillis;
    private Easing easing;
    private float progress = 1.0F;
    private long lastFrameIndex = -1L;

    public Anim(float initialValue) {
        this(initialValue, 0.0F, Easing.LINEAR);
    }

    public Anim(float initialValue, float durationMillis, Easing easing) {
        this.value = initialValue;
        this.origin = initialValue;
        this.target = initialValue;
        this.durationMillis = Math.max(0.0F, durationMillis);
        this.easing = easing == null ? Easing.LINEAR : easing;
    }

    /* ---------------------------------------------------------------- configuration */

    /** Travel time in milliseconds. Read from the owning module's own setting. */
    public Anim duration(float millis) {
        this.durationMillis = Math.max(0.0F, millis);
        return this;
    }

    public float duration() {
        return durationMillis;
    }

    public Anim easing(Easing next) {
        this.easing = next == null ? Easing.LINEAR : next;
        return this;
    }

    public Easing easing() {
        return easing;
    }

    /* --------------------------------------------------------------------- targeting */

    /** Jumps to a value with no animation. Used on enable/disable and on state resets. */
    public void set(float value) {
        this.value = value;
        this.origin = value;
        this.target = value;
        this.progress = 1.0F;
    }

    /**
     * Starts travelling towards {@code nextTarget} from the current value.
     * <p>
     * Re-targeting the value it is already travelling to is a no-op: a consumer that re-asserts its
     * target every frame ("held" keys do) must not restart the travel every frame, which would turn
     * a fixed-duration animation into an exponential decay that never actually arrives.
     */
    public void target(float nextTarget) {
        if (this.target == nextTarget) {
            return;
        }
        this.origin = this.value;
        this.target = nextTarget;
        this.progress = this.origin == nextTarget ? 1.0F : 0.0F;
        if (this.durationMillis <= 0.0F) {
            this.value = nextTarget;
            this.progress = 1.0F;
        }
    }

    /** Travels back towards the value the current animation started from (the "reverse" case). */
    public void reverse() {
        target(this.origin);
    }

    /** Retargets and forces the next {@link #update} to recompute from the new origin. */
    public void retarget(float nextTarget) {
        this.target = nextTarget;
    }

    /* ----------------------------------------------------------------------- reading */

    public float value() {
        return value;
    }

    public float target() {
        return target;
    }

    /** 0 at the start of the current travel, 1 once the target is reached. */
    public float progress() {
        return progress;
    }

    public boolean settled() {
        return progress >= 1.0F;
    }

    public boolean rising() {
        return target > origin;
    }

    /* -------------------------------------------------------------------- advancing */

    /** Advances by this frame's elapsed time and returns the new value. */
    public float update() {
        long frame = FrameClock.frameIndex();
        if (frame == lastFrameIndex) {
            // Already stepped this frame; another caller must not double the speed.
            return value;
        }
        lastFrameIndex = frame;
        return update(FrameClock.deltaMillis());
    }

    /**
     * Explicit-delta variant, for tests and for code that owns its own clock (a screen that wants
     * to animate while the game loop is paused, for example).
     */
    public float update(float deltaMillis) {
        if (progress >= 1.0F) {
            value = target;
            return value;
        }
        if (durationMillis <= 0.0F) {
            value = target;
            progress = 1.0F;
            return value;
        }
        if (deltaMillis > 0.0F) {
            progress += deltaMillis / durationMillis;
            if (progress >= 1.0F) {
                progress = 1.0F;
            }
        }
        float eased = easing.overshoots()
            ? AnimationMath.lerpUnclamped(origin, target, easing.apply(progress))
            : AnimationMath.lerp(origin, target, easing.apply(progress));
        value = progress >= 1.0F ? target : eased;
        return value;
    }

    @Override
    public String toString() {
        return "Anim{value=" + value + ", target=" + target + ", progress=" + progress
            + ", duration=" + durationMillis + ", easing=" + easing + "}";
    }
}
