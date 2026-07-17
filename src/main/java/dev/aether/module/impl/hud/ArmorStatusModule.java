package dev.aether.module.impl.hud;

import dev.aether.module.AbstractModule;
import dev.aether.module.ClientModule.ModuleCategory;
import dev.aether.module.ClientModule.ModuleMetadata;

public class ArmorStatusModule extends AbstractModule {
    public ArmorStatusModule() {
        super(ModuleMetadata.builder("hud.armor", "Armor Status")
            .category(ModuleCategory.HUD)
            .description("Provides a reserved HUD slot for armor durability.")
            .build());

        addBool("show_durability", "Show Durability", true);
        addBool("show_damage", "Show Damage", true);
    }
}
