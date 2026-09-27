package dev.aether.graphics;

/**
 * The camera freelook owns while it is active.
 * <p>
 * This is deliberately only the orientation: the player's rotation is never stored here, never
 * written and never read back, so there is no path by which moving the camera can move the body or
 * the head. The view starts from the player's own orientation (vanilla's third-person camera base)
 * and is then fed mouse deltas through {@link FreelookMath}, which scales them exactly like
 * vanilla's own look code does.
 * <p>
 * Everything is dropped on {@link #stop()}, so a fresh activation after a disable always starts
 * from the player again instead of inheriting a stale camera angle.
 */
public final class FreelookView {
    private boolean active;
    private float yaw;
    private float pitch;

    public boolean isActive() {
        return this.active;
    }

    public float yaw() {
        return this.yaw;
    }

    public float pitch() {
        return this.pitch;
    }

    /**
     * Seeds the camera from the player's current orientation.
     *
     * @param playerYaw   the player's yaw, converted to the third-person camera base
     * @param playerPitch the player's pitch, clamped to vanilla's limit
     */
    public void start(float playerYaw, float playerPitch) {
        this.yaw = FreelookMath.thirdPersonCameraYaw(playerYaw);
        this.pitch = FreelookMath.clamp(playerPitch, -FreelookMath.PITCH_LIMIT, FreelookMath.PITCH_LIMIT);
        this.active = true;
    }

    /** Drops the camera angle; the next {@link #start(float, float)} reseeds from the player. */
    public void stop() {
        this.active = false;
        this.yaw = 0.0F;
        this.pitch = 0.0F;
    }

    /**
     * Applies one mouse delta.
     *
     * @param sensitivityScale the module's sensitivity dial (100 = vanilla speed)
     * @param invertX          flips the horizontal axis
     * @param invertY          flips the vertical axis
     */
    public void look(int deltaX, int deltaY, float mouseSensitivity, float sensitivityScale,
                     boolean invertX, boolean invertY) {
        int yawDelta = invertX ? -deltaX : deltaX;
        this.yaw = FreelookMath.yawAfter(this.yaw, yawDelta, mouseSensitivity, sensitivityScale);
        this.pitch = FreelookMath.pitchAfter(this.pitch, deltaY, mouseSensitivity, sensitivityScale, invertY);
    }

    @Override
    public String toString() {
        return "FreelookView{active=" + this.active + ", yaw=" + this.yaw + ", pitch=" + this.pitch + "}";
    }
}
