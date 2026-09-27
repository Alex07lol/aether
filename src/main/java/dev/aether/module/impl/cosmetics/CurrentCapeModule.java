package dev.aether.module.impl.cosmetics;

import dev.aether.module.AbstractModule;
import dev.aether.module.ClientModule.ModuleCategory;
import dev.aether.module.ClientModule.ModuleMetadata;

/**
 * Wears the selected cape. The Forge renderer draws it as a waving cloth behind the player,
 * or samples the imported PNG when the cape came from a file.
 */
public class CurrentCapeModule extends AbstractModule {
    public static final String ID = "cosmetics.current_cape";

    public CurrentCapeModule() {
        super(ModuleMetadata.builder(ID, "Cape")
            .category(ModuleCategory.COSMETICS)
            .description("Wears the selected cape in world.")
            .build());
        addNumber("opacity", "Opacity", 90, 0, 100, 5);
        addNumber("wave", "Wave", 50, 0, 100, 5);
        addNumber("length", "Length", 100, 50, 150, 5);
        addBool("swing", "Walk Swing", true);
    }
}
