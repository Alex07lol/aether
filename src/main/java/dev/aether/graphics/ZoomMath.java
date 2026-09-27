package dev.aether.graphics;

/**
 * Zoom maths for the zoom module.
 * <p>
 * The module scales the field of view <em>while it is being computed</em> instead of writing a
 * value into {@code GameSettings.fovSetting}: the user's own FOV slider is never touched, so
 * there is nothing to restore, and another mod that changes the FOV is scaled rather than lost.
 * This class holds the numbers that decision needs - the percentage to multiplier conversion,
 * the scroll bounds and the smoothing step - so they can be tested without Minecraft.
 */
public final class ZoomMath {
    /** The dial is a percentage of the unzoomed FOV, so 100 means "do not zoom at all". */
    public static final int FULL_PERCENT = 100;

    /** Scale applied when the module is idle; multiplying the FOV by 1 leaves it alone. */
    public static final float NO_ZOOM = 1.0F;

    private ZoomMath() {
    }

    /**
     * The FOV multiplier for a zoom percentage.
     *
     * @param percent    the configured zoom percentage
     * @param minPercent the setting's own declared floor, so the confidence interval of the
     *                   slider and the maths can never disagree
     */
    public static float scaleFromPercent(int percent, int minPercent) {
        int floor = Math.max(1, Math.min(minPercent, FULL_PERCENT));
        return clamp((float) percent, floor, FULL_PERCENT) / 100.0F;
    }

    /** A single scroll notch: {@code direction} is +1 or -1, result stays inside the bounds. */
    public static int scrollTarget(int current, int direction, int step, int min, int max) {
        int low = Math.min(min, max);
        int high = Math.max(min, max);
        int size = Math.max(1, step);
        return Math.max(low, Math.min(high, current + direction * size));
    }

    /**
     * Exponential approach towards {@code target}, one step per rendered frame.
     * <p>
     * {@code settleMillis} is the time constant, not a hard deadline: the scale gets to ~63% of
     * the way there after that long and keeps closing the gap, which is what makes a released
     * zoom glide back instead of snapping. Any non-positive delta (a paused frame, a clock that
     * did not move) is treated as "no progress" rather than "finish now".
     */
    public static float smooth(float current, float target, float deltaMillis, float settleMillis) {
        if (settleMillis <= 0.0F) {
            return target;
        }
        if (deltaMillis <= 0.0F) {
            return current;
        }
        float alpha = clamp(deltaMillis / settleMillis, 0.0F, 1.0F);
        return current + (target - current) * alpha;
    }

    /** True when the animation has effectively arrived, which lets the hook stop working. */
    public static boolean settled(float current, float target) {
        return Math.abs(current - target) < 0.0005F;
    }

    public static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}
