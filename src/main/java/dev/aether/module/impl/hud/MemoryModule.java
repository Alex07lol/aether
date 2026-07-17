package dev.aether.module.impl.hud;

import dev.aether.module.AbstractModule;
import dev.aether.module.ClientModule.ModuleCategory;
import dev.aether.module.ClientModule.ModuleMetadata;

public class MemoryModule extends AbstractModule {
    public MemoryModule() {
        super(ModuleMetadata.builder("hud.memory", "Memory Usage")
            .category(ModuleCategory.HUD)
            .description("Shows JVM memory usage on the HUD.")
            .build());
    }
}
