package dev.aether.module.impl.cosmetics;

import dev.aether.module.AbstractModule;
import dev.aether.module.ClientModule.ModuleCategory;
import dev.aether.module.ClientModule.ModuleMetadata;

public class CurrentHatModule extends AbstractModule {
    public CurrentHatModule() {
        super(ModuleMetadata.builder("cosmetics.current_hat", "Current Hat")
            .category(ModuleCategory.COSMETICS)
            .description("Shows and manages the selected hat.")
            .build());
    }
}
