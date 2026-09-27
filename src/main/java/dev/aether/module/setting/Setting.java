package dev.aether.module.setting;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * A single persisted module setting.
 * <p>
 * Numeric and choice settings can carry their own metadata: a {@link Range} for sliders
 * and a choice list for pills. Screens read that metadata instead of keeping id-keyed
 * tables of their own, so a new setting cannot ship with a guessed slider bound or a
 * missing option list.
 */
public final class Setting<T> {
    private final String id;
    private final String label;
    private final SettingType type;
    private final T defaultValue;
    private T value;
    private Range range;
    private List<String> choices;

    public Setting(String id, String label, SettingType type, T defaultValue) {
        if (id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException("Setting id cannot be blank.");
        }
        this.id = id;
        this.label = label;
        this.type = type;
        this.defaultValue = defaultValue;
        this.value = defaultValue;
    }

    public String id() {
        return id;
    }

    public String label() {
        return label;
    }

    public SettingType type() {
        return type;
    }

    public T defaultValue() {
        return defaultValue;
    }

    public T value() {
        return value;
    }

    public void setValue(T value) {
        this.value = normalize(value);
    }

    public void reset() {
        this.value = defaultValue;
    }

    /** @return the slider bounds, or {@code null} when the setting is not bounded. */
    public Range range() {
        return range;
    }

    /** @return the selectable options, or {@code null} when the setting is free-form. */
    public List<String> choices() {
        return choices;
    }

    public boolean hasRange() {
        return range != null;
    }

    public boolean hasChoices() {
        return choices != null && !choices.isEmpty();
    }

    /** Attaches slider bounds. Ignores a {@code null} step and non-positive spans. */
    public Setting<T> range(int min, int max, int step) {
        this.range = new Range(min, max, step);
        this.value = normalize(this.value);
        return this;
    }

    /** Attaches the selectable options. The first entry becomes the fallback for unknown values. */
    public Setting<T> choices(String... options) {
        if (options == null || options.length == 0) {
            throw new IllegalArgumentException("A choice setting needs at least one option.");
        }
        this.choices = Collections.unmodifiableList(Arrays.asList(options.clone()));
        this.value = normalize(this.value);
        return this;
    }

    /** Clamps numbers into their range and drops choice values that are not on the list. */
    @SuppressWarnings("unchecked")
    private T normalize(T candidate) {
        if (candidate instanceof Number && range != null) {
            Number number = (Number) candidate;
            if (candidate instanceof Integer) {
                return (T) Integer.valueOf(range.clamp(number.intValue()));
            }
            if (candidate instanceof Long) {
                return (T) Long.valueOf(range.clamp(number.intValue()));
            }
        }
        if (choices != null && candidate instanceof String) {
            String text = (String) candidate;
            return choices.contains(text) ? candidate : (T) choices.get(0);
        }
        return candidate;
    }

    public String serializeValue() {
        return String.valueOf(value);
    }

    @SuppressWarnings("unchecked")
    public void restoreValue(String rawValue) {
        Object restored;
        if (type == SettingType.BOOLEAN) {
            restored = Boolean.valueOf(Boolean.parseBoolean(rawValue));
        } else if (type == SettingType.NUMBER || type == SettingType.COLOR || type == SettingType.KEYBIND) {
            restored = restoreNumber(rawValue);
        } else {
            restored = rawValue;
        }
        this.value = (T) normalize((T) restored);
    }

    private Object restoreNumber(String rawValue) {
        try {
            if (defaultValue instanceof Integer) {
                return Integer.valueOf(Integer.parseInt(rawValue));
            }
            if (defaultValue instanceof Long) {
                return Long.valueOf(Long.parseLong(rawValue));
            }
            if (defaultValue instanceof Float) {
                return Float.valueOf(Float.parseFloat(rawValue));
            }
            if (defaultValue instanceof Double) {
                return Double.valueOf(Double.parseDouble(rawValue));
            }
        } catch (NumberFormatException ignored) {
            return defaultValue;
        }
        return rawValue;
    }

    public enum SettingType {
        BOOLEAN,
        NUMBER,
        TEXT,
        COLOR,
        KEYBIND,
        CHOICE
    }

    /** Inclusive slider bounds with a step; step is stored as a positive integer. */
    public static final class Range {
        private final int min;
        private final int max;
        private final int step;

        public Range(int min, int max, int step) {
            if (max <= min) {
                throw new IllegalArgumentException("Range max must be greater than min.");
            }
            this.min = min;
            this.max = max;
            this.step = step <= 0 ? 1 : step;
        }

        public int min() {
            return min;
        }

        public int max() {
            return max;
        }

        public int step() {
            return step;
        }

        public int clamp(int value) {
            return Math.max(min, Math.min(max, value));
        }

        /** Snaps a value onto the nearest step from {@link #min}. */
        public int snap(int value) {
            int clamped = clamp(value);
            int offset = clamped - min;
            int snapped = min + Math.round(offset / (float) step) * step;
            return clamp(snapped);
        }

        @Override
        public String toString() {
            return min + ".." + max + "/" + step;
        }
    }
}
