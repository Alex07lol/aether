package dev.aether.module.impl.interface_;

import dev.aether.module.AbstractModule;
import dev.aether.module.ClientModule.ModuleCategory;
import dev.aether.module.ClientModule.ModuleMetadata;

public class ScreenshotManagerModule extends AbstractModule {
    public ScreenshotManagerModule() {
        super(ModuleMetadata.builder("interface.screenshot_manager", "Screenshot Manager")
            .category(ModuleCategory.INTERFACE)
            .description("Manages and organizes screenshots with an in-game viewer and sharing options.")
            .build());
    }
}
