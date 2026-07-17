package dev.aether.module.impl.themes;

import dev.aether.module.AbstractModule;
import dev.aether.module.ClientModule.ModuleCategory;
import dev.aether.module.ClientModule.ModuleMetadata;

public class LightThemeModule extends AbstractModule {
    public LightThemeModule() {
        super(ModuleMetadata.builder("theme.light", "Light")
            .category(ModuleCategory.THEMES)
            .description("Switches the Click GUI to a bright light theme.")
            .build());
    }
}
