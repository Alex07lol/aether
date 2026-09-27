package dev.aether.module.impl.cosmetics;

import dev.aether.module.AbstractModule;
import dev.aether.module.ClientModule.ModuleCategory;
import dev.aether.module.ClientModule.ModuleMetadata;

/** Draws the selected wing pair behind the player's shoulders. */
public class CurrentWingsModule extends AbstractModule {
    public static final String ID = "cosmetics.current_wings";

    public CurrentWingsModule() {
        super(ModuleMetadata.builder(ID, "Wings")
            .category(ModuleCategory.COSMETICS)
            .description("Wears the selected wings in world.")
            .build());
        addNumber("opacity", "Opacity", 85, 0, 100, 5);
        addNumber("spread", "Spread", 45, 10, 80, 5);
        addNumber("flap_speed", "Flap Speed", 50, 0, 100, 5);
        addNumber("size", "Size", 100, 50, 200, 5);
    }
}
