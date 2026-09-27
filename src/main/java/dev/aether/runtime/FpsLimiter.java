package dev.aether.runtime;

/**
 * The framerate the client should ask for, in vanilla's own {@code limitFramerate} units.
 * <p>
 * Minecraft already knows how to honour a frame cap: the "Max Framerate" video setting is read by the
 * game loop, which sleeps the remainder of the frame. Rather than inventing a second limiter (and a
 * second way to get the timing wrong), the module captures that one setting, writes the cap the
 * player configured, and puts the original value back when it is switched off.
 * <p>
 * Vanilla expresses "unlimited" as the top of its slider rather than as 0, which is why the value is
 * resolved here instead of at the call site.
 */
public final class FpsLimiter {
    /** Vanilla's slider value that means "no cap". */
    public static final int UNLIMITED = 260;

    /** Vanilla's slider floor; anything lower would be a frame cap nobody asked for. */
    public static final int MINIMUM = 10;

    private FpsLimiter() {
    }

    /**
     * @param targetFps    the configured gameplay cap, 0 for unlimited
     * @param unfocusedFps the configured cap while the window is not focused, 0 to follow the target
     * @param focused      whether the game window currently has focus
     * @return the value to write to the framerate setting
     */
    public static int resolve(int targetFps, int unfocusedFps, boolean focused) {
        int chosen = !focused && unfocusedFps > 0 ? unfocusedFps : targetFps;
        if (chosen <= 0) {
            // 0 means "no cap", whether the gameplay cap or the unfocused one asked for it.
            return UNLIMITED;
        }
        return Math.max(MINIMUM, Math.min(UNLIMITED, chosen));
    }

    /** True when the resolved value asks vanilla for no cap at all. */
    public static boolean isUnlimited(int framerate) {
        return framerate >= UNLIMITED;
    }
}
