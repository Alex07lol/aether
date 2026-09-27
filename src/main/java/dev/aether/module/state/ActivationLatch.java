package dev.aether.module.state;

/**
 * Turns a key stream into "should this module be active right now" for both activation modes.
 * <p>
 * Hold mode is a pass-through of the key. Toggle mode latches on a press edge, which is why the
 * press latch lives here rather than in the caller: a module that toggles has to remember both the
 * latched state and whether the key was already down, and both have to be forgotten when the module
 * is switched off or a GUI takes the keyboard. {@link #reset()} is the only way to deactivate, so a
 * disable cannot leave a half-pressed key behind.
 */
public final class ActivationLatch {
    private final ToggleKey latch = new ToggleKey();
    private ActivationMode mode = ActivationMode.HOLD;
    private boolean active;
    private boolean changed;

    public ActivationLatch() {
    }

    public ActivationLatch(ActivationMode mode) {
        if (mode != null) {
            this.mode = mode;
        }
    }

    public ActivationMode mode() {
        return this.mode;
    }

    /** Switching mode ends the current activation; a new mode starts from a clean press. */
    public void setMode(ActivationMode mode) {
        if (mode == null || mode == this.mode) {
            return;
        }
        this.mode = mode;
        reset();
    }

    /**
     * Feeds the current key state in.
     *
     * @param keyDown whether the module's key is down this tick
     * @return {@code true} when the module should be active this tick
     */
    public boolean update(boolean keyDown) {
        boolean previous = this.active;
        if (this.mode.isToggle()) {
            this.latch.update(keyDown);
            this.active = this.latch.active();
        } else {
            this.latch.reset();
            this.active = keyDown;
        }
        this.changed = previous != this.active;
        return this.active;
    }

    public boolean isActive() {
        return this.active;
    }

    /** @return true when the last {@link #update(boolean)} changed the activation. */
    public boolean changed() {
        return this.changed;
    }

    /**
     * Deactivates and forgets the press latch.
     *
     * @return true when the caller has to undo something the module was holding, i.e. when the
     *         module was active before the reset
     */
    public boolean reset() {
        boolean wasActive = this.active;
        this.active = false;
        this.changed = wasActive;
        this.latch.reset();
        return wasActive;
    }
}
