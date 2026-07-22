package dev.aether.module.impl.graphics;

import dev.aether.module.AbstractModule;
import dev.aether.module.ClientModule.ModuleCategory;
import dev.aether.module.ClientModule.ModuleMetadata;

public class ItemPhysicsModule extends AbstractModule {
    public ItemPhysicsModule() {
        super(ModuleMetadata.builder("graphics.item_physics", "Item Physics")
            .category(ModuleCategory.GRAPHICS)
            .description("Enables improved item drop physics with realistic rotation and bounce effects.")
            .build());
    }
}
