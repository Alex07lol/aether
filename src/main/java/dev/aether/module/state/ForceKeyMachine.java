package dev.aether.module.state;

/**
 * The "hold a key for me" behaviour shared by toggle sprint and toggle sneak.
 * <p>
 * Three semantics are useful, and GlideClient's toggle sprint is the reference for naming them:
 * <ul>
 *   <li>{@link Mode#VANILLA} - never touch the key. The module exists but does nothing, which is the
 *       honest answer for a player who only wants the HUD status line.</li>
 *   <li>{@link Mode#HELD} - force the key only while the module's own key is down (a second sprint
 *       key that behaves like the real one).</li>
 *   <li>{@link Mode#TOGGLED} - latch on a press edge, which is the classic toggle.</li>
 * </ul>
 * The machine owns the published state as well as the latch, so the vanilla key is written exactly
 * once per transition and handed back exactly once when the module is disabled, instead of being
 * rewritten every tick (which would break the player's own key handling).
 */
public final class ForceKeyMachine {

    public enum Mode {
        VANILLA,
        HELD,
        TOGGLED;

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

    private final ToggleKey latch = new ToggleKey();
    private Mode mode = Mode.TOGGLED;
    private boolean forcing;
    private boolean lastToggled;

    public Mode mode() {
        return this.mode;
    }

    /** Switching behaviour drops the latch: a Held machine has no toggled state to remember. */
    public void setMode(Mode next) {
        if (next == null || next == this.mode) {
            return;
        }
        this.mode = next;
        this.latch.reset();
    }

    /**
     * @param keyDown the module's physical key state this tick
     * @return the key state to publish, or {@code null} when the module must leave the vanilla key
     *         alone this tick (the common case once the state has settled)
     */
    public Boolean update(boolean keyDown) {
        this.lastToggled = false;
        switch (this.mode) {
            case VANILLA:
                this.latch.reset();
                return releaseIfForcing();
            case HELD:
                if (keyDown) {
                    if (!this.forcing) {
                        this.forcing = true;
                        return Boolean.TRUE;
                    }
                    return null;
                }
                return releaseIfForcing();
            case TOGGLED:
            default:
                this.lastToggled = this.latch.update(keyDown);
                if (this.latch.active() == this.forcing) {
                    return null;
                }
                this.forcing = this.latch.active();
                return Boolean.valueOf(this.forcing);
        }
    }

    private Boolean releaseIfForcing() {
        if (!this.forcing) {
            return null;
        }
        this.forcing = false;
        return Boolean.FALSE;
    }

    /** @return true while the latched toggle is on (always false outside {@link Mode#TOGGLED}). */
    public boolean toggled() {
        return this.mode == Mode.TOGGLED && this.latch.active();
    }

    /** @return true while the module owns the vanilla key. */
    public boolean isForcing() {
        return this.forcing;
    }

    /** @return true when the last {@link #update(boolean)} flipped the latched toggle. */
    public boolean justToggled() {
        return this.lastToggled;
    }

    /**
     * Forgets everything.
     *
     * @return true when the caller must publish a released key state exactly once
     */
    public boolean reset() {
        boolean wasForcing = this.forcing;
        this.forcing = false;
        this.lastToggled = false;
        this.latch.reset();
        return wasForcing;
    }

    @Override
    public String toString() {
        return "ForceKeyMachine{" + this.mode + ", forcing=" + this.forcing + ", toggled=" + toggled() + "}";
    }
}
