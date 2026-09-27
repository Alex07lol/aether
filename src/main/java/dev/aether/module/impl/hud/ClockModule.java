package dev.aether.module.impl.hud;

import dev.aether.module.AbstractModule;
import dev.aether.module.ClientModule.ModuleCategory;
import dev.aether.module.ClientModule.ModuleMetadata;

public class ClockModule extends AbstractModule {
    public ClockModule() {
        super(ModuleMetadata.builder("hud.clock", "Clock")
            .category(ModuleCategory.HUD)
            .description("Shows the local time in 12 or 24 hour format.")
            .build());

        addChoice("format", "Format", "24h").choices("24h", "12h");
        addBool("show_background", "Show Background", false);
        addColor("text_color", "Text Color", 0xFFFFFFFF);
        addColor("background_color", "Background Color", 0x6F000000);
    }
}
