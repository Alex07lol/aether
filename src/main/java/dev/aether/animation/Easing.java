package dev.aether.animation;

/**
 * Easing curves for the shared animation layer.
 * <p>
 * Every curve is a pure function of {@code t} in {@code [0,1]} and returns a value that starts at
 * {@code 0} and ends at {@code 1} (the two {@code BACK} curves deliberately overshoot past the end
 * on the way, which is what makes a widget feel like it snaps into place).
 * <p>
 * The set is the one the ported Soar behaviours need: {@code EASE_OUT_*} for enter/press
 * animations, {@code EASE_IN_OUT_CUBIC} for page and layout transitions, {@code EASE_OUT_BACK} for
 * target/mouse indicators, {@code LINEAR} for anything driven directly by a user setting.
 */
public enum Easing {
    /** No easing; the value tracks the animation's progress exactly. */
    LINEAR,
    /** Accelerating start, no deceleration. */
    EASE_IN_QUAD,
    /** Fast start, easing out. The default for a release. */
    EASE_OUT_QUAD,
    /** Fast start, softer landing than quad. */
    EASE_OUT_CUBIC,
    /** Fast start, very soft landing. Used for enter/exit of HUD widgets. */
    EASE_OUT_QUART,
    /** Slow at both ends. Used for page and layout transitions. */
    EASE_IN_OUT_CUBIC,
    /** Almost instant then a long settle. */
    EASE_OUT_EXPO,
    /** Overshoots past the target once. */
    EASE_OUT_BACK,
    /** Pulls back before moving forward. */
    EASE_IN_BACK;

    /** The back curves' overshoot amount (the classic 1.70158 constant). */
    private static final float OVERSHOOT = 1.70158F;

    public float apply(float t) {
        float x = AnimationMath.clamp01(t);
        switch (this) {
            case EASE_IN_QUAD:
                return x * x;
            case EASE_OUT_QUAD:
                return 1.0F - (1.0F - x) * (1.0F - x);
            case EASE_OUT_CUBIC: {
                float inverse = 1.0F - x;
                return 1.0F - inverse * inverse * inverse;
            }
            case EASE_OUT_QUART: {
                float inverse = 1.0F - x;
                return 1.0F - inverse * inverse * inverse * inverse;
            }
            case EASE_IN_OUT_CUBIC:
                return x < 0.5F
                    ? 4.0F * x * x * x
                    : 1.0F - (float) Math.pow(-2.0F * x + 2.0F, 3.0D) / 2.0F;
            case EASE_OUT_EXPO:
                return x >= 1.0F ? 1.0F : 1.0F - (float) Math.pow(2.0D, -10.0D * x);
            case EASE_OUT_BACK: {
                float shifted = x - 1.0F;
                return 1.0F + (OVERSHOOT + 1.0F) * shifted * shifted * shifted
                    + OVERSHOOT * shifted * shifted;
            }
            case EASE_IN_BACK:
                return (OVERSHOOT + 1.0F) * x * x * x - OVERSHOOT * x * x;
            case LINEAR:
            default:
                return x;
        }
    }

    /** True for the two curves that leave {@code [0,1]}; useful when clamping a drawn size. */
    public boolean overshoots() {
        return this == EASE_OUT_BACK || this == EASE_IN_BACK;
    }
}
