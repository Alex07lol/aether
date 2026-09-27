package dev.aether.ui;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Where the Control Center is and what is selected: the section, the menu state, the open module,
 * the selection, the scroll offset, the expanded rows and the focused control.
 * <p>
 * Every one of those used to be a loose field on the screen class, which is why the screen could
 * reach states such as "settings open but no module selected" or "search focused while editing a
 * keybind". Here the transitions are the only way to change anything, so the invalid combinations
 * cannot be produced: opening a module's settings always names that module and always switches to
 * the Modules section, entering search always leaves a settings page, and showing a section always
 * closes whatever was open.
 * <p>
 * The class is Minecraft-free, which is what lets the layout and navigation rules be unit tested
 * without a Forge runtime.
 */
public final class ControlCenterState {
    /** What the main area is doing; the sidebar section says which page it is doing it on. */
    public enum MenuState {
        /** The page's default content. */
        BROWSING,
        /** The search field owns the keyboard. */
        SEARCHING,
        /** One module's settings are open. */
        MODULE_SETTINGS,
        /** The client-wide preferences page is showing its controls. */
        GLOBAL_SETTINGS
    }

    private ControlCenterSection section = ControlCenterSection.MODULES;
    private MenuState menuState = MenuState.BROWSING;
    private String settingsModuleId;
    private final Set<String> expanded = new LinkedHashSet<String>();
    private int selected;
    private float scroll;
    private ControlFocus focus = ControlFocus.idle();
    private String filterKey = "!all";

    public ControlCenterSection section() {
        return this.section;
    }

    /**
     * Switches page. Any open settings page, focus and module selection is dropped, because a
     * half-open state carried across pages is exactly the bug this class exists to prevent.
     */
    public void showSection(ControlCenterSection next) {
        ControlCenterSection target = next == null ? ControlCenterSection.MODULES : next;
        if (target == this.section) {
            return;
        }
        this.section = target;
        this.settingsModuleId = null;
        this.menuState = MenuState.BROWSING;
        this.focus = ControlFocus.idle();
        this.selected = 0;
        this.scroll = 0F;
        if (target == ControlCenterSection.SETTINGS) {
            this.menuState = MenuState.GLOBAL_SETTINGS;
        }
    }

    /** Cycles sections for Tab / Shift+Tab and returns the one now showing. */
    public ControlCenterSection cycleSection(int direction) {
        showSection(this.section.shift(direction >= 0 ? 1 : -1));
        return this.section;
    }

    public MenuState state() {
        return this.menuState;
    }

    public boolean isBrowsing() {
        return this.menuState == MenuState.BROWSING;
    }

    public boolean isSearching() {
        return this.menuState == MenuState.SEARCHING;
    }

    /** @return the module whose settings are open, or {@code null} when no settings page is up. */
    public String settingsModuleId() {
        return this.menuState == MenuState.MODULE_SETTINGS ? this.settingsModuleId : null;
    }

    /** @return true when the module list and its filters are the page content. */
    public boolean showsModuleList() {
        return isModuleDriven(this.section) && this.menuState != MenuState.MODULE_SETTINGS;
    }

    /** @return true when the section's rows are module rows (so the module machinery applies). */
    public static boolean isModuleDriven(ControlCenterSection section) {
        return section == ControlCenterSection.MODULES
            || section == ControlCenterSection.THEMES
            || section == ControlCenterSection.COSMETICS;
    }

    /** Focuses the search field. Leaving a settings page is part of focusing search, not a caller's job. */
    public void beginSearch() {
        if (this.menuState == MenuState.MODULE_SETTINGS || this.menuState == MenuState.GLOBAL_SETTINGS) {
            this.settingsModuleId = null;
        }
        this.menuState = MenuState.SEARCHING;
    }

    /** Leaves the search field but keeps whatever was typed, so the filter survives. */
    public void endSearch() {
        if (this.menuState == MenuState.SEARCHING) {
            this.menuState = this.section == ControlCenterSection.SETTINGS
                ? MenuState.GLOBAL_SETTINGS
                : MenuState.BROWSING;
        }
    }

