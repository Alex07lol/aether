package dev.aether.module.impl.themes;

import dev.aether.module.AbstractModule;
import dev.aether.module.ClientModule.ModuleCategory;
import dev.aether.module.ClientModule.ModuleMetadata;
import dev.aether.theme.ThemeModule;
import dev.aether.theme.ThemePalette;
import dev.aether.theme.ThemePalettes;

public class AetherBlueThemeModule extends AbstractModule implements ThemeModule {
    public AetherBlueThemeModule() {
        super(ModuleMetadata.builder("theme.aether_blue", "Aether Blue")
            .category(ModuleCategory.THEMES)
            .description("Default Aether palette: deep navy surfaces with a blue accent.")
            .group(ThemeModule.GROUP)
            .favoriteByDefault(true)
            .build());
    }

    public ThemePalette palette() {
        return ThemePalettes.aetherBlue();
    }
}
