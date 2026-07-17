package dev.aether.module.impl.hud;

import dev.aether.module.AbstractModule;
import dev.aether.module.ClientModule.ModuleCategory;
import dev.aether.module.ClientModule.ModuleMetadata;

public class DayCounterModule extends AbstractModule {
    public DayCounterModule() {
        super(ModuleMetadata.builder("hud.day_counter", "Day Counter")
            .category(ModuleCategory.HUD)
            .description("Shows the current Minecraft Day on the HUD.")
            .build());

        addChoice("mode", "Mode", "Modern");
        addBool("show_background", "Show Background", true);
        addColor("text_color", "Text Color", 0xFFFFFFFF);
        addColor("background_color", "Background Color", 0x6F000000);
    }
}
