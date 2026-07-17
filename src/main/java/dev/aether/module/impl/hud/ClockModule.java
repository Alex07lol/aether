package dev.aether.module.impl.hud;

import dev.aether.module.AbstractModule;
import dev.aether.module.ClientModule.ModuleCategory;
import dev.aether.module.ClientModule.ModuleMetadata;

public class ClockModule extends AbstractModule {
    public ClockModule() {
        super(ModuleMetadata.builder("hud.clock", "Clock")
            .category(ModuleCategory.HUD)
            .description("Shows local time on the HUD.")
            .build());

        addChoice("format", "Format", "24h");
    }
}
