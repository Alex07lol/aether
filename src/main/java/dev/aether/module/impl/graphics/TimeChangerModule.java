package dev.aether.module.impl.graphics;

import dev.aether.module.AbstractModule;
import dev.aether.module.ClientModule.ModuleCategory;
import dev.aether.module.ClientModule.ModuleMetadata;

public class TimeChangerModule extends AbstractModule {
    public TimeChangerModule() {
        super(ModuleMetadata.builder("graphics.time_changer", "Time Changer")
            .category(ModuleCategory.GRAPHICS)
            .description("Visually changes the world time.")
            .build());

        addNumber("offset", "Offset", 12000);
        addNumber("speed", "Speed", 50);
    }
}
