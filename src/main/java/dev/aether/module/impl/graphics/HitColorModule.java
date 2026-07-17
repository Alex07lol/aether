package dev.aether.module.impl.graphics;

import dev.aether.module.AbstractModule;
import dev.aether.module.ClientModule.ModuleCategory;
import dev.aether.module.ClientModule.ModuleMetadata;

public class HitColorModule extends AbstractModule {
    public HitColorModule() {
        super(ModuleMetadata.builder("graphics.hit_color", "Hit Color")
            .category(ModuleCategory.GRAPHICS)
            .description("Adds a local hit feedback color slot.")
            .build());

        addColor("color", "Color", 0xFFFF5555);
    }
}
