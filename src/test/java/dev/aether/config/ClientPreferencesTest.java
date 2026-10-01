package dev.aether.config;

import dev.aether.TestSupport;

/**
 * Guards {@link ClientPreferences}: defaults, config round trip, and the section normalisation the
 * GUI relies on to never open on a section that does not exist.
 */
public final class ClientPreferencesTest {
    private ClientPreferencesTest() {
    }

    public static void main(String[] args) {
        defaults();
        roundTrip();
        sectionNormalisation();
        invertScroll();
        System.out.println("ClientPreferencesTest passed");
    }

    private static void defaults() {
        ClientPreferences preferences = new ClientPreferences();
        TestSupport.assertTrue(preferences.saveOnClose(), "the config is saved on close by default");
        TestSupport.assertTrue(preferences.showTooltips(), "hover tooltips are on by default");
        TestSupport.assertEquals("Modules", preferences.openSection(), "the GUI opens on the modules");
    }

    private static void roundTrip() {
        ClientPreferences preferences = new ClientPreferences();
        preferences.setSaveOnClose(false);
        preferences.setShowTooltips(false);
        preferences.setInvertScroll(true);
        preferences.setOpenSection("Cosmetics");

        ConfigDocument.Builder builder = ConfigDocument.builder();
        preferences.writeConfig(builder);
        ConfigDocument document = builder.build();

        ClientPreferences restored = new ClientPreferences();
        restored.applyConfig(document);
        TestSupport.assertTrue(!restored.saveOnClose(), "the save-on-close choice round trips");
        TestSupport.assertTrue(!restored.showTooltips(), "the tooltip choice round trips");
        TestSupport.assertTrue(restored.invertScroll(), "the scroll direction round trips");
        TestSupport.assertEquals("Cosmetics", restored.openSection(), "the section round trips");

        ClientPreferences untouched = new ClientPreferences();
        untouched.applyConfig(ConfigDocument.empty());
        TestSupport.assertTrue(untouched.saveOnClose(), "an empty config leaves the defaults alone");
    }

    private static void sectionNormalisation() {
        ClientPreferences preferences = new ClientPreferences();
        preferences.setOpenSection("hud editor");
        TestSupport.assertEquals("HUD Editor", preferences.openSection(), "section names are case-insensitive");
        preferences.setOpenSection("nonsense");
        TestSupport.assertEquals("Modules", preferences.openSection(), "an unknown section falls back");
        TestSupport.assertEquals(0, ClientPreferences.sectionIndex("nonsense"), "and indexes to the first");
        TestSupport.assertEquals(4, ClientPreferences.sectionIndex("Settings"), "known sections keep their order");
        TestSupport.assertEquals(5, ClientPreferences.SECTIONS.length, "the GUI ships five sections");
        TestSupport.assertEquals("Cosmetics", ClientPreferences.SECTIONS[1], "Cosmetics follows Modules");
        // The order is also the navigation bar's x order, and the x positions are Leaf's own tile
        // rectangles (430/650/1100/1320) with Aether's Themes tile in the free slot at 860 - so the
        // HUD editor has to stay third of the four Leaf tiles, at index 3.
        TestSupport.assertEquals("Themes", ClientPreferences.SECTIONS[2], "Themes takes the middle tile");
        TestSupport.assertEquals("HUD Editor", ClientPreferences.SECTIONS[3], "the HUD editor keeps Leaf's inner tile");
        TestSupport.assertEquals("Themes", ClientPreferences.normalizeSection("themes"), "the Themes section normalises");
    }

    private static void invertScroll() {
        ClientPreferences preferences = new ClientPreferences();
        TestSupport.assertTrue(!preferences.invertScroll(), "the wheel scrolls like vanilla by default");
        preferences.setInvertScroll(true);
        TestSupport.assertTrue(preferences.invertScroll(), "the flip takes immediately");
    }
}
