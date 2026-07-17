package dev.aether.hud;

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
}

