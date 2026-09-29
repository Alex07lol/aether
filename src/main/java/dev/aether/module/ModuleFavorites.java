package dev.aether.module;

import dev.aether.config.ConfigDocument;

import java.util.HashSet;
import java.util.Set;

/**
 * The modules the user starred in the GUI, independent of {@code favoriteByDefault}
 * (which is the module author's static hint). Stored under {@code module.favorite.<id>}
 * in the same config document, so favourites survive restarts like every other piece of
 * module state.
 * <p>
 * The GUI reads {@link #isFavorite(String)} combined with the metadata hint to decide
 * what the Favorites filter shows, and toggles through {@link #setFavorite(String, boolean)}.
 */
public final class ModuleFavorites {

    private static final String KEY_PREFIX = "module.favorite.";

    private final Set<String> favorites = new HashSet<String>();

    public boolean isFavorite(String moduleId) {
        return moduleId != null && favorites.contains(moduleId);
    }

    public void setFavorite(String moduleId, boolean favorite) {
        if (moduleId == null || moduleId.isEmpty()) {
            return;
        }
        if (favorite) {
            favorites.add(moduleId);
        } else {
            favorites.remove(moduleId);
        }
    }

    public boolean toggle(String moduleId) {
        boolean now = !isFavorite(moduleId);
        setFavorite(moduleId, now);
        return now;
    }

    public void applyConfig(ConfigDocument document) {
        favorites.clear();
        for (String key : document.values().keySet()) {
            if (key.startsWith(KEY_PREFIX) && document.getBoolean(key, false)) {
                favorites.add(key.substring(KEY_PREFIX.length()));
            }
        }
    }

    public void writeConfig(ConfigDocument.Builder builder) {
        for (String id : favorites) {
            builder.putBoolean(KEY_PREFIX + id, true);
        }
    }
}
