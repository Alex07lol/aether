package dev.aether.cosmetic;

import dev.aether.config.ConfigDocument;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Runtime state for cosmetics – which asset fills each slot and which assets are favourited.
 * This is separate from {@link CosmeticLibrary} which only stores the catalogue.
 */
public final class CosmeticState {

    private final Map<CosmeticType, String> equipped = new HashMap<>();
    private final Set<String> favorites = new HashSet<>();

    /** Equip the given asset (by id) into its own slot. */
    public void equip(String id, CosmeticAsset asset) {
        equipped.put(asset.type(), id);
    }

    /** Unequip the asset of the given type. */
    public void unequip(CosmeticType type) {
        equipped.remove(type);
    }

    /** Toggle favourite flag for a cosmetic id. */
    public void toggleFavorite(String id) {
        if (!favorites.add(id)) {
            favorites.remove(id);
        }
    }

    public boolean isEquipped(String id) {
        return equipped.containsValue(id);
    }

    public boolean isFavorite(String id) {
        return favorites.contains(id);
    }

    public String equippedFor(CosmeticType type) {
        return equipped.get(type);
    }

    public Map<CosmeticType, String> equipped() {
        return Collections.unmodifiableMap(equipped);
    }

    public Set<String> favorites() {
        return Collections.unmodifiableSet(favorites);
    }

    /** Persist to the client config. */
    public void writeConfig(ConfigDocument.Builder out) {
        for (Map.Entry<CosmeticType, String> e : equipped.entrySet()) {
            out.put("cosmetics." + e.getKey().name().toLowerCase(), e.getValue());
        }
        for (String fav : favorites) {
            out.putBoolean("cosmetics.favorite." + fav, true);
        }
    }

    /** Load from config. */
    public void applyConfig(ConfigDocument in) {
        for (CosmeticType type : CosmeticType.values()) {
            String key = "cosmetics." + type.name().toLowerCase();
            String id = in.get(key, null);
            if (id != null) equipped.put(type, id);
        }
        for (String key : in.values().keySet()) {
            if (key.startsWith("cosmetics.favorite.") && in.getBoolean(key, false)) {
                String favId = key.substring("cosmetics.favorite.".length());
                if (!favId.isEmpty()) favorites.add(favId);
            }
        }
    }
}