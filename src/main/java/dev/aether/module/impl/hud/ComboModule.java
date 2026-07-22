package dev.aether.module.impl.hud;

import dev.aether.module.AbstractModule;
import dev.aether.module.ClientModule.ModuleCategory;
import dev.aether.module.ClientModule.ModuleMetadata;

public class ComboModule extends AbstractModule {
    public ComboModule() {
        super(ModuleMetadata.builder("hud.combo", "Combo Counter")
            .category(ModuleCategory.HUD)
            .description("Tracks and displays your hit combo count on the HUD during combat.")
            .build());
    }
}
