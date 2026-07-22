package dev.aether.module.impl.graphics;

import dev.aether.module.AbstractModule;
import dev.aether.module.ClientModule.ModuleCategory;
import dev.aether.module.ClientModule.ModuleMetadata;

public class MotionBlurModule extends AbstractModule {
    public MotionBlurModule() {
        super(ModuleMetadata.builder("graphics.motion_blur", "Motion Blur")
            .category(ModuleCategory.GRAPHICS)
            .description("Applies a motion blur effect when turning or moving the camera quickly.")
            .build());

        addNumber("strength", "Strength", 40);
    }
}
