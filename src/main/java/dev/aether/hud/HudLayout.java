package dev.aether.hud;

import dev.aether.config.ConfigDocument;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class HudLayout {
    private final int gridSize;
    private final Map<String, HudElement> elements = new LinkedHashMap<String, HudElement>();

    public HudLayout(int gridSize) {
        if (gridSize < 1) {
            throw new IllegalArgumentException("Grid size must be positive.");
        }
        this.gridSize = gridSize;
    }

    public HudElement add(String id, int x, int y) {
        if (elements.containsKey(id)) {
            throw new IllegalArgumentException("HUD element already exists: " + id);
        }
        HudElement element = new HudElement(id, x, y);
        elements.put(id, element);
        return element;
    }

    public HudElement get(String id) {
        HudElement element = elements.get(id);
        if (element == null) {
            throw new IllegalArgumentException("Unknown HUD element: " + id);
        }
        return element;
    }

    public void move(String id, int x, int y, boolean snapToGrid) {
        get(id).moveTo(snapToGrid ? snap(x) : x, snapToGrid ? snap(y) : y);
    }

    public void scale(String id, float scale) {
        get(id).setScale(scale);
    }

    public void opacity(String id, float opacity) {
        get(id).setOpacity(opacity);
    }

    public void layer(String id, int layer) {
        get(id).setLayer(layer);
    }

    public java.util.Collection<HudElement> elements() {
        return Collections.unmodifiableCollection(elements.values());
    }

    public List<HudElement> renderOrder() {
        List<HudElement> ordered = new ArrayList<HudElement>(elements.values());
        Collections.sort(ordered, new Comparator<HudElement>() {
            public int compare(HudElement left, HudElement right) {
                return left.layer() - right.layer();
            }
        });
        return ordered;
    }

    private int snap(int value) {
        return Math.round((float) value / (float) gridSize) * gridSize;
    }

    /**
     * Persists every element's position, scale and opacity under {@code hud.<id>.*} in
     * the shared config document, so an edited HUD layout survives a restart. Elements
     * are moved through {@link #move}/{@link #scale}/{@link #opacity}; saving the
     * client writes them with everything else.
     */
    public dev.aether.config.ConfigDocument toConfig() {
        dev.aether.config.ConfigDocument.Builder builder = dev.aether.config.ConfigDocument.builder();
        for (HudElement element : elements.values()) {
            String prefix = "hud." + element.id();
            builder.put(prefix + ".x", String.valueOf(element.x()));
            builder.put(prefix + ".y", String.valueOf(element.y()));
            builder.put(prefix + ".scale", String.valueOf(element.scale()));
            builder.put(prefix + ".opacity", String.valueOf(element.opacity()));
        }
        return builder.build();
    }

    /** Restores persisted element geometry; unknown ids and malformed numbers are ignored. */
    public void applyConfig(ConfigDocument document) {
        for (HudElement element : elements.values()) {
            String prefix = "hud." + element.id();
            String x = document.get(prefix + ".x", null);
            String y = document.get(prefix + ".y", null);
            if (x != null && y != null) {
                try {
                    element.moveTo(Integer.parseInt(x.trim()), Integer.parseInt(y.trim()));
                } catch (NumberFormatException ignored) {
                }
            }
            String scale = document.get(prefix + ".scale", null);
            if (scale != null) {
                try {
                    element.setScale(Float.parseFloat(scale.trim()));
                } catch (NumberFormatException ignored) {
                }
            }
            String opacity = document.get(prefix + ".opacity", null);
            if (opacity != null) {
                try {
                    element.setOpacity(Float.parseFloat(opacity.trim()));
                } catch (NumberFormatException ignored) {
                }
            }
        }
    }
}

