package dev.aether.module.impl.graphics;

import dev.aether.module.AbstractModule;
import dev.aether.module.ClientModule.ModuleCategory;
import dev.aether.module.ClientModule.ModuleMetadata;

public class TimeChangerModule extends AbstractModule {
    public TimeChangerModule() {
        super(ModuleMetadata.builder("graphics.time_changer", "Time Changer")
            .category(ModuleCategory.GRAPHICS)
            .description("Changes the in-game world time of day. Offset controls the visual time (0=dawn, 12000=dusk).")
            .build());

        addNumber("offset", "Offset", 12000);
    }
}
