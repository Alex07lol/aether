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
        TestSupport.assertEquals(1, ClientPreferences.sectionIndex("nonsense"), "Modules is the fallback destination");
        // The order is the navigation order: Home, Modules, Cosmetics, HUD Editor,
        // Appearance, Profiles, Settings - the compact navigation's rail order, with the
        // HUD editor last because it is a fullscreen destination, not a routed category.
        TestSupport.assertEquals("Home", ClientPreferences.SECTIONS[0], "Home opens the menu");
        TestSupport.assertEquals("Modules", ClientPreferences.SECTIONS[1], "Modules follows Home");
        TestSupport.assertEquals("Cosmetics", ClientPreferences.SECTIONS[2], "Cosmetics follows Modules");
        TestSupport.assertEquals("Appearance", ClientPreferences.SECTIONS[4],
            "Appearance is where a theme is worn");
        TestSupport.assertEquals("Profiles", ClientPreferences.SECTIONS[5], "Profiles is its own destination");
        TestSupport.assertEquals("Settings", ClientPreferences.SECTIONS[6], "Settings closes the row");
        TestSupport.assertEquals(7, ClientPreferences.SECTIONS.length, "the GUI ships seven destinations");
        // The retired Themes destination must load as Appearance rather than fall back to Modules.
        TestSupport.assertEquals("Appearance", ClientPreferences.normalizeSection("themes"),
            "the Themes section maps onto Appearance");
        preferences.setOpenSection("Themes");
        TestSupport.assertEquals("Appearance", preferences.openSection(),
            "a config remembering Themes opens Appearance");
    }

    private static void invertScroll() {
        ClientPreferences preferences = new ClientPreferences();
        TestSupport.assertTrue(!preferences.invertScroll(), "the wheel scrolls like vanilla by default");
        preferences.setInvertScroll(true);
        TestSupport.assertTrue(preferences.invertScroll(), "the flip takes immediately");
    }
}
