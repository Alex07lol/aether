package dev.aether.module.impl.interface_;

import dev.aether.module.AbstractModule;
import dev.aether.module.ClientModule.ModuleCategory;
import dev.aether.module.ClientModule.ModuleMetadata;

public class BossbarModule extends AbstractModule {
    public BossbarModule() {
        super(ModuleMetadata.builder("interface.bossbar", "Bossbar")
            .category(ModuleCategory.INTERFACE)
            .description("Adds customizable tweaks to the Bossbar.")
            .build());
    }
}
