package dev.aether.module.impl.cosmetics;

import dev.aether.module.AbstractModule;
import dev.aether.module.ClientModule.ModuleCategory;
import dev.aether.module.ClientModule.ModuleMetadata;

public class CurrentWingsModule extends AbstractModule {
    public CurrentWingsModule() {
        super(ModuleMetadata.builder("cosmetics.current_wings", "Current Wings")
            .category(ModuleCategory.COSMETICS)
            .description("Shows and manages the selected wings.")
            .build());
    }
}
