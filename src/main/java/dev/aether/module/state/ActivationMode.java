package dev.aether.module.state;

/**
 * How a module that is driven by a key reacts to that key.
 * <p>
 * Most camera modules read this from their own setting so a player can choose between
 * "held down" and "press to toggle", which is the one behaviour difference GlideClient and
 * CloudClient both ship and Aether previously hard-coded per module.
 */
public enum ActivationMode {
    /** Active only while the key is physically down. */
    HOLD,

    /** One press turns it on, the next turns it off. */
    TOGGLE;

    /** @return the mode named by {@code value}, or {@code fallback} when it is unknown or null. */
    public static ActivationMode from(String value, ActivationMode fallback) {
        if (value == null) {
            return fallback;
        }
        String trimmed = value.trim();
        for (ActivationMode mode : values()) {
            if (mode.name().equalsIgnoreCase(trimmed)) {
                return mode;
            }
        }
        return fallback;
    }

    public boolean isToggle() {
        return this == TOGGLE;
    }
}
