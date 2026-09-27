package dev.aether.graphics;

/**
 * The weather the client should draw, as a pure value.
 * <p>
 * The module tells the render layer what rain and thunder strength to report; the world keeps
 * whatever the server said. That is what makes the module reversible with nothing to restore: a
 * disabled module asks for no override at all, and the next frame reads the server's own weather
 * again.
 * <p>
 * Snow is deliberately not a mode. Which biome snows is a biome property, so "make it snow" cannot
 * be expressed as a strength - supporting it would mean rewriting biome temperature per frame, which
 * is exactly the kind of world mutation this module exists to avoid.
 */
public final class WeatherValues {

    public enum Mode {
        /** Draw the server's weather, i.e. no override at all. */
        SERVER,
        /** Clear skies. */
        CLEAR,
        /** Rain, no lightning. */
        RAIN,
        /** Rain and lightning. */
        STORM;

        /** @return the mode named by {@code value}, or {@code fallback} when unknown or null. */
        public static Mode from(String value, Mode fallback) {
            if (value == null) {
                return fallback;
            }
            String trimmed = value.trim();
            for (Mode mode : values()) {
                if (mode.name().equalsIgnoreCase(trimmed)) {
                    return mode;
                }
            }
            return fallback;
        }
    }

    private WeatherValues() {
    }

    /**
     * @param rainStrength the configured rain strength as a percentage (0-100)
     * @return the rain strength to report, or {@code null} when the server's value must be used
     */
    public static Float rainStrength(Mode mode, int rainStrength) {
        if (mode == null || mode == Mode.SERVER) {
            return null;
        }
        if (mode == Mode.CLEAR) {
            return Float.valueOf(0.0F);
        }
        return Float.valueOf(percent(rainStrength));
    }

    /**
     * @param thunderStrength the configured thunder strength as a percentage (0-100)
     * @return the thunder strength to report, or {@code null} when the server's value must be used
     */
    public static Float thunderStrength(Mode mode, int thunderStrength) {
        if (mode == null || mode == Mode.SERVER) {
            return null;
        }
        if (mode == Mode.CLEAR || mode == Mode.RAIN) {
            // Plain rain never flashes; that is the difference between the two modes.
            return Float.valueOf(0.0F);
        }
        return Float.valueOf(percent(thunderStrength));
    }

    /** True when the mode changes anything at all. */
    public static boolean overrides(Mode mode) {
        return mode != null && mode != Mode.SERVER;
    }

    private static float percent(int value) {
        int clamped = Math.max(0, Math.min(100, value));
        return clamped / 100.0F;
    }
}
