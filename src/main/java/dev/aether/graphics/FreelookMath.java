package dev.aether.graphics;

/**
 * Camera maths for the freelook module.
 * <p>
 * Freelook keeps the camera orientation in its own fields and never writes the player's
 * rotation, so the numbers here have to line up with the way vanilla turns the head: the
 * sensitivity curve is vanilla's own {@code (s * 0.6 + 0.2)^3 * 8} factor and the final
 * {@code 0.15} degrees-per-unit constant comes from {@code EntityPlayerSP.setAngles}. A module
 * sensitivity of 100 therefore moves the camera exactly as fast as vanilla moves the player.
 * <p>
 * The yaw base is the player's yaw plus 180 degrees: that is the angle vanilla's third-person
 * camera is built from, which is why freelook hands the value straight to the camera setup.
 */
public final class FreelookMath {
    /** Vanilla's degrees per mouse unit ({@code EntityPlayerSP.setAngles} scales by this). */
    public static final float DEGREES_PER_MOUSE_UNIT = 0.15F;

    /** Vanilla's mouse slider default; used when game settings cannot be read. */
    public static final float DEFAULT_MOUSE_SENSITIVITY = 0.5F;

    /** Vanilla clamps the head pitch to straight up/down. */
    public static final float PITCH_LIMIT = 90.0F;

    private FreelookMath() {
    }

    /**
     * Vanilla's mouse delta scale: {@code (sensitivity * 0.6 + 0.2)^3 * 8 * 0.15} degrees per unit.
     * At the default slider of 0.5 this is exactly {@code 0.15}.
     */
    public static float mouseScale(float mouseSensitivity) {
        float slider = clamp(mouseSensitivity, 0.0F, 1.0F) * 0.6F + 0.2F;
        return slider * slider * slider * 8.0F * DEGREES_PER_MOUSE_UNIT;
    }

    /** The module's sensitivity dial as a multiplier: 100 means "vanilla look speed". */
    public static float moduleScale(int sensitivityPercent) {
        return clamp((float) sensitivityPercent, 1.0F, 1000.0F) / 100.0F;
    }

    /** The camera yaw vanilla's third-person camera is oriented to. */
    public static float thirdPersonCameraYaw(float playerYaw) {
        return wrapDegrees(playerYaw + 180.0F);
    }

    /** Applies a horizontal mouse delta, wrapping the result so long holds cannot drift. */
    public static float yawAfter(float cameraYaw, int deltaX, float mouseSensitivity, float moduleScale) {
        return wrapDegrees(cameraYaw + deltaX * mouseScale(mouseSensitivity) * moduleScale);
    }

    /**
     * Applies a vertical mouse delta, clamped to the vanilla pitch limit.
     * <p>
     * Vanilla subtracts the delta, so pushing the mouse up (positive deltaY, which is the sign
     * LWJGL reports once the cursor is grabbed) looks up. {@code invertY} flips that.
     */
    public static float pitchAfter(float cameraPitch, int deltaY, float mouseSensitivity, float moduleScale,
                                   boolean invertY) {
        float delta = deltaY * mouseScale(mouseSensitivity) * moduleScale;
        float next = invertY ? cameraPitch + delta : cameraPitch - delta;
        return clamp(next, -PITCH_LIMIT, PITCH_LIMIT);
    }

    /** Vanilla-style angle wrap into {@code [-180, 180)}. */
    public static float wrapDegrees(float degrees) {
        float wrapped = degrees % 360.0F;
        if (wrapped >= 180.0F) {
            wrapped -= 360.0F;
        }
        if (wrapped < -180.0F) {
            wrapped += 360.0F;
        }
        return wrapped;
    }

    public static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}
