package dev.aether.module.impl.cosmetics;

import dev.aether.module.AbstractModule;
import dev.aether.module.ClientModule.ModuleCategory;
import dev.aether.module.ClientModule.ModuleMetadata;

public class CosmeticManagerModule extends AbstractModule {
    public static final String ID = "cosmetics.manager";

    public CosmeticManagerModule() {
        super(ModuleMetadata.builder(ID, "Cosmetic Manager")
            .category(ModuleCategory.COSMETICS)
            .description("Opens the cosmetics screen when switched on.")
            .build());
    }
}
