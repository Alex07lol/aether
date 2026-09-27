package dev.aether.ui;

import dev.aether.TestSupport;

/**
 * Guards {@link ControlCenterState}: the state machine's transitions, the invariants that make
 * impossible screen states unrepresentable, and the selection/scroll clamping.
 */
public final class ControlCenterStateTest {
    private ControlCenterStateTest() {
    }

    public static void main(String[] args) {
        defaults();
        sectionSwitches();
        moduleSettingsLifecycle();
        searchTransitions();
        expansionAndSelection();
        filterKeys();
        consistencyInvariant();
        System.out.println("ControlCenterStateTest passed");
    }

    private static void defaults() {
        ControlCenterState state = new ControlCenterState();
        TestSupport.assertEquals(ControlCenterSection.MODULES, state.section(), "the deck opens on Modules");
        TestSupport.assertEquals(ControlCenterState.MenuState.BROWSING, state.state(), "and starts browsing");
        TestSupport.assertTrue(state.isBrowsing(), "browsing is the default state");
        TestSupport.assertTrue(state.showsModuleList(), "Modules is a module-driven page");
        TestSupport.assertTrue(state.consistent(), "the default state is consistent");
    }

    private static void sectionSwitches() {
        ControlCenterState state = new ControlCenterState();
        state.showSection(ControlCenterSection.PROFILES);
        TestSupport.assertEquals(ControlCenterSection.PROFILES, state.section(), "showSection moves the page");
        TestSupport.assertTrue(state.isBrowsing(), "and leaves browsing mode");
        TestSupport.assertTrue(state.consistent(), "a plain section switch is consistent");

        state.showSection(ControlCenterSection.SETTINGS);
        TestSupport.assertEquals(ControlCenterState.MenuState.GLOBAL_SETTINGS, state.state(),
            "the Settings page is the global settings page");
        TestSupport.assertTrue(state.consistent(), "the settings page is consistent");

        state.showSection(ControlCenterSection.MODULES);
        TestSupport.assertEquals(ControlCenterState.MenuState.BROWSING, state.state(),
            "leaving Settings returns to browsing");

        // cycling covers every section and wraps in both directions
        ControlCenterState cycle = new ControlCenterState();
        for (int i = 0; i < ControlCenterSection.ordered().length * 2; i++) {
            cycle.cycleSection(1);
            TestSupport.assertTrue(cycle.consistent(), "cycling forwards stays consistent");
        }
        cycle.cycleSection(-1);
        TestSupport.assertTrue(cycle.consistent(), "cycling backwards stays consistent");
    }

    private static void moduleSettingsLifecycle() {
        ControlCenterState state = new ControlCenterState();
        TestSupport.assertTrue(!state.openModuleSettings(null), "a null module id is refused");
        TestSupport.assertTrue(!state.openModuleSettings("  "), "a blank module id is refused");
        TestSupport.assertTrue(state.consistent(), "refusing to open leaves the state untouched");

        TestSupport.assertTrue(state.openModuleSettings("pvp.zoom"), "opening a real module works");
        TestSupport.assertEquals(ControlCenterState.MenuState.MODULE_SETTINGS, state.state(), "the state is MODULE_SETTINGS");
        TestSupport.assertEquals("pvp.zoom", state.settingsModuleId(), "and names the module");
        TestSupport.assertTrue(state.consistent(), "module settings are consistent");
        TestSupport.assertTrue(!state.showsModuleList(), "the list is replaced by the settings page");

        state.closeModuleSettings();
        TestSupport.assertTrue(state.isBrowsing(), "closing returns to browsing");
        TestSupport.assertEquals(null, state.settingsModuleId(), "and forgets the module");
    }

    private static void searchTransitions() {
        ControlCenterState state = new ControlCenterState();
        state.openModuleSettings("pvp.zoom");
        state.beginSearch();
        TestSupport.assertEquals(ControlCenterState.MenuState.SEARCHING, state.state(),
            "focusing search from a settings page leaves the settings page");
        TestSupport.assertEquals(null, state.settingsModuleId(), "and drops the open module");
        state.endSearch();
        TestSupport.assertTrue(state.isBrowsing(), "ending search returns to browsing");

        // Searching on the Settings page returns to global settings, not browsing.
        ControlCenterState onSettings = new ControlCenterState();
        onSettings.showSection(ControlCenterSection.SETTINGS);
        onSettings.beginSearch();
        onSettings.endSearch();
        TestSupport.assertEquals(ControlCenterState.MenuState.GLOBAL_SETTINGS, onSettings.state(),
            "search on the Settings page lands back on global settings");
    }

    private static void expansionAndSelection() {
        ControlCenterState state = new ControlCenterState();
        TestSupport.assertTrue(state.toggleExpanded("a"), "the first toggle expands");
        TestSupport.assertTrue(state.isExpanded("a"), "the row is expanded");
        TestSupport.assertTrue(!state.toggleExpanded("a"), "the second toggle collapses");
        TestSupport.assertTrue(!state.isExpanded("a"), "the row is collapsed again");
        TestSupport.assertTrue(!state.toggleExpanded(null), "a null id never expands");

        state.expand(java.util.Arrays.asList("x", "y"));
        TestSupport.assertEquals(2, state.expandedCount(), "bulk expand registers both rows");
        TestSupport.assertTrue(state.isExpanded("x") && state.isExpanded("y"), "both are expanded");
        state.collapseAll();
        TestSupport.assertEquals(0, state.expandedCount(), "collapse all empties the set");

        state.select(3, 5);
        TestSupport.assertEquals(3, state.selected(), "selection moves to the index");
        state.select(99, 5);
        TestSupport.assertEquals(4, state.selected(), "and is clamped to the list");
        state.select(0, 0);
        TestSupport.assertEquals(0, state.selected(), "an empty list clamps to zero");
        state.move(10, 3);
        TestSupport.assertEquals(2, state.selected(), "move clamps at the end");
        state.move(-10, 3);
        TestSupport.assertEquals(0, state.selected(), "move clamps at the start");

        state.scrollTo(100F, 50F);
        TestSupport.assertEquals(50F, state.scroll(), "scroll clamps to max");
        state.scrollBy(-100F, 50F);
        TestSupport.assertEquals(0F, state.scroll(), "scroll never goes negative");
    }

    private static void filterKeys() {
        ControlCenterState state = new ControlCenterState();
        TestSupport.assertEquals("!all", state.filterKey(), "the default filter shows everything");
        state.filterKey("cat:PVP");
        TestSupport.assertEquals("cat:PVP", state.filterKey(), "a category filter is stored");
        state.filterKey(null);
        TestSupport.assertEquals("!all", state.filterKey(), "a null filter falls back to all");
        state.filterKey("");
        TestSupport.assertEquals("!all", state.filterKey(), "an empty filter falls back to all");
    }

    private static void consistencyInvariant() {
        ControlCenterState state = new ControlCenterState();
        // The one impossible state the old boolean soup allowed: settings open, no module named.
        state.openModuleSettings("pvp.zoom");
        TestSupport.assertTrue(state.consistent(), "settings with a module is consistent");
        // Simulate corruption: forget the module while the settings page is open.
        state.openModuleSettings(null); // refused, state unchanged
        TestSupport.assertTrue(state.consistent(), "a refused open keeps consistency");
        state.showSection(ControlCenterSection.PROFILES);
        TestSupport.assertTrue(state.consistent(), "switching sections resets to a consistent state");
    }
}
