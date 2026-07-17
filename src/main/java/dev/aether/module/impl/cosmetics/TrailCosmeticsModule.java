package dev.aether.module.impl.cosmetics;

import dev.aether.module.AbstractModule;
import dev.aether.module.ClientModule.ModuleCategory;
import dev.aether.module.ClientModule.ModuleMetadata;

public class TrailCosmeticsModule extends AbstractModule {
    public TrailCosmeticsModule() {
        super(ModuleMetadata.builder("cosmetics.trails", "Trail Cosmetics")
            .category(ModuleCategory.COSMETICS)
            .description("Adds a reserved trail cosmetic slot.")
            .build());
    }
}
