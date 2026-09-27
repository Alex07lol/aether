package dev.aether.graphics;

/**
 * The hurt-camera shake for the no hurt cam module.
 * <p>
 * Vanilla's shake is not linear: it ramps in and out over {@code sin(progress^4 * pi)} and rolls
 * the camera up to 14 degrees around the yaw the hit came from. Aether keeps that curve and only
 * scales it, so 100 draws exactly what vanilla draws, 0 removes the shake completely, and the
 * values in between are a real dial. The hurt timers themselves are never rewritten, which is
 * what keeps the damage overlay and any other reader of {@code hurtTime} untouched.
 */
public final class HurtCamMath {
    /** Degrees vanilla rolls and pitches the camera at full strength. */
    public static final float VANILLA_SHAKE_DEGREES = 14.0F;

    private HurtCamMath() {
    }

    /** The module's 0-100 dial as a 0-1 scale. */
    public static float scaleFromPercent(int percent) {
        return clamp((float) percent, 0.0F, 100.0F) / 100.0F;
    }

    /**
     * Vanilla's shake curve, without the module's scaling.
     *
     * @return 0 when the shake is over (or has not started), 1 at the peak of the animation
     */
    public static float curve(int hurtTime, int maxHurtTime, float partialTicks) {
        if (maxHurtTime <= 0 || hurtTime <= 0) {
            return 0.0F;
        }
        float remaining = (float) hurtTime - partialTicks;
        if (remaining <= 0.0F) {
            return 0.0F;
        }
        float progress = Math.min(1.0F, remaining / (float) maxHurtTime);
        return (float) Math.sin(progress * progress * progress * progress * Math.PI);
    }

    /** The rotation vanilla applies around the attack yaw and the X axis, scaled by the dial. */
    public static float shakeDegrees(int hurtTime, int maxHurtTime, float partialTicks, float scale) {
        return curve(hurtTime, maxHurtTime, partialTicks) * VANILLA_SHAKE_DEGREES * clamp(scale, 0.0F, 1.0F);
    }

    /** True when the module's own draw would differ from vanilla's. */
    public static boolean applies(float scale) {
        return scale < 1.0F;
    }

    public static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}
