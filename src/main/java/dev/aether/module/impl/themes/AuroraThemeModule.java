package dev.aether.module.impl.themes;

import dev.aether.module.AbstractModule;
import dev.aether.module.ClientModule.ModuleCategory;
import dev.aether.module.ClientModule.ModuleMetadata;
import dev.aether.theme.ThemeModule;
import dev.aether.theme.ThemePalette;
import dev.aether.theme.ThemePalettes;

public class AuroraThemeModule extends AbstractModule implements ThemeModule {
    public AuroraThemeModule() {
        super(ModuleMetadata.builder("theme.aurora", "Aurora")
            .category(ModuleCategory.THEMES)
            .description("Dark teal surfaces with a green aurora accent.")
            .group(ThemeModule.GROUP)
            .build());
    }

    public ThemePalette palette() {
        return ThemePalettes.aurora();
    }
}
