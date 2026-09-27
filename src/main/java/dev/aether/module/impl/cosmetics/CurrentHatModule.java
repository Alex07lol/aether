package dev.aether.module.impl.cosmetics;

import dev.aether.module.AbstractModule;
import dev.aether.module.ClientModule.ModuleCategory;
import dev.aether.module.ClientModule.ModuleMetadata;

/** Sits the selected hat on the player's head. */
public class CurrentHatModule extends AbstractModule {
    public static final String ID = "cosmetics.current_hat";

    public CurrentHatModule() {
        super(ModuleMetadata.builder(ID, "Hat")
            .category(ModuleCategory.COSMETICS)
            .description("Wears the selected hat in world.")
            .build());
        addNumber("radius", "Radius", 100, 50, 150, 5);
        addNumber("height", "Height", 100, 50, 200, 5);
        addNumber("tilt", "Tilt", 0, 0, 30, 1);
        addBool("brim", "Brim", true);
    }
}
