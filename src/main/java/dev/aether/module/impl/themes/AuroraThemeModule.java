package dev.aether.module.impl.themes;

import dev.aether.module.AbstractModule;
import dev.aether.module.ClientModule.ModuleCategory;
import dev.aether.module.ClientModule.ModuleMetadata;

public class AuroraThemeModule extends AbstractModule {
    public AuroraThemeModule() {
        super(ModuleMetadata.builder("theme.aurora", "Aurora")
            .category(ModuleCategory.THEMES)
            .description("Switches the Click GUI to an aurora accent theme.")
            .build());
    }
}