    /** Opens a module's settings page. @return false when no module id was given. */
    public boolean openModuleSettings(String moduleId) {
        if (moduleId == null || moduleId.trim().isEmpty()) {
            return false;
        }
        // The section is not moved on purpose: theme and cosmetic modules show their settings on
        // their own pages, and a jump back to Modules would fight the navigation the player chose.
        this.menuState = MenuState.MODULE_SETTINGS;
        this.settingsModuleId = moduleId;
        this.focus = ControlFocus.idle();
        return true;
    }

    public void closeModuleSettings() {
        if (this.menuState == MenuState.MODULE_SETTINGS) {
            this.menuState = MenuState.BROWSING;
        }
        this.settingsModuleId = null;
    }

    public void showGlobalSettings() {
        this.section = ControlCenterSection.SETTINGS;
        this.menuState = MenuState.GLOBAL_SETTINGS;
        this.settingsModuleId = null;
        this.focus = ControlFocus.idle();
    }

    /* ── expanded rows ──────────────────────────────────────────────────── */

    /** @return true when the row is now expanded. */
    public boolean toggleExpanded(String moduleId) {
        if (moduleId == null) {
            return false;
        }
        if (this.expanded.remove(moduleId)) {
            return false;
        }
        this.expanded.add(moduleId);
        return true;
    }

    public boolean isExpanded(String moduleId) {
        return moduleId != null && this.expanded.contains(moduleId);
    }

    public void expand(Collection<String> moduleIds) {
        if (moduleIds != null) {
            this.expanded.addAll(moduleIds);
        }
    }

    public void collapseAll() {
        this.expanded.clear();
    }

    public int expandedCount() {
        return this.expanded.size();
    }

    /** @return an unmodifiable, ordered snapshot of the expanded module ids. */
    public List<String> expandedIds() {
        return Collections.unmodifiableList(new java.util.ArrayList<String>(this.expanded));
    }

    /* ── selection and scroll ───────────────────────────────────────────── */

    /**
     * The highlighted row. The list is what makes the bounds real, so selection is stored as an
     * index that is always clamped by the current list size rather than as a module, which would
     * survive a filter change and point at a row that is no longer shown.
     */
    public int selected() {
        return this.selected;
    }

    public void select(int index, int size) {
        this.selected = clamp(index, 0, Math.max(0, size - 1));
    }

    public void move(int delta, int size) {
        select(this.selected + delta, size);
    }

    public float scroll() {
        return this.scroll;
    }

    public void scrollTo(float value, float maxScroll) {
        this.scroll = clamp(value, 0F, Math.max(0F, maxScroll));
    }

    public void scrollBy(float amount, float maxScroll) {
        scrollTo(this.scroll + amount, maxScroll);
    }

    /* ── the active filter chip and control focus ───────────────────────── */

    /** The filter chip key: {@code !all}, {@code !live}, {@code !fav} or {@code cat:<CATEGORY>}. */
    public String filterKey() {
        return this.filterKey;
    }

    public void filterKey(String value) {
        this.filterKey = value == null || value.trim().isEmpty() ? "!all" : value;
    }

    public ControlFocus focus() {
        return this.focus;
    }

    public void setFocus(ControlFocus next) {
        this.focus = next == null ? ControlFocus.idle() : next;
    }

    /** Drops the focus without touching anything else; used when the screen closes. */
    public ControlFocus clearFocus() {
        ControlFocus previous = this.focus;
        this.focus = ControlFocus.idle();
        return previous;
    }

    /**
     * @return true when the state makes sense. The single invariant worth asserting is that a
     *     settings page always names a module, and that nothing names a module when no settings
     *     page is open.
     */
    public boolean consistent() {
        if (this.menuState == MenuState.MODULE_SETTINGS) {
            return this.settingsModuleId != null && isModuleDriven(this.section);
        }
        if (this.settingsModuleId != null) {
            return false;
        }
        return this.section != ControlCenterSection.SETTINGS || this.menuState == MenuState.GLOBAL_SETTINGS
            || this.menuState == MenuState.SEARCHING;
    }

    static int clamp(int value, int min, int max) {
        if (max < min) {
            return min;
        }
        return Math.max(min, Math.min(max, value));
    }

    static float clamp(float value, float min, float max) {
        if (max < min) {
            return min;
        }
        return Math.max(min, Math.min(max, value));
    }
}
