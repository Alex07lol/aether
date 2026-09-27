package dev.aether.module.state;

/**
 * Captures a vanilla value the first time a module needs to override it, and hands it back
 * exactly once when the module is done.
 * <p>
 * Every "module turns a setting off and puts it back" feature has the same three failure modes:
 * capturing the value again while the module is already active (which captures the module's own
 * override and makes the restore destroy the user's setting), restoring when nothing was
 * captured (which writes a value the module never owned), and leaking the captured value across
 * an enable/disable/enable cycle so the second enable behaves differently from the first.
 * <p>
 * This class is the single mechanism that closes all three, which is why the graphics and
 * performance modules share it instead of each keeping its own nullable field.
 *
 * @param <T> the captured value's type; the class never interprets it
 */
public final class ValueHold<T> {
    private final String name;
    private T held;
    private boolean holding;

    public ValueHold(String name) {
        this.name = name;
    }

    /** Human readable tag used in {@link #toString()} and in debugging sessions. */
    public String name() {
        return this.name;
    }

    /**
     * Captures {@code value} when nothing is held yet.
     *
     * @return true when this call performed the capture; false when a value was already held and
     *         the caller must keep using {@link #held()}
     */
    public boolean capture(T value) {
        if (this.holding) {
            return false;
        }
        this.held = value;
        this.holding = true;
        return true;
    }

    public boolean isHolding() {
        return this.holding;
    }

    /** The captured value, or {@code null} when nothing is held. */
    public T held() {
        return this.held;
    }

    /**
     * Returns the captured value once and clears the hold.
     *
     * @return the value to restore, or {@code null} when the module never captured anything
     *         (so the caller must not write anything back)
     */
    public T release() {
        if (!this.holding) {
            return null;
        }
        T value = this.held;
        this.held = null;
        this.holding = false;
        return value;
    }

    /** Drops the hold without restoring; used when the state was already restored elsewhere. */
    public void forget() {
        this.held = null;
        this.holding = false;
    }

    @Override
    public String toString() {
        return "ValueHold{" + this.name + " holding=" + this.holding + ", held=" + this.held + "}";
    }
}
