package dev.aether.theme;

import dev.aether.AetherClient;
import dev.aether.TestSupport;
import dev.aether.module.ClientModule;
import dev.aether.module.ClientModule.ModuleState;
import dev.aether.module.ModuleRegistry;

import java.nio.file.Paths;

/**
 * Covers the contract the Forge layer relies on: the five theme modules are mutually
 * exclusive through their registry group, and {@link AetherClient#theme()} resolves to the
 * enabled palette while handing out a stable instance between frames.
 */
public final class ThemeModuleTest {
    public static void main(String[] args) {
        AetherClient client = new AetherClient(Paths.get("build", "test-config", "theme.json"));
        ModuleRegistry modules = client.modules();

        for (String id : new String[] {"theme.aether_blue", "theme.midnight", "theme.aurora", "theme.frost", "theme.light"}) {
            ClientModule module = modules.get(id);
            TestSupport.assertTrue(module instanceof ThemeModule, id + " should implement ThemeModule.");
            TestSupport.assertEquals(ThemeModule.GROUP, module.metadata().group(), id + " should join the theme group.");
        }

        AetherTheme fallback = client.theme();
        TestSupport.assertEquals(fallback, client.theme(), "The default theme should be a cached instance.");

        modules.setEnabled("theme.midnight", true);
        AetherTheme midnight = client.theme();
        TestSupport.assertEquals("Midnight", midnight.name(), "Enabling a theme should become the active theme.");
        TestSupport.assertEquals(ThemePalettes.midnight(), midnight.palette(), "Active theme should expose its own palette.");
        TestSupport.assertEquals(midnight, client.theme(), "The resolved theme should be cached between frames.");

        // Enabling another theme must switch the group, not stack two palettes.
        modules.setEnabled("theme.aurora", true);
        TestSupport.assertEquals(ModuleState.DISABLED, modules.get("theme.midnight").state(),
            "Enabling a theme should disable the previous one.");
        TestSupport.assertEquals("Aurora", client.theme().name(), "The newest theme should win.");

        long midnightPalette = paletteSignature(ThemePalettes.midnight());
        long auroraPalette = paletteSignature(ThemePalettes.aurora());
        TestSupport.assertTrue(midnightPalette != auroraPalette, "Built-in palettes should differ.");
        TestSupport.assertTrue(ThemePalettes.isLightSurface(ThemePalettes.light()), "The Light palette needs a light surface flag.");
        TestSupport.assertTrue(!ThemePalettes.isLightSurface(ThemePalettes.midnight()), "The Midnight palette is not a light surface.");

        modules.setEnabled("theme.aurora", false);
        TestSupport.assertEquals(fallback, client.theme(), "Turning every theme off should return the built-in default.");
    }

    private static long paletteSignature(ThemePalette palette) {
        return ((long) rgb(palette.surface()) << 24) | ((long) rgb(palette.surfaceSoft()) << 16)
            | ((long) rgb(palette.accent()) << 8) | rgb(palette.text());
    }

    private static long rgb(ColorRgb rgb) {
        return (rgb.red() << 16) | (rgb.green() << 8) | rgb.blue();
    }
}
