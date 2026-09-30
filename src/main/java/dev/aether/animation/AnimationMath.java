package dev.aether.animation;

/**
 * Pure interpolation helpers shared by every animated value in the client.
 * <p>
 * These are static and Minecraft-free so the frame-rate independence of the whole animation layer
 * can be proven by a plain unit test ({@code AnimationTest}) instead of by eye at 240 FPS.
 * <p>
 * {@link #approach} is the exponential form: the fraction of the remaining distance covered by a
 * frame is {@code 1 - e^(-dt/rate)}, which is what makes the result depend on <em>elapsed time</em>
 * and not on the number of frames. The classic {@code dt * speed} form used by most clients is
 * frame-rate dependent: at 240 FPS a value advances twice as fast as at 120 FPS.
 */
public final class AnimationMath {

    private AnimationMath() {
    }

    public static float clamp01(float value) {
        if (value < 0.0F) {
            return 0.0F;
        }
        return value > 1.0F ? 1.0F : value;
    }

    public static float clamp(float value, float min, float max) {
        if (value < min) {
            return min;
        }
        return value > max ? max : value;
    }

    /** Linear blend; {@code t} is clamped, so callers can pass an easing output directly. */
    public static float lerp(float from, float to, float t) {
        float amount = clamp01(t);
        return from + (to - from) * amount;
    }

    /** Linear blend in an unbounded domain, for values an easing curve may overshoot. */
    public static float lerpUnclamped(float from, float to, float t) {
        return from + (to - from) * t;
    }

    /**
     * Exponential approach towards a target, one rendered frame's worth.
     * <p>
     * {@code rateMillis} is the time constant: after that much elapsed time the value has covered
     * ~63% of the remaining distance. A non-positive rate snaps, and a non-positive delta makes no
     * progress (a paused frame must not teleport the value).
     */
    public static float approach(float current, float target, float deltaMillis, float rateMillis) {
        if (rateMillis <= 0.0F) {
            return target;
        }
        if (deltaMillis <= 0.0F) {
            return current;
        }
        float alpha = 1.0F - (float) Math.exp(-deltaMillis / rateMillis);
        return current + (target - current) * alpha;
    }

    /** True when a value is close enough to its target that it can be treated as arrived. */
    public static boolean reached(float value, float target) {
        return Math.abs(target - value) < 0.0005F;
    }

    /**
     * ARGB blend between two packed colours. The channels are interpolated independently, which is
     * fine for a press/selection tint where both endpoints are opaque or both already carry alpha.
     */
    public static int lerpColor(int from, int to, float t) {
        float amount = clamp01(t);
        int a = Math.round(((from >> 24) & 0xFF) + (((to >> 24) & 0xFF) - ((from >> 24) & 0xFF)) * amount);
        int r = Math.round(((from >> 16) & 0xFF) + (((to >> 16) & 0xFF) - ((from >> 16) & 0xFF)) * amount);
        int g = Math.round(((from >> 8) & 0xFF) + (((to >> 8) & 0xFF) - ((from >> 8) & 0xFF)) * amount);
        int b = Math.round((from & 0xFF) + ((to & 0xFF) - (from & 0xFF)) * amount);
        return (a & 0xFF) << 24 | (r & 0xFF) << 16 | (g & 0xFF) << 8 | (b & 0xFF);
    }

    /** Replaces a packed colour's alpha channel, for fading a surface by an animated value. */
    public static int withAlpha(int argb, float alpha) {
        int channel = Math.round(clamp01(alpha) * 255.0F);
        return channel << 24 | (argb & 0x00FFFFFF);
    }

    /** Multiplies a packed colour's alpha by an animated 0-1 value. */
    public static int scaleAlpha(int argb, float factor) {
        int current = (argb >>> 24) & 0xFF;
        int channel = Math.round(clamp01(current / 255.0F * clamp01(factor)) * 255.0F);
        return channel << 24 | (argb & 0x00FFFFFF);
    }
}
