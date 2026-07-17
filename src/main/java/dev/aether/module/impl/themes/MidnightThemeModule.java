package dev.aether.module.impl.themes;

import dev.aether.module.AbstractModule;
import dev.aether.module.ClientModule.ModuleCategory;
import dev.aether.module.ClientModule.ModuleMetadata;

public class MidnightThemeModule extends AbstractModule {
    public MidnightThemeModule() {
        super(ModuleMetadata.builder("theme.midnight", "Midnight")
            .category(ModuleCategory.THEMES)
            .description("Switches the Click GUI to a dark midnight theme.")
            .build());
    }
}
