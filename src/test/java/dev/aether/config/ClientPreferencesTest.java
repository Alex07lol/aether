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
        preferences.setOpenSection("Cosmetics");

        ConfigDocument.Builder builder = ConfigDocument.builder();
        preferences.writeConfig(builder);
        ConfigDocument document = builder.build();

        ClientPreferences restored = new ClientPreferences();
        restored.applyConfig(document);
        TestSupport.assertTrue(!restored.saveOnClose(), "the save-on-close choice round trips");
        TestSupport.assertTrue(!restored.showTooltips(), "the tooltip choice round trips");
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
        TestSupport.assertEquals(3, ClientPreferences.sectionIndex("Settings"), "known sections keep their order");
        TestSupport.assertEquals(4, ClientPreferences.SECTIONS.length, "the GUI ships four sections");
        TestSupport.assertEquals("Cosmetics", ClientPreferences.SECTIONS[1], "Cosmetics follows Modules");
    }
}
