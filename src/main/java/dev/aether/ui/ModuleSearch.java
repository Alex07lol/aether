package dev.aether.ui;

import dev.aether.module.ClientModule;
import dev.aether.module.ClientModule.ModuleCategory;
import dev.aether.module.ClientModule.ModuleState;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * The Control Center's module list: filtering, fuzzy search and the cached result set.
 * <p>
 * The list used to be rebuilt inside the screen's render path, which meant re-scoring every module
 * against the query on every frame. Here the inputs are explicit - the registry, the query, the
 * category and the two filter switches - and the results are recomputed only when one of them
 * actually changes. Rendering the same list twice in a row is a size check.
 * <p>
 * The scoring itself is shared: the deck, the legacy list view and any future search surface rank
 * modules with the same rules, so a module cannot appear in one place and vanish in another.
 */
public final class ModuleSearch {
    private List<ClientModule> source = Collections.emptyList();
    private String query = "";
    private String needle = "";
    private ModuleCategory category;
    private boolean liveOnly;
    private boolean favoritesOnly;
    private boolean dirty = true;

    private final List<ClientModule> results = new ArrayList<ClientModule>();

    /** One read-only view, reused so repeated reads hand out the same list instance. */
    private final List<ClientModule> view = Collections.unmodifiableList(this.results);

    /** Sets the searchable registry snapshot; an unchanged list does not invalidate the results. */
    public void source(List<ClientModule> modules) {
        List<ClientModule> next = modules == null ? Collections.<ClientModule>emptyList() : modules;
        if (next == this.source) {
            return;
        }
        this.source = next;
        invalidate();
    }

    public void query(String value) {
        String next = value == null ? "" : value;
        if (next.equals(this.query)) {
            return;
        }
        this.query = next;
        this.needle = next.toLowerCase(Locale.ENGLISH);
        invalidate();
    }

    public String query() {
        return this.query;
    }

    public void category(ModuleCategory value) {
        if (value == this.category) {
            return;
        }
        this.category = value;
        invalidate();
    }

    public ModuleCategory category() {
        return this.category;
    }

    /** @return the current live-only filter state. */
    public boolean liveOnly() {
        return this.liveOnly;
    }

    /** @return the current favorites-only filter state. */
    public boolean favoritesOnly() {
        return this.favoritesOnly;
    }

    public void liveOnly(boolean value) {
        if (value == this.liveOnly) {
            return;
        }
        this.liveOnly = value;
        invalidate();
    }

    public void favoritesOnly(boolean value) {
        if (value == this.favoritesOnly) {
            return;
        }
        this.favoritesOnly = value;
        invalidate();
    }

    /** Forces the next {@link #results()} call to recompute; used when a module state changed. */
    public void invalidate() {
        this.dirty = true;
    }

    public boolean isDirty() {
        return this.dirty;
    }

    /** @return the cached, filtered, ranked modules; recomputed only while dirty. */
    public List<ClientModule> results() {
        if (this.dirty) {
            rebuild();
        }
        return this.view;
    }

    public int size() {
        return results().size();
    }

    private void rebuild() {
        this.dirty = false;
        this.results.clear();
        for (ClientModule module : this.source) {
            if (!matchesFilter(module, this.category, this.liveOnly, this.favoritesOnly)) {
                continue;
            }
            if (!this.needle.isEmpty() && score(module, this.needle) < 0) {
                continue;
            }
            this.results.add(module);
        }
        final Map<String, Integer> scores = new HashMap<String, Integer>();
        for (ClientModule module : this.results) {
            scores.put(module.metadata().id(),
                Integer.valueOf(this.needle.isEmpty() ? 0 : score(module, this.needle)));
        }
        Collections.sort(this.results, new Comparator<ClientModule>() {
            public int compare(ClientModule a, ClientModule b) {
                Integer left = scores.get(a.metadata().id());
                Integer right = scores.get(b.metadata().id());
                if (left != null && right != null && !left.equals(right)) {
                    return right.intValue() - left.intValue();
                }
                if (a.metadata().favoriteByDefault() != b.metadata().favoriteByDefault()) {
                    return a.metadata().favoriteByDefault() ? -1 : 1;
                }
                return a.metadata().name().compareToIgnoreCase(b.metadata().name());
            }
        });
    }

    /** Category + state filters, independent of the query so they can be combined freely. */
    public static boolean matchesFilter(ClientModule module, ModuleCategory category, boolean liveOnly,
                                        boolean favoritesOnly) {
        if (module == null) {
            return false;
        }
        if (category != null && module.metadata().category() != category) {
            return false;
        }
        if (liveOnly && module.state() != ModuleState.ENABLED) {
            return false;
        }
        if (favoritesOnly && !module.metadata().favoriteByDefault()) {
            return false;
        }
        return true;
    }

    /** @return true when {@code lowerCaseQuery} fits the module at all. */
    public static boolean matches(ClientModule module, String lowerCaseQuery) {
        return lowerCaseQuery == null || lowerCaseQuery.isEmpty() || score(module, lowerCaseQuery) >= 0;
    }

    /**
     * Ranks a module against a lower-case query: name first, then id, then description and category.
     *
     * @return the best score, or -1 when the query does not fit anywhere
     */
    public static int score(ClientModule module, String needle) {
        String name = module.metadata().name().toLowerCase(Locale.ENGLISH);
        String id = module.metadata().id().toLowerCase(Locale.ENGLISH);
        String description = module.metadata().description().toLowerCase(Locale.ENGLISH);
        String category = module.metadata().category().name().toLowerCase(Locale.ENGLISH);
        int best = score(name, needle);
        best = Math.max(best, score(id, needle));
        best = Math.max(best, score(description, needle) - 6);
        best = Math.max(best, score(category, needle) - 4);
        return best;
    }

    /** Subsequence match with a contiguity bonus; returns -1 when the needle does not fit. */
    public static int score(String haystack, String needle) {
        if (needle.isEmpty()) {
            return 0;
        }
        int cursor = 0;
        int total = 0;
        int streak = 0;
        for (int i = 0; i < needle.length(); i++) {
            char want = needle.charAt(i);
            int found = haystack.indexOf(want, cursor);
            if (found < 0) {
                return -1;
            }
            if (found == cursor) {
                streak++;
                total += 4 + streak;
            } else {
                streak = 0;
                total += 2 - Math.min(2, found - cursor);
            }
            cursor = found + 1;
        }
        if (haystack.contains(needle)) {
            total += 24;
        }
        return total;
    }
}
