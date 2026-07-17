package dev.aether.module.impl.graphics;

import dev.aether.module.AbstractModule;
import dev.aether.module.ClientModule.ModuleCategory;
import dev.aether.module.ClientModule.ModuleMetadata;

public class AnimationModule extends AbstractModule {
    public AnimationModule() {
        super(ModuleMetadata.builder("graphics.animation", "1.7 Animations")
            .category(ModuleCategory.GRAPHICS)
            .description("Enables 1.7 block-hit and item animations in 1.8.9.")
            .build());

        addBool("block_animation", "Block Animation", true);
        addBool("eat_drink_animation", "Eat/Drink Animation", true);
        addBool("bow_animation", "Bow Animation", true);
        addBool("rod_animation", "Fishing Rod", true);
    }
}
