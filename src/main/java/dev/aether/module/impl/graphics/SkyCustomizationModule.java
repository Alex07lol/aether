package dev.aether.module.impl.graphics;

import dev.aether.module.AbstractModule;
import dev.aether.module.ClientModule.ModuleCategory;
import dev.aether.module.ClientModule.ModuleMetadata;

public class SkyCustomizationModule extends AbstractModule {
    public SkyCustomizationModule() {
        super(ModuleMetadata.builder("graphics.sky_customization", "Sky Customization")
            .category(ModuleCategory.GRAPHICS)
            .description("Customizes the sky rendering with configurable colors, fog, and cloud styles.")
            .build());
    }
}
