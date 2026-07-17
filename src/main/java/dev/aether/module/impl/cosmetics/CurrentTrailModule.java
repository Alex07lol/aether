package dev.aether.module.impl.cosmetics;

import dev.aether.module.AbstractModule;
import dev.aether.module.ClientModule.ModuleCategory;
import dev.aether.module.ClientModule.ModuleMetadata;

public class CurrentTrailModule extends AbstractModule {
    public CurrentTrailModule() {
        super(ModuleMetadata.builder("cosmetics.current_trail", "Current Trail")
            .category(ModuleCategory.COSMETICS)
            .description("Shows and manages the selected trail.")
            .build());
    }
}
