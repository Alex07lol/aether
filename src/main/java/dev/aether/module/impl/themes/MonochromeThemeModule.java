package dev.aether.module.impl.themes;

import dev.aether.module.AbstractModule;
import dev.aether.module.ClientModule.ModuleCategory;
import dev.aether.module.ClientModule.ModuleMetadata;
import dev.aether.module.ModuleKind;
import dev.aether.theme.ThemeModule;
import dev.aether.theme.ThemePalette;
import dev.aether.theme.ThemePalettes;

/**
 * Translucent black and white, the palette the client defaults to and the one the ported Leaf
 * screens are designed around. It exists as a module as well as a default so a player who switches
 * to a coloured theme can switch back.
 */
public class MonochromeThemeModule extends AbstractModule implements ThemeModule {
    public MonochromeThemeModule() {
        super(ModuleMetadata.builder("theme.monochrome", "Monochrome")
            .category(ModuleCategory.THEMES)
            .description("Translucent black glass with white detail; the default Aether palette.")
            .group(ThemeModule.GROUP)
            .kind(ModuleKind.THEME)
            .favoriteByDefault(true)
            .build());
    }

    public ThemePalette palette() {
        return ThemePalettes.mono();
    }
}
