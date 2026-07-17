package dev.aether.module.impl.cosmetics;

import dev.aether.module.AbstractModule;
import dev.aether.module.ClientModule.ModuleCategory;
import dev.aether.module.ClientModule.ModuleMetadata;

public class CurrentCapeModule extends AbstractModule {
    public CurrentCapeModule() {
        super(ModuleMetadata.builder("cosmetics.current_cape", "Current Cape")
            .category(ModuleCategory.COSMETICS)
            .description("Shows and manages the selected cape.")
            .build());
    }
}
