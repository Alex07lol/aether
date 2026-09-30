package dev.aether.hud;

import dev.aether.animation.AnimationMath;

/**
 * The Mouse Display's indicator offset maths, kept out of the renderer so it can be tested without
 * a running game.
 * <p>
 * The widget works in polar coordinates: the raw per-frame mouse delta sets a <em>target</em>
 * direction and a <em>target</em> distance (clamped to the pad's radius), and an exponential
 * approach moves the rendered offset towards that target at a rate derived from the module's speed
 * dial. When the mouse stops, the target distance collapses to zero, so the same approach is also
 * the spring back to centre. {@code AnimationMath.approach} is frame-rate independent, which is
 * what makes the travel identical at 30 and 240 FPS.
 * <p>
 * The offset is computed fresh every frame from the (persistent) current offset, so the maths never
 * allocates and the renderer holds the only mutable state.
 */
public final class MouseIndicator {

    private MouseIndicator() {
    }

    /**
     * Advances the indicator offset one frame towards the raw mouse delta.
     *
     * @param offsetX       current rendered offset from the pad's centre, x
     * @param offsetY       current rendered offset from the pad's centre, y
     * @param deltaPixels   raw mouse movement this frame ({@code [x, y]}, device units)
     * @param radius        the pad's usable radius; the target offset is clamped to it
     * @param speedScale    how far the indicator may drift per 1000 raw units of movement, in
     *                      offset units; comes from the module's speed dial
     * @param deltaMillis   the frame delta, for the frame-rate-independent approach
     * @param rateMillis    approach rate: lower is snappier; {@code 1000/speedFactor} of the dial
     * @return the new offset as {@code [x, y]}
     */
    public static float[] next(float offsetX, float offsetY, int[] deltaPixels,
                               float radius, float speedScale, float deltaMillis, float rateMillis) {
        float length = (float) Math.sqrt(deltaPixels[0] * (double) deltaPixels[0]
            + deltaPixels[1] * (double) deltaPixels[1]);
        float targetX = 0.0F;
        float targetY = 0.0F;
        if (length > 0.001F && speedScale > 0.0F) {
            // Direction preserved, magnitude proportional to the movement and clamped to the pad:
            // a small nudge leans the indicator, a hard flick pins it to the rim.
            float scale = Math.min(length * speedScale, radius) / length;
            targetX = deltaPixels[0] * scale;
            targetY = deltaPixels[1] * scale;
        }
        float x = AnimationMath.approach(offsetX, targetX, deltaMillis, rateMillis);
        float y = AnimationMath.approach(offsetY, targetY, deltaMillis, rateMillis);
        // The per-axis approach can overshoot the radius on a diagonal, so the result is clamped
        // as a vector.
        float out = (float) Math.sqrt(x * x + y * y);
        if (out > radius) {
            float clamp = radius / out;
            x *= clamp;
            y *= clamp;
        }
        return new float[] {x, y};
    }

    /**
     * The compass word of a direction, matching the HUD Direction element's eight-way naming.
     *
     * @param dx horizontal offset, negative = west
     * @param dy vertical offset, negative = north (screen up)
     */
    public static String direction(float dx, float dy) {
        if (Math.abs(dx) < 0.001F && Math.abs(dy) < 0.001F) {
            return "";
        }
        double angle = Math.toDegrees(Math.atan2(dx, -dy));
        if (angle < 0.0D) {
            angle += 360.0D;
        }
        String[] names = {"North", "North-East", "East", "South-East", "South", "South-West", "West", "North-West"};
        return names[((int) Math.floor((angle + 22.5D) / 45.0D)) & 7];
    }
}
