package dev.aether.module.impl.pvp;

import dev.aether.module.AbstractModule;
import dev.aether.module.ClientModule.ModuleCategory;
import dev.aether.module.ClientModule.ModuleMetadata;

public class SnaplookModule extends AbstractModule {
    public SnaplookModule() {
        super(ModuleMetadata.builder("pvp.snaplook", "Snaplook")
            .category(ModuleCategory.PVP)
            .description("Allows you to view in 3rd person while holding a button.")
            .build());

        addKeybind("keybind", "Keybind", 33);
    }
}
