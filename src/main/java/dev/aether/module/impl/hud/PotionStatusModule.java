package dev.aether.module.impl.hud;

import dev.aether.module.AbstractModule;
import dev.aether.module.ClientModule.ModuleCategory;
import dev.aether.module.ClientModule.ModuleMetadata;

public class PotionStatusModule extends AbstractModule {
    public PotionStatusModule() {
        super(ModuleMetadata.builder("hud.potions", "Potion Effects")
            .category(ModuleCategory.HUD)
            .description("Provides a reserved HUD slot for active potion effects.")
            .build());

        addChoice("mode", "Mode", "Compact");
    }
}
