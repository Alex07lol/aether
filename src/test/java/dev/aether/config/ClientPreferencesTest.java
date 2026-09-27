package dev.aether.config;

import dev.aether.TestSupport;

/**
 * Guards {@link ClientPreferences}: defaults, config round trip, and the section normalisation the
 * Control Center relies on to never open on a section that does not exist.
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
        TestSupport.assertEquals("Modules", preferences.openSection(), "the deck opens on the modules");
    }

    private static void roundTrip() {
        ClientPreferences preferences = new ClientPreferences();
        preferences.setSaveOnClose(false);
        preferences.setShowTooltips(false);
        preferences.setOpenSection("Profiles");

        ConfigDocument.Builder builder = ConfigDocument.builder();
        preferences.writeConfig(builder);
        ConfigDocument document = builder.build();

        ClientPreferences restored = new ClientPreferences();
        restored.applyConfig(document);
        TestSupport.assertTrue(!restored.saveOnClose(), "the save-on-close choice round trips");
        TestSupport.assertTrue(!restored.showTooltips(), "the tooltip choice round trips");
        TestSupport.assertEquals("Profiles", restored.openSection(), "the section round trips");

        ClientPreferences untouched = new ClientPreferences();
        untouched.applyConfig(ConfigDocument.empty());
        TestSupport.assertTrue(untouched.saveOnClose(), "an empty config leaves the defaults alone");
    }

    private static void sectionNormalisation() {
        ClientPreferences preferences = new ClientPreferences();
        preferences.setOpenSection("themes");
        TestSupport.assertEquals("Themes", preferences.openSection(), "section names are case-insensitive");
        preferences.setOpenSection("nonsense");
        TestSupport.assertEquals("Modules", preferences.openSection(), "an unknown section falls back");
        TestSupport.assertEquals(0, ClientPreferences.sectionIndex("nonsense"), "and indexes to the first");
        TestSupport.assertEquals(5, ClientPreferences.sectionIndex("Settings"), "known sections keep their order");
        TestSupport.assertEquals(6, ClientPreferences.SECTIONS.length, "the Control Center ships six sections");
        TestSupport.assertEquals("Screenshots", ClientPreferences.SECTIONS[4], "Screenshots sits between Cosmetics and Settings");
    }
}
