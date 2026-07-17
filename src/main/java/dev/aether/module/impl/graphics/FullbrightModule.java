package dev.aether.module.impl.graphics;

import dev.aether.module.AbstractModule;
import dev.aether.module.ClientModule.ModuleCategory;
import dev.aether.module.ClientModule.ModuleMetadata;

public class FullbrightModule extends AbstractModule {
    public FullbrightModule() {
        super(ModuleMetadata.builder("graphics.fullbright", "Fullbright")
            .category(ModuleCategory.GRAPHICS)
            .description("Raises local brightness while enabled.")
            .favoriteByDefault(true)
            .build());

        addNumber("brightness", "Brightness", 100);
    }
}
