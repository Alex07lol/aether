package dev.aether.module.impl.themes;

import dev.aether.module.AbstractModule;
import dev.aether.module.ClientModule.ModuleCategory;
import dev.aether.module.ClientModule.ModuleMetadata;
import dev.aether.module.ModuleKind;
import dev.aether.theme.ThemeModule;
import dev.aether.theme.ThemePalette;
import dev.aether.theme.ThemePalettes;

public class FrostThemeModule extends AbstractModule implements ThemeModule {
    public FrostThemeModule() {
        super(ModuleMetadata.builder("theme.frost", "Frost")
            .category(ModuleCategory.THEMES)
            .description("Cool blue-grey surfaces with an icy highlight accent.")
            .group(ThemeModule.GROUP)
            .kind(ModuleKind.THEME)
            .build());
    }

    public ThemePalette palette() {
        return ThemePalettes.frost();
    }
}
