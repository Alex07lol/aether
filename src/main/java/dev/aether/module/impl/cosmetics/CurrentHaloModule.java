package dev.aether.module.impl.cosmetics;

import dev.aether.module.AbstractModule;
import dev.aether.module.ClientModule.ModuleCategory;
import dev.aether.module.ClientModule.ModuleMetadata;

public class CurrentHaloModule extends AbstractModule {
    public CurrentHaloModule() {
        super(ModuleMetadata.builder("cosmetics.current_halo", "Current Halo")
            .category(ModuleCategory.COSMETICS)
            .description("Shows and manages the selected halo.")
            .build());
    }
}
