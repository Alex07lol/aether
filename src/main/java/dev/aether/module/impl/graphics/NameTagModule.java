package dev.aether.module.impl.graphics;

import dev.aether.module.AbstractModule;
import dev.aether.module.ClientModule.ModuleCategory;
import dev.aether.module.ClientModule.ModuleMetadata;

public class NameTagModule extends AbstractModule {
    public NameTagModule() {
        super(ModuleMetadata.builder("graphics.nametag", "Name Tags")
            .category(ModuleCategory.GRAPHICS)
            .description("Customizes player name tags and rendering.")
            .build());
    }
}
