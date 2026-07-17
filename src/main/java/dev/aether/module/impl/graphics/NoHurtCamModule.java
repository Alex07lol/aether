package dev.aether.module.impl.graphics;

import dev.aether.module.AbstractModule;
import dev.aether.module.ClientModule.ModuleCategory;
import dev.aether.module.ClientModule.ModuleMetadata;

public class NoHurtCamModule extends AbstractModule {
    public NoHurtCamModule() {
        super(ModuleMetadata.builder("graphics.no_hurt_cam", "No Hurt Cam")
            .category(ModuleCategory.GRAPHICS)
            .description("Controls how much damage shakes the camera.")
            .build());

        addNumber("shake_amount", "Shake Amount", 0);
    }
}
