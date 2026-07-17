package dev.aether.module.setting;

public final class Setting<T> {
    private final String id;
    private final String label;
    private final SettingType type;
    private final T defaultValue;
    private T value;

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
        this.value = value;
    }

    public void reset() {
        this.value = defaultValue;
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
        this.value = (T) restored;
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
}
