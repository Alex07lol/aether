package dev.aether.module.impl.cosmetics;

import dev.aether.module.AbstractModule;
import dev.aether.module.ClientModule.ModuleCategory;
import dev.aether.module.ClientModule.ModuleMetadata;
import dev.aether.module.ModuleKind;

/** Rings the selected halo above the player's head. */
public class CurrentHaloModule extends AbstractModule {
    public static final String ID = "cosmetics.current_halo";

    public CurrentHaloModule() {
        super(ModuleMetadata.builder(ID, "Halo")
            .category(ModuleCategory.COSMETICS)
            .kind(ModuleKind.COSMETIC_SERVICE)
            .description("Wears the selected halo in world.")
            .build());
        addNumber("radius", "Radius", 30, 10, 80, 5);
        addNumber("opacity", "Opacity", 90, 0, 100, 5);
        addBool("glow", "Glow", true);
        addBool("bob", "Bob", true);
    }
}
