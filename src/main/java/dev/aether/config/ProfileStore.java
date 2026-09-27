package dev.aether.config;

import dev.aether.module.ModuleRegistry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

/**
 * Named snapshots of the module configuration: which modules are on and what every setting holds.
 * <p>
 * A profile is stored in the same config document as the live configuration, under
 * {@code profile.<name>.<original key>}, so switching profiles is a matter of capturing the registry
 * through {@link ModuleRegistry#toConfig()} and feeding a saved snapshot back through
 * {@link ModuleRegistry#applyConfig(ConfigDocument)} - the same normalisation path the config file
 * itself uses, which is what keeps a profile from ever restoring an out-of-range value.
 * <p>
 * The theme is not stored separately on purpose: themes are modules, so their enabled state is part
 * of the snapshot and a profile switches the palette with everything else.
 */
public final class ProfileStore {
    private static final String PREFIX = "profile.";
    private static final int MAX_NAME_LENGTH = 32;

    /** Snapshots keyed by the lower-cased profile name, so lookups ignore capitalisation. */
    private final Map<String, Map<String, String>> profiles = new TreeMap<String, Map<String, String>>();

    /** The name as the player typed it, keyed the same way, so the list keeps their spelling. */
    private final Map<String, String> displayNames = new TreeMap<String, String>();

    /** Reads every {@code profile.*} value out of the config document, replacing what was held. */
    public void applyConfig(ConfigDocument document) {
        this.profiles.clear();
        this.displayNames.clear();
        for (Map.Entry<String, String> entry : document.values().entrySet()) {
            String key = entry.getKey();
            if (!key.startsWith(PREFIX)) {
                continue;
            }
            int nameEnd = key.indexOf('.', PREFIX.length());
            if (nameEnd < 0) {
                continue;
            }
            String name = key.substring(PREFIX.length(), nameEnd);
            String nested = key.substring(nameEnd + 1);
            if (name.isEmpty() || nested.isEmpty()) {
                continue;
            }
            String profileKey = name.toLowerCase(Locale.ENGLISH);
            Map<String, String> snapshot = this.profiles.get(profileKey);
            if (snapshot == null) {
                snapshot = new LinkedHashMap<String, String>();
                this.profiles.put(profileKey, snapshot);
                this.displayNames.put(profileKey, name);
            }
            snapshot.put(nested, entry.getValue());
        }
    }

    /** Writes every profile back into the config document being saved. */
    public void writeConfig(ConfigDocument.Builder builder) {
        for (Map.Entry<String, Map<String, String>> profile : this.profiles.entrySet()) {
            String name = this.displayNames.containsKey(profile.getKey())
                ? this.displayNames.get(profile.getKey())
                : profile.getKey();
            for (Map.Entry<String, String> entry : profile.getValue().entrySet()) {
                builder.put(PREFIX + name + "." + entry.getKey(), entry.getValue());
            }
        }
    }

    /** @return the profile names as they were typed, sorted so the list never jumps around. */
    public List<String> names() {
        List<String> names = new ArrayList<String>();
        for (String key : this.profiles.keySet()) {
            names.add(this.displayNames.containsKey(key) ? this.displayNames.get(key) : key);
        }
        Collections.sort(names, new Comparator<String>() {
            public int compare(String a, String b) {
                return a.compareToIgnoreCase(b);
            }
        });
        return Collections.unmodifiableList(names);
    }

    public int size() {
        return this.profiles.size();
    }

    public boolean exists(String name) {
        return this.profiles.containsKey(key(name));
    }

    /** Captures the current state of every registered module and setting under {@code name}. */
    public boolean save(String name, ModuleRegistry modules) {
        String display = sanitize(name);
        if (display.isEmpty() || modules == null) {
            return false;
        }
        Map<String, String> snapshot = new LinkedHashMap<String, String>(modules.toConfig().values());
        if (snapshot.isEmpty()) {
            return false;
        }
        String key = key(display);
        this.profiles.put(key, snapshot);
        this.displayNames.put(key, display);
        return true;
    }

    /**
     * Applies a saved profile through the registry's own config path. Modules that the profile does
     * not mention keep their current state, so a profile saved by an older build cannot silently
     * switch off a module that did not exist yet.
     */
    public boolean apply(String name, ModuleRegistry modules) {
        Map<String, String> snapshot = this.profiles.get(key(name));
        if (snapshot == null || modules == null) {
            return false;
        }
        ConfigDocument.Builder builder = ConfigDocument.builder();
        builder.putAll(snapshot);
        modules.applyConfig(builder.build());
        return true;
    }

    public boolean delete(String name) {
        String key = key(name);
        this.displayNames.remove(key);
        return this.profiles.remove(key) != null;
    }

    /** Reads a saved profile without applying it, mainly so screens can show a summary. */
    public Map<String, String> snapshot(String name) {
        Map<String, String> snapshot = this.profiles.get(key(name));
        return snapshot == null ? Collections.<String, String>emptyMap() : Collections.unmodifiableMap(snapshot);
    }

    private static String key(String name) {
        return sanitize(name).toLowerCase(Locale.ENGLISH);
    }

    /** How many modules a saved profile switches on. */
    public int enabledCount(String name) {
        int count = 0;
        for (Map.Entry<String, String> entry : snapshot(name).entrySet()) {
            if (entry.getKey().endsWith(".enabled") && Boolean.parseBoolean(entry.getValue())) {
                count++;
            }
        }
        return count;
    }

    /**
     * Turns a typed name into a config-safe key: no dots (the key layout uses them as separators), no
     * whitespace runs, no control characters, and a length limit so a long paste cannot create an
     * unreadable key.
     */
    public static String sanitize(String rawName) {
        if (rawName == null) {
            return "";
        }
        StringBuilder cleaned = new StringBuilder();
        boolean lastWasSpace = false;
        for (int i = 0; i < rawName.length() && cleaned.length() < MAX_NAME_LENGTH; i++) {
            char c = rawName.charAt(i);
            if (c < ' ') {
                continue;
            }
            if (c == '.') {
                // A dot cannot survive - the key layout uses it as a separator - but it should not
                // glue two words together either, so treat it as a space.
                if (cleaned.length() > 0 && !lastWasSpace) {
                    lastWasSpace = true;
                    cleaned.append(' ');
                }
                continue;
            }
            if (Character.isWhitespace(c)) {
                if (cleaned.length() == 0 || lastWasSpace) {
                    continue;
                }
                lastWasSpace = true;
                cleaned.append(' ');
                continue;
            }
            lastWasSpace = false;
            cleaned.append(c);
        }
        return cleaned.toString().trim();
    }
}
