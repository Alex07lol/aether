package dev.aether.waypoint;

import dev.aether.config.ConfigDocument;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Persists waypoints in the shared config document under {@code waypoint.<name>.<field>}.
 * <p>
 * Waypoints ride the same file and save path as modules and cosmetics - Aether keeps exactly one
 * configuration file, and a second store would be a second thing that can fail. Each waypoint owns
 * the keys {@code waypoint.<name>.x|y|z|world|color|enabled}; the display name is preserved in a
 * side map so {@code My Base} and {@code my base} do not collide silently.
 * <p>
 * The manager never touches the world: it only reads config documents and hands out copies. Saving
 * happens through {@link #writeConfig}, which the client's single {@code save()} path calls.
 */
public final class WaypointManager {
    private static final String PREFIX = "waypoint.";
    private static final int MAX_NAME_LENGTH = 32;
    private static final int MAX_WAYPOINTS = 128;

    /** Waypoints keyed by lower-cased name, so lookups and dedupe ignore capitalisation. */
    private final Map<String, Waypoint> waypoints = new LinkedHashMap<String, Waypoint>();

    /** The name as the player typed it, keyed the same way, so the list keeps their spelling. */
    private final Map<String, String> displayNames = new LinkedHashMap<String, String>();

    /** Reads every {@code waypoint.*} value out of the config document, replacing what was held. */
    public void applyConfig(ConfigDocument document) {
        this.waypoints.clear();
        this.displayNames.clear();
        this.positioned.clear();
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
            String field = key.substring(nameEnd + 1);
            if (name.isEmpty() || field.isEmpty()) {
                continue;
            }
            String keyName = name.toLowerCase(Locale.ENGLISH);
            if ("x".equals(field) || "y".equals(field) || "z".equals(field)) {
                this.positioned.add(keyName);
            }
            Waypoint existing = this.waypoints.get(keyName);
            Waypoint parsed = parse(existing, name, field, entry.getValue());
            if (parsed != null) {
                this.waypoints.put(keyName, parsed);
                this.displayNames.put(keyName, name);
            }
        }
        dropUnpositioned();
    }

    /** Keys that received at least one coordinate field in the latest {@link #applyConfig}. */
    private final java.util.Set<String> positioned = new java.util.HashSet<String>();

    /**
     * Removes waypoints that never received a position. A document can carry a stray key (a
     * hand-edited file, a half-deleted entry); a marker without coordinates is not a marker.
     * (0, 0, 0) stays legitimate: an entry that parsed an explicit coordinate is kept.
     */
    private void dropUnpositioned() {
        this.waypoints.keySet().retainAll(this.positioned);
        this.displayNames.keySet().retainAll(this.positioned);
    }

    /** Writes every waypoint back into the config document being saved. */
    public void writeConfig(ConfigDocument.Builder builder) {
        for (Map.Entry<String, Waypoint> entry : this.waypoints.entrySet()) {
            String name = this.displayNames.containsKey(entry.getKey())
                ? this.displayNames.get(entry.getKey())
                : entry.getKey();
            Waypoint waypoint = entry.getValue();
            builder.put(PREFIX + name + ".world", waypoint.world());
            builder.put(PREFIX + name + ".x", Integer.toString(waypoint.x()));
            builder.put(PREFIX + name + ".y", Integer.toString(waypoint.y()));
            builder.put(PREFIX + name + ".z", Integer.toString(waypoint.z()));
            builder.put(PREFIX + name + ".color", Integer.toString(waypoint.color()));
            builder.put(PREFIX + name + ".enabled", Boolean.toString(waypoint.enabled()));
        }
    }

    /**
     * Builds the next state of one waypoint from one config value. Unknown fields are ignored,
     * so a future field can be added without breaking older saves.
     */
    private static Waypoint parse(Waypoint soFar, String displayName, String field, String value) {
        if ("name".equals(field)) {
            // Names live in the key, not in a field; a name field is accepted and ignored.
            return soFar;
        }
        if (soFar == null) {
            soFar = new Waypoint(displayName, "minecraft:overworld", 0, 0, 0, 0xFFFF55, true);
        }
        try {
            if ("world".equals(field)) {
                return soFar.withWorld(emptyToDefault(value, "minecraft:overworld"));
            }
            if ("x".equals(field)) {
                return soFar.withPosition(Integer.parseInt(value.trim()), soFar.y(), soFar.z());
            }
            if ("y".equals(field)) {
                return soFar.withPosition(soFar.x(), Integer.parseInt(value.trim()), soFar.z());
            }
            if ("z".equals(field)) {
                return soFar.withPosition(soFar.x(), soFar.y(), Integer.parseInt(value.trim()));
            }
            if ("color".equals(field)) {
                return soFar.withColor((int) Long.parseLong(value.trim()));
            }
            if ("enabled".equals(field)) {
                return soFar.withEnabled(Boolean.parseBoolean(value.trim()));
            }
        } catch (NumberFormatException invalidValue) {
            // A malformed number keeps whatever the document said before, or the default.
        }
        return soFar;
    }

    private static String emptyToDefault(String value, String fallback) {
        return value == null || value.trim().isEmpty() ? fallback : value.trim();
    }

    /** @return the waypoint names as typed, sorted case-insensitively. */
    public List<String> names() {
        List<String> names = new ArrayList<String>(this.displayNames.values());
        Collections.sort(names, new Comparator<String>() {
            public int compare(String a, String b) {
                return a.compareToIgnoreCase(b);
            }
        });
        return Collections.unmodifiableList(names);
    }

    /** @return all waypoints in name order. */
    public List<Waypoint> all() {
        List<Waypoint> result = new ArrayList<Waypoint>();
        for (String key : sortedKeys()) {
            result.add(this.waypoints.get(key));
        }
        return Collections.unmodifiableList(result);
    }

    /** @return the waypoints that are enabled and in {@code world}, in name order. */
    public List<Waypoint> forWorld(String world) {
        String active = world == null ? "" : world;
        List<Waypoint> result = new ArrayList<Waypoint>();
        for (Waypoint waypoint : all()) {
            if (waypoint.enabled() && active.equalsIgnoreCase(waypoint.world())) {
                result.add(waypoint);
            }
        }
        return result;
    }

    public int size() {
        return this.waypoints.size();
    }

    public boolean exists(String name) {
        return name != null && this.waypoints.containsKey(sanitize(name).toLowerCase(Locale.ENGLISH));
    }

    public Waypoint get(String name) {
        if (name == null) {
            return null;
        }
        return this.waypoints.get(sanitize(name).toLowerCase(Locale.ENGLISH));
    }

    /**
     * Adds a waypoint. @return false when the name is empty, already taken, or the store is full.
     */
    public boolean add(String name, String world, int x, int y, int z, int color) {
        String display = sanitize(name);
        if (display.isEmpty() || this.waypoints.size() >= MAX_WAYPOINTS) {
            return false;
        }
        String key = display.toLowerCase(Locale.ENGLISH);
        if (this.waypoints.containsKey(key)) {
            return false;
        }
        this.waypoints.put(key, new Waypoint(display, world, x, y, z, color, true));
        this.displayNames.put(key, display);
        return true;
    }

    /** Replaces a waypoint's position. @return false when no waypoint has that name. */
    public boolean update(String name, int x, int y, int z) {
        Waypoint existing = get(name);
        if (existing == null) {
            return false;
        }
        String key = sanitize(name).toLowerCase(Locale.ENGLISH);
        this.waypoints.put(key, existing.withPosition(x, y, z));
        return true;
    }

    /** Replaces a waypoint wholesale (used by the editor UI). @return false when it is unknown. */
    public boolean replace(Waypoint waypoint, String originalName) {
        if (waypoint == null || originalName == null) {
            return false;
        }
        String key = sanitize(originalName).toLowerCase(Locale.ENGLISH);
        if (!this.waypoints.containsKey(key)) {
            return false;
        }
        this.waypoints.put(key, waypoint);
        return true;
    }

    /** Flips a waypoint between enabled and disabled. @return the new state, or false when unknown. */
    public boolean toggle(String name) {
        Waypoint existing = get(name);
        if (existing == null) {
            return false;
        }
        String key = sanitize(name).toLowerCase(Locale.ENGLISH);
        Waypoint updated = existing.withEnabled(!existing.enabled());
        this.waypoints.put(key, updated);
        return updated.enabled();
    }

    /** @return true when the waypoint existed and was removed. */
    public boolean remove(String name) {
        if (name == null) {
            return false;
        }
        String key = sanitize(name).toLowerCase(Locale.ENGLISH);
        this.displayNames.remove(key);
        return this.waypoints.remove(key) != null;
    }

    /**
     * Turns a typed name into a config-safe key: no dots (the key layout uses them as separators),
     * no whitespace runs, no control characters, and a length limit so a long paste cannot create
     * an unreadable key. Mirrors {@code ProfileStore.sanitize}.
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

    private List<String> sortedKeys() {
        List<String> keys = new ArrayList<String>(this.waypoints.keySet());
        Collections.sort(keys, new Comparator<String>() {
            public int compare(String a, String b) {
                String da = displayNames.containsKey(a) ? displayNames.get(a) : a;
                String db = displayNames.containsKey(b) ? displayNames.get(b) : b;
                return da.compareToIgnoreCase(db);
            }
        });
        return keys;
    }
}
