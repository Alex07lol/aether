package dev.aether.module.impl.cosmetics;

import dev.aether.module.AbstractModule;
import dev.aether.module.ClientModule.ModuleCategory;
import dev.aether.module.ClientModule.ModuleMetadata;

public class CosmeticManagerModule extends AbstractModule {
    public CosmeticManagerModule() {
        super(ModuleMetadata.builder("cosmetics.manager", "Cosmetic Manager")
            .category(ModuleCategory.COSMETICS)
            .description("Opens cape and cosmetic import tools.")
            .build());
    }
}
