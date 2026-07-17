package dev.aether.config;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class ConfigDocument {
    private final Map<String, String> values;

    private ConfigDocument(Map<String, String> values) {
        this.values = Collections.unmodifiableMap(new LinkedHashMap<String, String>(values));
    }

    public static ConfigDocument empty() {
        return new ConfigDocument(Collections.<String, String>emptyMap());
    }

    public static Builder builder() {
        return new Builder();
    }

    public Map<String, String> values() {
        return values;
    }

    public String get(String key, String defaultValue) {
        String value = values.get(key);
        return value == null ? defaultValue : value;
    }

    public boolean getBoolean(String key, boolean defaultValue) {
        String value = values.get(key);
        return value == null ? defaultValue : Boolean.parseBoolean(value);
    }

    public static final class Builder {
        private final Map<String, String> values = new LinkedHashMap<String, String>();

        public Builder put(String key, String value) {
            if (key == null || key.trim().isEmpty()) {
                throw new IllegalArgumentException("Config key cannot be blank.");
            }
            values.put(key, value == null ? "" : value);
            return this;
        }

        public Builder putBoolean(String key, boolean value) {
            return put(key, Boolean.toString(value));
        }

        public Builder putAll(Map<String, String> source) {
            for (Map.Entry<String, String> entry : source.entrySet()) {
                put(entry.getKey(), entry.getValue());
            }
            return this;
        }

        public ConfigDocument build() {
            return new ConfigDocument(values);
        }
    }
}

