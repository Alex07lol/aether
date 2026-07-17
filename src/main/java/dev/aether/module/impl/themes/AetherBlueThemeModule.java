package dev.aether.module.impl.themes;

import dev.aether.module.AbstractModule;
import dev.aether.module.ClientModule.ModuleCategory;
import dev.aether.module.ClientModule.ModuleMetadata;

public class AetherBlueThemeModule extends AbstractModule {
    public AetherBlueThemeModule() {
        super(ModuleMetadata.builder("theme.aether_blue", "Aether Blue")
            .category(ModuleCategory.THEMES)
            .description("Switches the Click GUI to the Aether Blue theme.")
            .build());
    }
}
