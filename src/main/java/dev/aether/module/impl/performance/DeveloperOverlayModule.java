package dev.aether.module.impl.performance;

import dev.aether.module.AbstractModule;
import dev.aether.module.ClientModule.ModuleCategory;
import dev.aether.module.ClientModule.ModuleMetadata;

public class DeveloperOverlayModule extends AbstractModule {
    public DeveloperOverlayModule() {
        super(ModuleMetadata.builder("developer.overlay", "Developer Overlay")
            .category(ModuleCategory.PERFORMANCE)
            .description("Shows local frame, memory, and runtime diagnostics for development.")
            .build());
    }
}
