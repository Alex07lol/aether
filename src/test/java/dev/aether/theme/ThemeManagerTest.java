package dev.aether.theme;

import dev.aether.AetherClient;
import dev.aether.TestSupport;
import dev.aether.config.ConfigDocument;

import java.nio.file.Paths;

/**
 * Covers the theme system's own contract now that palettes live outside the module registry:
 * the manager owns which theme is worn, the client resolves the resolved palette to a stable
 * instance between frames, and the chosen theme round-trips through the config - including
 * configs written by the builds that enabled palettes through module keys.
 */
public final class ThemeManagerTest {
    public static void main(String[] args) {
        managerBasics();
        clientResolution();
        configRoundTrip();
        moduleEraMigration();

        long midnightPalette = paletteSignature(ThemePalettes.midnight());
        long auroraPalette = paletteSignature(ThemePalettes.aurora());
        TestSupport.assertTrue(midnightPalette != auroraPalette, "Built-in palettes should differ.");
        TestSupport.assertTrue(ThemePalettes.isLightSurface(ThemePalettes.light()),
            "The Light palette needs a light surface flag.");
        TestSupport.assertTrue(!ThemePalettes.isLightSurface(ThemePalettes.midnight()),
            "The Midnight palette is not a light surface.");

        System.out.println("ThemeManagerTest passed");
    }

    private static void managerBasics() {
        ThemeManager manager = new ThemeManager();
        TestSupport.assertTrue(manager.count() >= 6, "the manager ships the built-in palettes");
        TestSupport.assertEquals(ThemeManager.DEFAULT_THEME_ID, manager.activeId(),
            "the default palette is worn first");
        TestSupport.assertTrue(manager.isDefaultActive(), "and reports itself as the default");
        TestSupport.assertTrue(manager.definitionOf(ThemeManager.DEFAULT_THEME_ID) != null,
            "the default theme resolves");
        TestSupport.assertTrue(manager.definitionOf("theme.nonexistent") == null,
            "an unknown id does not resolve");
        TestSupport.assertTrue(manager.definitionOf(null) == null, "null does not resolve");

        manager.select("theme.aurora");
        TestSupport.assertEquals("theme.aurora", manager.activeId(), "a known id is worn");
        manager.select("theme.nonexistent");
        TestSupport.assertEquals("theme.aurora", manager.activeId(), "an unknown id keeps the current one");

        manager.select("theme.aurora");
        manager.resetToDefault();
        TestSupport.assertTrue(manager.isDefaultActive(), "reset returns to the default");

        // Definitions carry their palette and id, in a stable order.
        ThemeDefinition[] themes = manager.themes();
        TestSupport.assertEquals(manager.count(), themes.length, "every theme materialises");
        TestSupport.assertEquals(ThemePalettes.mono(), themes[0].palette(),
            "the first theme is the default palette");
    }

    private static void clientResolution() {
        AetherClient client = new AetherClient(Paths.get("build", "test-config", "theme.json"));
        client.themes().resetToDefault();

        AetherTheme fallback = client.theme();
        TestSupport.assertEquals(fallback, client.theme(), "the default theme should be a cached instance.");

        client.themes().select("theme.midnight");
        AetherTheme midnight = client.theme();
        TestSupport.assertEquals("Midnight", midnight.name(), "selecting a theme becomes the active theme.");
        TestSupport.assertEquals(ThemePalettes.midnight(), midnight.palette(),
            "the active theme exposes its own palette.");
        TestSupport.assertEquals(midnight, client.theme(), "the resolved theme should be cached between frames.");
        TestSupport.assertEquals("theme.midnight", midnight.id(), "and knows which theme it is.");

        client.themes().select("theme.aurora");
        TestSupport.assertEquals("Aurora", client.theme().name(), "the newest selection should win.");

        client.themes().resetToDefault();
        TestSupport.assertEquals(fallback.name(), client.theme().name(),
            "resetting returns to the built-in default.");
    }

    private static void configRoundTrip() {
        ThemeManager manager = new ThemeManager();
        manager.select("theme.frost");

        ConfigDocument.Builder builder = ConfigDocument.builder();
        manager.writeConfig(builder);
        ConfigDocument document = builder.build();

        ThemeManager restored = new ThemeManager();
        restored.applyConfig(document);
        TestSupport.assertEquals("theme.frost", restored.activeId(), "the worn theme round trips");

        // The default writes nothing: a fresh config has no theme key at all.
        ThemeManager defaults = new ThemeManager();
        ConfigDocument.Builder empty = ConfigDocument.builder();
        defaults.writeConfig(empty);
        TestSupport.assertTrue(!empty.build().values().containsKey("theme.active"),
            "the default theme does not need a stored key");
    }

    private static void moduleEraMigration() {
        // Configs written while palettes were enabled through the registry read once here.
        ConfigDocument.Builder builder = ConfigDocument.builder();
        builder.putBoolean("module.theme.midnight.enabled", true);
        builder.putBoolean("module.theme.aurora.enabled", false);
        ThemeManager manager = new ThemeManager();
        manager.applyConfig(builder.build());
        TestSupport.assertEquals("theme.midnight", manager.activeId(),
            "an enabled module-era theme becomes the worn one");

        // A stored key wins over the module-era leftovers when both exist.
        ConfigDocument.Builder both = ConfigDocument.builder();
        both.put("theme.active", "theme.frost");
        both.putBoolean("module.theme.midnight.enabled", true);
        ThemeManager second = new ThemeManager();
        second.applyConfig(both.build());
        TestSupport.assertEquals("theme.frost", second.activeId(),
            "the new key is authoritative when present");
    }

    private static long paletteSignature(ThemePalette palette) {
        return ((long) rgb(palette.surface()) << 24) | ((long) rgb(palette.surfaceSoft()) << 16)
            | ((long) rgb(palette.accent()) << 8) | rgb(palette.text());
    }

    private static int rgb(ColorRgb rgb) {
        return (rgb.red() << 16) | (rgb.green() << 8) | rgb.blue();
    }
}
