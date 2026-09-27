package dev.aether.module.state;

/**
 * The latched state behind a "press the key to toggle" module such as toggle sprint or toggle
 * sneak.
 * <p>
 * Two pieces of state matter and both are owned here: whether the feature is toggled on, and
 * whether the physical key was down during the previous tick. The second one is what makes the
 * press an <em>edge</em> - without it a held key would flip the state twenty times a second - and
 * it is also the piece that has to survive a module going away, which is why {@link #reset()} is
 * the only way to disable one of these.
 */
public final class ToggleKey {
    private boolean active;
    private boolean keyDown;

    /**
     * Feeds the current key state in.
     *
     * @return true when this call is the press edge that flipped {@link #active()}
     */
    public boolean update(boolean down) {
        boolean toggled = false;
        if (down && !this.keyDown) {
            this.active = !this.active;
            toggled = true;
        }
        this.keyDown = down;
        return toggled;
    }

    public boolean active() {
        return this.active;
    }

    public boolean keyDown() {
        return this.keyDown;
    }

    /** Drops the latch only: the next key press still has to be an edge. */
    public void clearActive() {
        this.active = false;
    }

    /**
     * Forgets everything, including the physical key latch. Called when the owning module is
     * disabled or when no world is loaded, so a re-enable always starts from a clean slate
     * instead of inheriting a half-pressed key.
     */
    public void reset() {
        this.active = false;
        this.keyDown = false;
    }

    @Override
    public String toString() {
        return "ToggleKey{active=" + this.active + ", keyDown=" + this.keyDown + "}";
    }
}
