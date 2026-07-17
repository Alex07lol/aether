package dev.aether.module.impl.themes;

import dev.aether.module.AbstractModule;
import dev.aether.module.ClientModule.ModuleCategory;
import dev.aether.module.ClientModule.ModuleMetadata;

public class FrostThemeModule extends AbstractModule {
    public FrostThemeModule() {
        super(ModuleMetadata.builder("theme.frost", "Frost")
            .category(ModuleCategory.THEMES)
            .description("Switches the Click GUI to the frost theme.")
            .build());
    }
}
