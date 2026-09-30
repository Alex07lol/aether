package dev.aether.animation;

/**
 * The single clock every client animation reads.
 * <p>
 * Minecraft 1.8.9 runs its game loop twice at two different rates: {@code runGameLoop} calls
 * {@code runTick()} once per <em>game tick</em> (~20 Hz) and {@code updateCameraAndRender} once per
 * <em>frame</em>. Anything visual must be driven by frame time, so {@link #beginFrame()} is called
 * once per rendered frame from the Forge render-tick event and every {@link Anim} reads the delta
 * published here.
 * <p>
 * This exists so that no renderer has to call {@code System.nanoTime()} itself: one clock means one
 * delta per frame, which is what makes an animation at 240 FPS take exactly as long as the same
 * animation at 30 FPS, and it makes the frame index available for {@link Anim}'s
 * "advance at most once per frame" guard.
 */
public final class FrameClock {

    /** Delta used before the first frame is measured; one 60 FPS frame. */
    public static final float FIRST_FRAME_MILLIS = 1000.0F / 60.0F;

    /**
     * Upper bound for a single frame's delta. A hitch (saving the world, a garbage collection, the
     * window being dragged) must not make every animation jump; anything longer is treated as one
     * long frame of this length instead.
     */
    public static final float MAX_FRAME_MILLIS = 250.0F;

    private static long frameIndex;
    private static long lastFrameNanos;
    private static float deltaMillis = FIRST_FRAME_MILLIS;

    private FrameClock() {
    }

    /** Publishes the delta for a new frame. Called exactly once per rendered frame. */
    public static void beginFrame() {
        long now = System.nanoTime();
        if (lastFrameNanos == 0L) {
            deltaMillis = FIRST_FRAME_MILLIS;
        } else {
            float elapsed = (now - lastFrameNanos) / 1000000.0F;
            deltaMillis = AnimationMath.clamp(elapsed, 0.0F, MAX_FRAME_MILLIS);
        }
        lastFrameNanos = now;
        frameIndex++;
    }

    /** Milliseconds since the previous frame, already clamped against hitches. */
    public static float deltaMillis() {
        return deltaMillis;
    }

    /** Seconds since the previous frame; the unit most easing-style maths wants. */
    public static float deltaSeconds() {
        return deltaMillis / 1000.0F;
    }

    /** Monotonic frame counter; {@link Anim} uses it to advance at most once per frame. */
    public static long frameIndex() {
        return frameIndex;
    }

    /** Forces the given delta, for tests and for callers that own their own frame loop. */
    public static void beginFrame(float forcedDeltaMillis) {
        deltaMillis = AnimationMath.clamp(forcedDeltaMillis, 0.0F, MAX_FRAME_MILLIS);
        frameIndex++;
    }

    /** Drops the timing state; the next frame starts from {@link #FIRST_FRAME_MILLIS}. */
    public static void reset() {
        frameIndex = 0L;
        lastFrameNanos = 0L;
        deltaMillis = FIRST_FRAME_MILLIS;
    }
}
