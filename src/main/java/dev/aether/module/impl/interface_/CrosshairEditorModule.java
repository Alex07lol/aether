package dev.aether.module.impl.interface_;

import dev.aether.module.AbstractModule;
import dev.aether.module.ClientModule.ModuleCategory;
import dev.aether.module.ClientModule.ModuleMetadata;

public class CrosshairEditorModule extends AbstractModule {
    public CrosshairEditorModule() {
        super(ModuleMetadata.builder("interface.crosshair_editor", "Crosshair Editor")
            .category(ModuleCategory.INTERFACE)
            .description("Adds a configurable crosshair utility slot.")
            .build());
    }
}
