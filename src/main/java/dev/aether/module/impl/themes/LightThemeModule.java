package dev.aether.module.impl.themes;

import dev.aether.module.AbstractModule;
import dev.aether.module.ClientModule.ModuleCategory;
import dev.aether.module.ClientModule.ModuleMetadata;
import dev.aether.module.ModuleKind;
import dev.aether.theme.ThemeModule;
import dev.aether.theme.ThemePalette;
import dev.aether.theme.ThemePalettes;

public class LightThemeModule extends AbstractModule implements ThemeModule {
    public LightThemeModule() {
        super(ModuleMetadata.builder("theme.light", "Light")
            .category(ModuleCategory.THEMES)
            .description("Bright surfaces with dark text for daytime use.")
            .group(ThemeModule.GROUP)
            .kind(ModuleKind.THEME)
            .build());
    }

    public ThemePalette palette() {
        return ThemePalettes.light();
    }
}
