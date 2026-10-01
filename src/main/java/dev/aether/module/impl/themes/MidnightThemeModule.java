package dev.aether.module.impl.themes;

import dev.aether.module.AbstractModule;
import dev.aether.module.ClientModule.ModuleCategory;
import dev.aether.module.ClientModule.ModuleMetadata;
import dev.aether.module.ModuleKind;
import dev.aether.theme.ThemeModule;
import dev.aether.theme.ThemePalette;
import dev.aether.theme.ThemePalettes;

public class MidnightThemeModule extends AbstractModule implements ThemeModule {
    public MidnightThemeModule() {
        super(ModuleMetadata.builder("theme.midnight", "Midnight")
            .category(ModuleCategory.THEMES)
            .description("Near-black surfaces with a violet accent for low-light sessions.")
            .group(ThemeModule.GROUP)
            .kind(ModuleKind.THEME)
            .build());
    }

    public ThemePalette palette() {
        return ThemePalettes.midnight();
    }
}
